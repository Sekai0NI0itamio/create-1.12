package nl.melonstudios.create.tesr.actor;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityMechanicalArm;
import nl.melonstudios.create.util.Utils;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class TESRMechanicalArm extends TESRKineticBase<TileEntityMechanicalArm> {
    private static final float REST_LOWER = 55.0F;
    private static final float REST_UPPER = -110.0F;
    private static final float WORK_LOWER = 18.0F;
    private static final float WORK_UPPER = -42.0F;

    @Override
    protected void render(TileEntityMechanicalArm te, float pt, float alpha) {
        float progress = MathHelper.clamp(Utils.lerp(pt, te.lastProgress, te.progress), 0.0F, 1.0F);
        float yaw = te.lastYaw + (te.yaw - te.lastYaw) * pt;
        float swing = (float) Math.sin(progress * Math.PI);
        if (te.phase == TileEntityMechanicalArm.Phase.SEARCH_INPUTS
                || te.phase == TileEntityMechanicalArm.Phase.SEARCH_OUTPUTS) {
            swing *= 0.25F;
        }
        float lowerAngle = REST_LOWER + (WORK_LOWER - REST_LOWER) * swing;
        float upperAngle = REST_UPPER + (WORK_UPPER - REST_UPPER) * swing;

        this.spinShaft(te, pt, EnumFacing.Axis.Y);

        TextureAtlasSprite sprite = this.mc.getTextureMapBlocks()
                .getAtlasSprite("create:block/mechanical_arm");
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5F, 0.375F, 0.5F);
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);

        this.drawBox(sprite, -0.125F, 0.0F, -0.125F, 0.125F, 0.22F, 0.125F);

        GlStateManager.translate(0.0F, 0.22F, 0.0F);
        GlStateManager.rotate(lowerAngle, 1.0F, 0.0F, 0.0F);
        this.drawBox(sprite, -0.09F, 0.0F, -0.09F, 0.09F, 0.875F, 0.09F);

        GlStateManager.translate(0.0F, 0.875F, 0.0F);
        GlStateManager.rotate(upperAngle, 1.0F, 0.0F, 0.0F);
        this.drawBox(sprite, -0.07F, -0.9375F, -0.07F, 0.07F, 0.0F, 0.07F);

        GlStateManager.translate(0.0F, -0.9375F, 0.0F);
        float clawOpen = te.heldItem.isEmpty() ? 0.35F : 0.08F;
        this.drawBox(sprite, -0.16F, -0.09F, -0.06F, -0.16F + clawOpen, 0.09F, 0.06F);
        this.drawBox(sprite, 0.16F - clawOpen, -0.09F, -0.06F, 0.16F, 0.09F, 0.06F);

        if (!te.heldItem.isEmpty()) {
            ItemStack stack = te.heldItem;
            IBakedModel itemModel = this.mc.getRenderItem()
                    .getItemModelWithOverrides(stack, te.getWorld(), null);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, -0.12F, 0.0F);
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.enableRescaleNormal();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            this.mc.getRenderItem().renderItem(stack, itemModel);
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
    }

    private void drawBox(TextureAtlasSprite sprite,
                         float x0, float y0, float z0, float x1, float y1, float z1) {
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        builder.pos(x0, y0, z0).tex(u0, v1).endVertex();
        builder.pos(x0, y1, z0).tex(u0, v0).endVertex();
        builder.pos(x1, y1, z0).tex(u1, v0).endVertex();
        builder.pos(x1, y0, z0).tex(u1, v1).endVertex();
        builder.pos(x1, y0, z1).tex(u0, v1).endVertex();
        builder.pos(x1, y1, z1).tex(u0, v0).endVertex();
        builder.pos(x0, y1, z1).tex(u1, v0).endVertex();
        builder.pos(x0, y0, z1).tex(u1, v1).endVertex();
        builder.pos(x0, y0, z1).tex(u0, v1).endVertex();
        builder.pos(x0, y1, z1).tex(u0, v0).endVertex();
        builder.pos(x0, y1, z0).tex(u1, v0).endVertex();
        builder.pos(x0, y0, z0).tex(u1, v1).endVertex();
        builder.pos(x1, y0, z0).tex(u0, v1).endVertex();
        builder.pos(x1, y1, z0).tex(u0, v0).endVertex();
        builder.pos(x1, y1, z1).tex(u1, v0).endVertex();
        builder.pos(x1, y0, z1).tex(u1, v1).endVertex();
        builder.pos(x0, y1, z0).tex(u0, v1).endVertex();
        builder.pos(x0, y1, z1).tex(u0, v0).endVertex();
        builder.pos(x1, y1, z1).tex(u1, v0).endVertex();
        builder.pos(x1, y1, z0).tex(u1, v1).endVertex();
        builder.pos(x0, y0, z1).tex(u0, v1).endVertex();
        builder.pos(x0, y0, z0).tex(u0, v0).endVertex();
        builder.pos(x1, y0, z0).tex(u1, v0).endVertex();
        builder.pos(x1, y0, z1).tex(u1, v1).endVertex();
        tessellator.draw();
    }
}
