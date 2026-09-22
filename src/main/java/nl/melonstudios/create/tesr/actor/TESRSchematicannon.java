package nl.melonstudios.create.tesr.actor;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntitySchematicannon;

/**
 * Schematicannon flight tracer: renders the just-fired block arcing from the
 * cannon mouth to its target along the reference bezier flight path
 * (translate of SchematicannonRenderer#renderLaunchedBlocks, block part).
 *
 * NOTE: registered in ClientProxy (TileEntitySchematicannon -> "schematicannon").
 * Cannon barrel aim is part of the static block model; only the in-flight
 * block tracer renders here.
 */
@SideOnly(Side.CLIENT)
public class TESRSchematicannon extends TESRKineticBase<TileEntitySchematicannon> {
    @Override
    protected void render(TileEntitySchematicannon te, float pt, float alpha) {
        if (te.lastShotTarget == null || te.lastShotBlockId < 0) return;
        if (te.getWorld() == null) return;

        BlockPos cannon = te.getPos();
        BlockPos target = te.lastShotTarget;
        double distSq = cannon.distanceSq(target);
        // Translate of LaunchedItem#ticksForDistance.
        int totalTicks = Math.max(10, (int) (Math.sqrt(Math.sqrt(distSq)) * 4.0F));
        float elapsed = (float) (te.getWorld().getTotalWorldTime() - te.lastShotTime) + pt;
        if (elapsed < 0 || elapsed >= totalTicks) return;

        Block block = Block.getBlockById(te.lastShotBlockId);
        if (block == null) return;
        Item item = Item.getItemFromBlock(block);
        if (item == null) return;

        // Local space: origin is the cannon pos (wrapper already translated).
        Vec3d start = new Vec3d(0.5, 1.5, 0.5);
        Vec3d tend = new Vec3d(target.getX() - cannon.getX() + 0.5,
                target.getY() - cannon.getY() + 0.5,
                target.getZ() - cannon.getZ() + 0.5);
        Vec3d distance = tend.subtract(start);
        double yDifference = tend.y - start.y;
        double throwHeight = distance.lengthVector() * 0.6 + yDifference;
        Vec3d cannonOffset = distance.addVector(0.0, throwHeight, 0.0).normalize().scale(2.0);
        start = start.add(cannonOffset);
        yDifference = tend.y - start.y;

        float progress = elapsed / totalTicks;
        float t = progress;
        Vec3d flat = tend.subtract(start).scale(progress);
        flat = new Vec3d(flat.x, 0.0, flat.z);
        double yOffset = 2.0 * (1.0 - t) * t * throwHeight + t * t * yDifference;
        Vec3d loc = new Vec3d(flat.x + 0.5, yOffset + 1.5, flat.z + 0.5).add(cannonOffset);

        GlStateManager.pushMatrix();
        GlStateManager.translate(loc.x, loc.y, loc.z);
        GlStateManager.translate(0.125F, 0.125F, 0.125F);
        GlStateManager.rotate(360.0F * t, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(360.0F * t, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-0.125F, -0.125F, -0.125F);
        float scale = 0.3F;
        GlStateManager.scale(scale, scale, scale);
        this.mc.getRenderItem().renderItem(new ItemStack(item, 1, te.lastShotMeta),
                ItemCameraTransforms.TransformType.GROUND);
        GlStateManager.popMatrix();
    }
}
