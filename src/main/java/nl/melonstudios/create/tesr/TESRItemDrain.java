package nl.melonstudios.create.tesr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.world.World;
import net.minecraftforge.client.model.BakedItemModel;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.tileentity.TileEntityItemDrain;
import nl.melonstudios.create.util.RenderUtils;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class TESRItemDrain extends TileEntitySpecialRenderer<TileEntityItemDrain> {
    public TESRItemDrain() {
        this.rendererDispatcher = TileEntityRendererDispatcher.instance;
    }

    @Override
    public void render(TileEntityItemDrain te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderUtils.prepare(x, y, z);
        FluidStack fluid = te.tank.getFluid();
        if (fluid != null) {
            World world = te.getWorld();
            double level = (fluid.amount / 1500.0) * 0.5 + 0.15;
            double time = (double) world.getTotalWorldTime() + partialTicks
                    + Math.abs(te.hashCode() | ((long) te.getPos().hashCode() << 32));
            double lvl1 = Math.sin(Math.toRadians(time % 360))*0.01+level;
            double lvl2 = Math.sin(Math.toRadians((time+90) % 360))*0.01+level;
            double lvl3 = Math.sin(Math.toRadians((time+180) % 360))*0.01+level;
            double lvl4 = Math.sin(Math.toRadians((time+270) % 360))*0.01+level;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder renderer = tessellator.getBuffer();
            renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fluid.getFluid().getStill(fluid).toString());
            int brightness = world.getCombinedLight(te.getPos(), fluid.getFluid().getLuminosity(fluid));
            int l1 = brightness >> 0x10 & 0xFFFF;
            int l2 = brightness & 0xFFFF;
            int color = fluid.getFluid().getColor(fluid);
            int a = color >> 24 & 0xFF;
            int r = color >> 16 & 0xFF;
            int g = color >> 8 & 0xFF;
            int b = color & 0xFF;
            RenderUtils.renderFluidSurface(renderer, sprite, 0.05, 0.05, 0.95, 0.95, lvl1, lvl2, lvl3, lvl4, r, g, b, a, l1, l2);
            tessellator.draw();
        }
        if (!te.draining.isEmpty()) {
            GlStateManager.translate(0.5, 0.875F, 0.5);
            if (te.rollingDirection != null) {
                GlStateManager.rotate(-te.rollingDirection.getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
            }
            GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
            IBakedModel model = Minecraft.getMinecraft().getRenderItem()
                    .getItemModelWithOverrides(te.draining, this.getWorld(), null);
            if (model instanceof BakedItemModel) {
                GlStateManager.scale(0.5F, 0.5F, 0.5F);
            } else {
                GlStateManager.scale(0.25F, 0.25F, 0.25F);
            }
            Minecraft.getMinecraft().getRenderItem()
                    .renderItem(te.draining, model);
        }
        RenderUtils.finish();
    }
}
