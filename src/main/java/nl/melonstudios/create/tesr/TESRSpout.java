package nl.melonstudios.create.tesr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.tileentity.actor.TileEntitySpout;
import nl.melonstudios.create.util.RenderUtils;
import org.lwjgl.opengl.GL11;

/**
 * Spout visuals, translated from the reference SpoutRenderer:
 * a fluid box inside the tank body, a falling stream column while
 * the spout is pouring (fillTimer &gt; 0), and a squeeze offset
 * applied per body slice (top/middle/bottom) during the pour.
 */
@SideOnly(Side.CLIENT)
public class TESRSpout extends TileEntitySpecialRenderer<TileEntitySpout> {
    public TESRSpout() {
        this.rendererDispatcher = TileEntityRendererDispatcher.instance;
    }

    /** Full pour length in ticks; mirrors the 20-tick fillTimer window. */
    private static final float POUR_LENGTH = 20.0F;
    /** Stream reach below the nozzle; stays inside the 1-block render box. */
    private static final double STREAM_DEPTH = 1.0D;

    @Override
    public void render(TileEntitySpout te, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        World world = te.getWorld();
        if (world == null) {
            return;
        }
        FluidStack fluid = te.tank.getFluid();
        if (fluid == null || fluid.amount <= 0) {
            return;
        }

        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderUtils.prepare(x, y, z);

        Minecraft mc = Minecraft.getMinecraft();
        BlockPos pos = te.getPos();
        int brightness = world.getCombinedLight(pos, fluid.getFluid().getLuminosity(fluid));
        int l1 = brightness >> 0x10 & 0xFFFF;
        int l2 = brightness & 0xFFFF;
        int color = fluid.getFluid().getColor(fluid);
        int a = color >> 24 & 0xFF;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder renderer = tessellator.getBuffer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

        // Tank fluid box. Reference bounds: inset 2.5/16, 11/16 tall column.
        float fill = MathHelper.clamp(fluid.amount / 1000.0F, 0.0F, 1.0F);
        float min = 2.5F / 16.0F;
        float max = min + 11.0F / 16.0F;
        float height = (11.0F / 16.0F) * Math.max(fill, 0.175F);
        TextureAtlasSprite still =
                mc.getTextureMapBlocks().getAtlasSprite(fluid.getFluid().getStill(fluid).toString());
        if (fluid.getFluid().isGaseous(fluid)) {
            this.renderBox(renderer, still, min, max - height, min, max, max, max, r, g, b, a, l1, l2, false);
        } else {
            this.renderBox(renderer, still, min, min, min, max, min + height, max, r, g, b, a, l1, l2, false);
        }

        // Falling stream while pouring. fillTimer is synced to the client,
        // so fillTimer > 0 is the pouring flag.
        boolean pouring = te.fillTimer > 0;
        float progress = 0.0F;
        if (pouring) {
            progress = MathHelper.clamp(1.0F - te.fillTimer / POUR_LENGTH, 0.0F, 1.0F);
            float pulse = 1.0F - (2.0F * progress - 1.0F) * (2.0F * progress - 1.0F);
            double half = 0.5D / 16.0D + (2.0D / 16.0D) * pulse;
            double cx = 0.5D - half;
            double cz = 0.5D - half;
            double w = half * 2.0D;
            TextureAtlasSprite flow =
                    mc.getTextureMapBlocks().getAtlasSprite(fluid.getFluid().getFlowing(fluid).toString());
            double scroll = ((double) world.getTotalWorldTime() + partialTicks) * 0.06D % 1.0D;
            RenderUtils.renderScrollingQuad(renderer, flow, cx, -STREAM_DEPTH, cz, w, STREAM_DEPTH, w,
                    EnumFacing.NORTH, r, g, b, a, l1, l2, scroll, false, false);
            RenderUtils.renderScrollingQuad(renderer, flow, cx, -STREAM_DEPTH, cz, w, STREAM_DEPTH, w,
                    EnumFacing.SOUTH, r, g, b, a, l1, l2, scroll, false, false);
            RenderUtils.renderScrollingQuad(renderer, flow, cx, -STREAM_DEPTH, cz, w, STREAM_DEPTH, w,
                    EnumFacing.WEST, r, g, b, a, l1, l2, scroll, false, false);
            RenderUtils.renderScrollingQuad(renderer, flow, cx, -STREAM_DEPTH, cz, w, STREAM_DEPTH, w,
                    EnumFacing.EAST, r, g, b, a, l1, l2, scroll, false, false);
            RenderUtils.putTexturedQuad(renderer, flow, cx, -STREAM_DEPTH, cz, w, 0.0D, w,
                    EnumFacing.DOWN, r, g, b, a, l1, l2, true);
        }

        tessellator.draw();

        // Squeeze: re-render each body slice (top/middle/bottom, same bounds
        // as models/block/spout_top|middle|bottom.json) with the reference's
        // cumulative per-slice shift. Only while visibly squeezing so the
        // slices never sit coplanar with the static block model at rest.
        if (pouring) {
            float squeeze = -(float) Math.sin(Math.PI * progress);
            if (squeeze < -0.02F) {
                int solid = world.getCombinedLight(pos, 0);
                int s1 = solid >> 0x10 & 0xFFFF;
                int s2 = solid & 0xFFFF;
                TextureAtlasSprite spout =
                        mc.getTextureMapBlocks().getAtlasSprite("create:block/spout");

                renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                GlStateManager.pushMatrix();
                this.renderBox(renderer, spout,
                        1.0D / 16.0D, 12.0D / 16.0D, 1.0D / 16.0D,
                        15.0D / 16.0D, 1.0D, 15.0D / 16.0D,
                        255, 255, 255, 255, s1, s2, false);
                GlStateManager.translate(0.0F, -3.0F * squeeze / 32.0F, 0.0F);
                this.renderBox(renderer, spout,
                        1.0D / 16.0D, 4.0D / 16.0D, 1.0D / 16.0D,
                        15.0D / 16.0D, 12.0D / 16.0D, 15.0D / 16.0D,
                        255, 255, 255, 255, s1, s2, false);
                GlStateManager.translate(0.0F, -3.0F * squeeze / 32.0F, 0.0F);
                this.renderBox(renderer, spout,
                        1.0D / 16.0D, 0.0D, 1.0D / 16.0D,
                        15.0D / 16.0D, 4.0D / 16.0D, 15.0D / 16.0D,
                        255, 255, 255, 255, s1, s2, false);
                GlStateManager.popMatrix();
                tessellator.draw();
            }
        }

        RenderUtils.finish();
    }

