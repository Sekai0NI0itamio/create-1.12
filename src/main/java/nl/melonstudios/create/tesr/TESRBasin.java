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
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.BakedItemModel;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.tileentity.TileEntityBasin;
import nl.melonstudios.create.util.RenderUtils;
import nl.melonstudios.create.util.SubInteractionBox;
import nl.melonstudios.create.util.Utils;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@SideOnly(Side.CLIENT)
public class TESRBasin extends TileEntitySpecialRenderer<TileEntityBasin> {
    public TESRBasin() {
        this.rendererDispatcher = TileEntityRendererDispatcher.instance;
        this.mc = Minecraft.getMinecraft();
    }

    protected final Minecraft mc;

    protected final double[] level = new double[1];
    protected final double[] levels = new double[4];

    @Override
    public void render(TileEntityBasin te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        this.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        List<FluidStack> liquids = te.fluid.getHandlers().stream().map(FluidTank::getFluid).filter(Objects::nonNull).collect(Collectors.toList());

        int fluidAmount = 0;
        for (FluidStack liquid : liquids) fluidAmount += liquid.amount;
        fluidAmount = Math.min(fluidAmount, 1500);
        this.level[0] = 0.125 + ((fluidAmount / 1500.0) * 0.8);
        if (!liquids.isEmpty()) {
            RenderUtils.prepare(x, y, z);
            GlStateManager.disableBlend(); //transparency is an issue at times

            World world = this.getWorld();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder renderer = tessellator.getBuffer();
            renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

            for (int i = 0; i < liquids.size(); i++) {
                FluidStack liquid = liquids.get(i);
                this.setLevels(world, te, liquid.hashCode(), partialTicks);
                this.renderLiquid(liquid, world, te.getPos(), i, renderer);
            }

            tessellator.draw();

            RenderUtils.finish();
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        if (te.recipeFilter != null) {
            ItemStack stack = te.recipeFilter.getRenderItem();
            if (!stack.isEmpty()) {
                GlStateManager.pushMatrix();

                IBakedModel itemModel = this.mc.getRenderItem()
                        .getItemModelWithOverrides(stack, te.getWorld(), null);
                GlStateManager.translate(0.5F, 0.75F, 0.5F);
                for (int i = 0; i < 4; i++) {
                    GlStateManager.pushMatrix();
                    if (i != 0) GlStateManager.rotate(i*90.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.translate(0.0F, 0.0F, 0.5F);
                    if (!(itemModel instanceof BakedItemModel)) GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.scale(0.25F, 0.25F, 0.25F);
                    this.mc.getRenderItem().renderItem(stack, itemModel);
                    GlStateManager.popMatrix();
                }
                GlStateManager.popMatrix();
            }
        }
        if (!te.inventory.isEmpty()) {
            GlStateManager.pushMatrix();
            int count = te.inventory.size();
            GlStateManager.translate(0.5, this.level[0], 0.5);
            float baseRot = Utils.clampedLerp(partialTicks, te.itemRotationOld, te.itemRotation);
            float spacing = 360.0F / count;
            Random rnd = new Random(te.hashCode());
            for (int i = 0; i < count; i++) {
                GlStateManager.pushMatrix();
                ItemStack stack = te.inventory.get(i);
                IBakedModel itemModel = this.mc.getRenderItem()
                        .getItemModelWithOverrides(stack, te.getWorld(), null);
                GlStateManager.rotate(spacing*i+baseRot, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(0.35F, 0.0F, 0.0F);
                if (fluidAmount > 0) {
                    // Gentle bob while submerged, matching the reference BasinRenderer.
                    double renderTime = (double) this.getWorld().getTotalWorldTime() + partialTicks;
                    GlStateManager.translate(0.0F,
                            (Math.sin(renderTime / 12.0 + spacing * i) + 1.5) / 32.0, 0.0F);
                }
                if (itemModel instanceof BakedItemModel) {
                    GlStateManager.scale(0.25F, 0.25F, 0.25F);
                } else {
                    GlStateManager.scale(0.15F, 0.15F, 0.15F);
                }
                GlStateManager.rotate(90.0F, rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat());
                this.mc.getRenderItem().renderItem(stack, itemModel);
                GlStateManager.popMatrix();
            }
            GlStateManager.popMatrix();
        }
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();

        SubInteractionBox.renderPotentialInteractionBoxes(this.mc.objectMouseOver, te);
        GlStateManager.popMatrix();
    }

    private void renderLiquid(FluidStack liquid, World world, BlockPos pos, int i, BufferBuilder renderer) {
        TextureAtlasSprite sprite = this.mc.getTextureMapBlocks().getAtlasSprite(liquid.getFluid().getStill(liquid).toString());
        int brightness = world.getCombinedLight(pos, liquid.getFluid().getLuminosity(liquid));
        int l1 = brightness >> 0x10 & 0xFFFF;
        int l2 = brightness & 0xFFFF;
        int color = liquid.getFluid().getColor(liquid);
        int a = color >> 24 & 0xFF;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        RenderUtils.renderFluidSurface(renderer, sprite,
                0.1, 0.1, 0.9, 0.9,
                this.levels[i&3], this.levels[i+1&3], this.levels[i+2&3], this.levels[i+3&3],
                r, g, b, a, l1, l2
        );
    }
    private void setLevels(World world, TileEntityBasin te, int offset, float pt) {
        double time = (double) world.getTotalWorldTime() + pt + Math.abs(te.hashCode() | ((long)te.getPos().hashCode() << 32));
        double lvl1 = Math.sin(Math.toRadians((time+offset) % 360))*0.01+this.level[0];
        double lvl2 = Math.sin(Math.toRadians((time+offset+90) % 360))*0.01+this.level[0];
        double lvl3 = Math.sin(Math.toRadians((time+offset+180) % 360))*0.01+this.level[0];
        double lvl4 = Math.sin(Math.toRadians((time+offset+270) % 360))*0.01+this.level[0];
        this.levels[0] = lvl1;
        this.levels[1] = lvl2;
        this.levels[2] = lvl3;
        this.levels[3] = lvl4;
    }
}
