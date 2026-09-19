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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.tileentity.TileEntityFluidTank;
import nl.melonstudios.create.util.RenderUtils;
import org.lwjgl.opengl.GL11;

/**
 * Renders the fluid surface inside tank columns, bottom-up per block,
 * mirroring the reference FluidTankRenderer fluid box.
 */
@SideOnly(Side.CLIENT)
public class TESRFluidTank extends TileEntitySpecialRenderer<TileEntityFluidTank> {
    public TESRFluidTank() {
        this.rendererDispatcher = TileEntityRendererDispatcher.instance;
        this.mc = Minecraft.getMinecraft();
    }

    protected final Minecraft mc;

    @Override
    public void render(TileEntityFluidTank te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        World world = this.getWorld();
        if (world == null) {
            return;
        }
        TileEntityFluidTank bottom = te.bottom();
        if (bottom == null) {
            return;
        }
        FluidStack fluid = bottom.effectiveTank().getFluid();
        if (fluid == null || fluid.amount <= 0) {
            return;
        }
        int below = te.getPos().getY() - bottom.getPos().getY();
        double local = (fluid.amount - (double) below * 8000.0D) / 8000.0D;
        if (local <= 0.0D) {
            return;
        }
        if (local > 1.0D) {
            local = 1.0D;
        }
        double surface = 0.06D + local * 0.88D;

        this.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderUtils.prepare(x, y, z);
        GlStateManager.disableBlend();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder renderer = tessellator.getBuffer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

        TextureAtlasSprite sprite = this.mc.getTextureMapBlocks()
                .getAtlasSprite(fluid.getFluid().getStill(fluid).toString());
        BlockPos pos = te.getPos();
        int brightness = world.getCombinedLight(pos, fluid.getFluid().getLuminosity(fluid));
        int l1 = brightness >> 0x10 & 0xFFFF;
        int l2 = brightness & 0xFFFF;
        int color = fluid.getFluid().getColor(fluid);
        int a = color >> 24 & 0xFF;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        RenderUtils.renderFluidSurface(renderer, sprite,
                0.06, 0.06, 0.94, 0.94,
                surface, surface, surface, surface,
                r, g, b, a, l1, l2);

        tessellator.draw();
        RenderUtils.finish();
    }
}