    private void renderBox(BufferBuilder renderer, TextureAtlasSprite sprite,
                           double x1, double y1, double z1, double x2, double y2, double z2,
                           int r, int g, int b, int alpha, int l1, int l2, boolean flowing) {
        double w = x2 - x1;
        double h = y2 - y1;
        double d = z2 - z1;
        RenderUtils.putTexturedQuad(renderer, sprite, x1, y1, z1, w, h, d,
                EnumFacing.DOWN, r, g, b, alpha, l1, l2, flowing);
        RenderUtils.putTexturedQuad(renderer, sprite, x1, y1, z1, w, h, d,
                EnumFacing.UP, r, g, b, alpha, l1, l2, flowing);
        RenderUtils.putTexturedQuad(renderer, sprite, x1, y1, z1, w, h, d,
                EnumFacing.NORTH, r, g, b, alpha, l1, l2, flowing);
        RenderUtils.putTexturedQuad(renderer, sprite, x1, y1, z1, w, h, d,
                EnumFacing.SOUTH, r, g, b, alpha, l1, l2, flowing);
        RenderUtils.putTexturedQuad(renderer, sprite, x1, y1, z1, w, h, d,
                EnumFacing.WEST, r, g, b, alpha, l1, l2, flowing);
        RenderUtils.putTexturedQuad(renderer, sprite, x1, y1, z1, w, h, d,
                EnumFacing.EAST, r, g, b, alpha, l1, l2, flowing);
    }
}
