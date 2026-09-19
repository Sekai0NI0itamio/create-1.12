package nl.melonstudios.create.tesr.actor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.BakedItemModel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockRender;
import nl.melonstudios.create.block.state.EnumDirection;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityCrafter;
import nl.melonstudios.create.util.EnumRenderPart;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class TESRCrafter extends TESRKineticBase<TileEntityCrafter> {
    private static short unpackX(int packed) {
        return (short) (packed & 0xFFFF);
    }
    private static short unpackY(int packed) {
        return (short) ((packed >> 16) & 0xFFFF);
    }


    @Override
    protected void render(TileEntityCrafter te, float pt, float alpha) {
        this.spinShaftlessCog(te, te.getSpeed(), te.getFacing().getAxis(), pt);

        if (te.isOccupied()) {
            {
                IBlockState state = BlockRender.byEnum(EnumRenderPart.getCraftingCover(te.getFacing()));
                IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);
                this.renderBakedModel(1.0F, model, state);
            }
            if (te.crafterContext != null && te.crafterContext.currentPattern != null) {
                Int2ObjectMap<ItemStack> pattern = te.crafterContext.currentPattern;
                boolean isDestination = te.getPointerCrafter() == null;

                //TODO: render crafter context
                GlStateManager.enableRescaleNormal();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

                GlStateManager.pushMatrix();

                GlStateManager.translate(0.5, 0.5, 0.5);
                GlStateManager.rotate(-te.getFacing().getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
                EnumDirection shift = te.getDirection();
                float adjusted = Math.max(0.0F, te.crafterContext.progress - 0.25F);

                if (isDestination) {

                } else {
                    GlStateManager.translate(adjusted * shift.getOffsetX(), adjusted * shift.getOffsetY(), 0.0F);
                }

                for (Int2ObjectMap.Entry<ItemStack> entry : pattern.int2ObjectEntrySet()) {
                    ItemStack stack = entry.getValue();
                    if (stack.isEmpty()) continue;

                    GlStateManager.pushMatrix();
                    short x = unpackX(entry.getIntKey());
                    short y = unpackY(entry.getIntKey());

                    IBakedModel model = this.mc.getRenderItem().getItemModelWithOverrides(stack, this.getWorld(), null);
                    boolean isFlat = model instanceof BakedItemModel;
                    GlStateManager.translate(x*0.25F, y*0.25F, 0.5F + (isFlat ? 0.03125F : 0.0F));

                    if (!isFlat) GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                    if (isFlat) {
                        GlStateManager.scale(0.33F, 0.33F, 0.33F);
                    } else {
                        GlStateManager.scale(0.25F, 0.25F, 0.25F);
                    }
                    this.mc.getRenderItem().renderItem(stack, model);

                    GlStateManager.popMatrix();
                }

                GlStateManager.popMatrix();

                GlStateManager.disableRescaleNormal();
                GlStateManager.disableBlend();
            }
        }
        if (te.shouldRenderItemInside()) {
            GlStateManager.enableRescaleNormal();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

            IBakedModel model = this.mc.getRenderItem().getItemModelWithOverrides(te.containedItem, this.getWorld(), null);
            boolean isFlat = model instanceof BakedItemModel;

            GlStateManager.translate(0.5, 0.5, 0.5);
            GlStateManager.rotate(-te.getFacing().getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, 0.0F, 0.5F + (isFlat ? 0.03125F : 0.0F));

            if (!isFlat) GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            if (isFlat) {
                GlStateManager.scale(0.33F, 0.33F, 0.33F);
            } else {
                GlStateManager.scale(0.25F, 0.25F, 0.25F);
            }
            this.mc.getRenderItem().renderItem(te.containedItem, model);

            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
        }
    }
}
