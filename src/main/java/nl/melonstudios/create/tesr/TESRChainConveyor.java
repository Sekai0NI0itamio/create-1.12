package nl.melonstudios.create.tesr;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.BakedItemModel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockChainConveyor;
import nl.melonstudios.create.block.state.EnumBeltPart;
import nl.melonstudios.create.tileentity.TileEntityChainConveyor;
import org.lwjgl.opengl.GL11;

/**
 * Minimal chain-conveyor renderer: spins the endpoint shaft with the
 * kinetic speed and draws each carried stack riding the chain column.
 * Item drawing mirrors TESRBeltBase (flat items laid flat, cubes as-is).
 *
 * NOTE: needs registration in the client proxy (NEEDS-LEAD); without it
 * the lift works but cargo is invisible while in transit.
 */
@SideOnly(Side.CLIENT)
public class TESRChainConveyor extends TESRKineticBase<TileEntityChainConveyor> {
    public TESRChainConveyor() {
    }

    @Override
    protected void render(TileEntityChainConveyor te, float pt, float alpha) {
        IBlockState state = te.getState();
        if (!(state.getBlock() instanceof BlockChainConveyor)) return;
        if (state.getValue(BlockChainConveyor.PART) != EnumBeltPart.MIDDLE) {
            this.spinShaft(te, pt, EnumFacing.Axis.Y);
        }
        if (!te.hasLine() || te.getCargo().isEmpty()) return;

        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        float baseY = (te.getBottomY() - te.getPos().getY()) + 0.35F;
        for (TileEntityChainConveyor.Carried c : te.getCargo()) {
            if (c.stack.isEmpty()) continue;
            GlStateManager.pushMatrix();
            IBakedModel model = this.mc.getRenderItem().getItemModelWithOverrides(c.stack, this.getWorld(), null);
            boolean isFlat = model instanceof BakedItemModel;
            GlStateManager.translate(0.5F, baseY + c.offset, 0.5F);
            if (isFlat) {
                if (this.isUpright(c.stack)) {
                    this.drawUpright(model, c.stack);
                } else {
                    this.drawFlat(model, c.stack);
                }
            } else {
                this.drawCube(model, c.stack);
            }
            GlStateManager.popMatrix();
        }

        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
    }

    private void drawFlat(IBakedModel model, net.minecraft.item.ItemStack stack) {
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        GlStateManager.translate(0.0F, 0.05F, 0.0F);
        GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
        this.mc.getRenderItem().renderItem(stack, model);
    }

    private void drawCube(IBakedModel model, net.minecraft.item.ItemStack stack) {
        GlStateManager.scale(0.25F, 0.25F, 0.25F);
        GlStateManager.translate(0.0F, 0.5F, 0.0F);
        this.mc.getRenderItem().renderItem(stack, model);
    }

    private void drawUpright(IBakedModel model, net.minecraft.item.ItemStack stack) {
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        GlStateManager.translate(0.0F, 0.1875F, 0.0F);
        this.mc.getRenderItem().renderItem(stack, model);
    }

    private boolean isUpright(net.minecraft.item.ItemStack stack) {
        int uprightId = net.minecraftforge.oredict.OreDictionary.getOreID("create:uprightOnBelt");
        for (int id : net.minecraftforge.oredict.OreDictionary.getOreIDs(stack)) {
            if (id == uprightId) return true;
        }
        return false;
    }
}
