package nl.melonstudios.create.tesr.actor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.BakedItemModel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import nl.melonstudios.create.block.actor.BlockBeltBase;
import nl.melonstudios.create.block.state.EnumBeltPart;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityBeltBase;
import org.lwjgl.opengl.GL11;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class TESRBeltBase<T extends TileEntityBeltBase> extends TESRKineticBase<T> {
    public TESRBeltBase() {

    }

    @Override
    protected void render(T te, float pt, float alpha) {
        IBlockState state = te.getState();
        BlockBeltBase block = (BlockBeltBase) state.getBlock();
        if (state.getValue(BlockBeltBase.PART) != EnumBeltPart.MIDDLE) {
            this.spinShaft(te, pt, block.getRotationAxis(state));
        }

        if (block.isFunctional(state)) {
            if (te.left.isEmpty() && te.right.isEmpty()) return;
            GlStateManager.enableRescaleNormal();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

            EnumFacing facing = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, block.getTransportAxis(state));

            if (!te.left.isEmpty()) {
                double pos = te.getLeftPos(pt);
                GlStateManager.pushMatrix();
                IBakedModel model = this.mc.getRenderItem().getItemModelWithOverrides(te.left, this.getWorld(), null);
                boolean isFlat = model instanceof BakedItemModel;
                // Reference BeltRenderer rests items at beltStartOffset y = 15/16;
                // 0.75 put them coplanar with the scrolling top quad.
                GlStateManager.translate(0.5 + facing.getFrontOffsetX() * pos, 15.0 / 16.0, 0.5 + facing.getFrontOffsetZ() * pos);

                if (isFlat) {
                    if (this.upright(te.left)) {
                        this.renderUprightItem(model, te.left);
                    } else {
                        this.renderFlatItem(model, te.left);
                    }
                } else this.renderCubeItem(model, te.left);
                GlStateManager.popMatrix();
            }

            if (!te.right.isEmpty()) {
                double pos = te.getRightPos(pt);
                GlStateManager.pushMatrix();
                IBakedModel model = this.mc.getRenderItem().getItemModelWithOverrides(te.right, this.getWorld(), null);
                boolean isFlat = model instanceof BakedItemModel;
                // Same 15/16 rest height as the left slot (see above).
                GlStateManager.translate(0.5 + facing.getFrontOffsetX() * pos, 15.0 / 16.0, 0.5 + facing.getFrontOffsetZ() * pos);

                if (isFlat) {
                    if (this.upright(te.right)) {
                        this.renderUprightItem(model, te.right);
                    } else {
                        this.renderFlatItem(model, te.right);
                    }
                } else this.renderCubeItem(model, te.right);
                GlStateManager.popMatrix();
            }

            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
        }
    }

    private void renderFlatItem(IBakedModel model, ItemStack stack) {
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        GlStateManager.translate(0.0F, 0.05F, 0.0F);
        GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
        int amount = (stack.getCount() + 6) / 8;
        this.mc.getRenderItem().renderItem(stack, model);
        Random rand = new Random(OFFSET_SEED);
        for (int i = 0; i < amount; i++) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 0.05F * (i+1));
            GlStateManager.rotate(rand.nextInt(360), 0.0F, 0.0F, 1.0F);
            this.mc.getRenderItem().renderItem(stack, model);
            GlStateManager.popMatrix();
        }
    }
    private void renderCubeItem(IBakedModel model, ItemStack stack) {
        GlStateManager.scale(0.25F, 0.25F, 0.25F);
        GlStateManager.translate(0.0F, 0.5F, 0.0F);
        int amount = (stack.getCount() + 6) / 8;
        this.mc.getRenderItem().renderItem(stack, model);
        Random rand = new Random(OFFSET_SEED);
        for (int i = 0; i < amount; i++) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(
                    rand.nextFloat() * 0.5F - 0.25F,
                    rand.nextFloat() * 0.5F - 0.25F,
                    rand.nextFloat() * 0.5F - 0.25F
            );
            GlStateManager.rotate(rand.nextInt(4) * 90.0F, 0.0F, 1.0F, 0.0F);
            this.mc.getRenderItem().renderItem(stack, model);
            GlStateManager.popMatrix();
        }
    }
    private void renderUprightItem(IBakedModel model, ItemStack stack) {
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        // Reference lifts upright items by 3/32 of a block; applied after the
        // 0.5 scale that is 0.1875 units here.
        GlStateManager.translate(0.0F, 0.1875F, 0.0F);
        int amount = (stack.getCount() + 6) / 8;
        this.mc.getRenderItem().renderItem(stack, model);
        Random rand = new Random(OFFSET_SEED);
        for (int i = 0; i < amount; i++) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(
                    rand.nextFloat() * 0.5F - 0.25F,
                    rand.nextFloat() * 0.5F - 0.25F,
                    rand.nextFloat() * 0.2F - 0.1F
            );
            this.mc.getRenderItem().renderItem(stack, model);
            GlStateManager.popMatrix();
        }
    }

    private boolean upright(ItemStack stack) {
        int oreID = OreDictionary.getOreID("create:uprightOnBelt");
        for (int i : OreDictionary.getOreIDs(stack)) {
            if (oreID == i) return true;
        }
        return false;
    }

    private static final long OFFSET_SEED = 42069L;
}
