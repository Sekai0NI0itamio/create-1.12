package nl.melonstudios.create.tesr;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.tileentity.logistics.TileEntityPackager;
import nl.melonstudios.create.util.Utils;
import org.lwjgl.opengl.GL11;

/**
 * Iris-hatch overlay for the packager. The static block model keeps its frame,
 * while this renderer lays a closed-iris shutter over the lid. Whenever the
 * tile entity emits a package its cycle clock runs, and the shutter shrinks
 * toward the centre (the hatch reads as opening) before settling shut again.
 * The top shaft spins with the kinetic network like every other kinetic TESR.
 */
@SideOnly(Side.CLIENT)
public class TESRPackager extends TESRKineticBase<TileEntityPackager> {
    private static final ResourceLocation IRIS_CLOSED =
            new ResourceLocation("create", "textures/block/packager_iris_closed.png");
    /** Lid top face sits at 14/16; the shutter floats just above it. */
    private static final double SHUTTER_Y = 0.877D;
    /** Half-width of the shutter at rest, in block units. */
    private static final float SHUTTER_HALF = 0.44F;

    @Override
    protected void render(TileEntityPackager te, float pt, float alpha) {
        this.spinShaft(te, pt, EnumFacing.Axis.Y);

        float ticks = Utils.clampedLerp(pt, te.lastAnimationTicks, te.animationTicks);
        float done = (TileEntityPackager.CYCLE - ticks) / (float) TileEntityPackager.CYCLE;
        float bump = 0.0F;
        if (done > 0.0F && done < 1.0F) bump = (float) Math.sin(Math.PI * done);
        float half = SHUTTER_HALF * (1.0F - 0.85F * bump);

        this.mc.renderEngine.bindTexture(IRIS_CLOSED);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        builder.pos(0.5D - half, SHUTTER_Y, 0.5D - half).tex(0.0D, 0.0D).endVertex();
        builder.pos(0.5D - half, SHUTTER_Y, 0.5D + half).tex(0.0D, 1.0D).endVertex();
        builder.pos(0.5D + half, SHUTTER_Y, 0.5D + half).tex(1.0D, 1.0D).endVertex();
        builder.pos(0.5D + half, SHUTTER_Y, 0.5D - half).tex(1.0D, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        this.rebindTex();
    }
}
