package nl.melonstudios.create.tesr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.BakedItemModel;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.block.BlockChute;
import nl.melonstudios.create.block.state.EnumChuteVariant;
import nl.melonstudios.create.tileentity.TileEntityChute;
import org.lwjgl.opengl.GL11;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class TESRChute extends TileEntitySpecialRenderer<TileEntityChute> {
    public TESRChute() {
        this.rendererDispatcher = TileEntityRendererDispatcher.instance;
        this.mc = Minecraft.getMinecraft();
    }

    protected final Minecraft mc;

    @Override
    public void render(TileEntityChute te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te.stack.isEmpty()) return;
        // Match reference ChuteRenderer: the travelling item is only visible through the transparent (window) shape.
        IBlockState state = te.getWorld().getBlockState(te.getPos());
        if (state.getBlock() instanceof BlockChute
                && state.getValue(BlockChute.VARIANT) != EnumChuteVariant.WINDOW) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x+0.5, y+0.5, z+0.5);
        GlStateManager.rotate(te.randomizedItemRotation + ((CreateLegacy.getRenderTimeF(partialTicks)) % 120) * 3,
                0.0F, 1.0F, 0.0F);
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        ItemStack stack = te.stack;
        IBakedModel model = this.mc.getRenderItem().getItemModelWithOverrides(stack, te.getWorld(), null);
        if (model instanceof BakedItemModel) {
            this.renderFlatItem(model, stack);
        } else this.renderCubeItem(model, stack);

        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderFlatItem(IBakedModel model, ItemStack stack) {
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
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
    private void renderCubeItem(IBakedModel model, ItemStack stack) {
        GlStateManager.scale(0.25F, 0.25F, 0.25F);
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

    private static final long OFFSET_SEED = 42069L;
}
