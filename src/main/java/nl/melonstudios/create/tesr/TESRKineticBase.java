package nl.melonstudios.create.tesr;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.block.BlockRender;
import nl.melonstudios.create.cfg.ClientConfig;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.kinetics.FastStateRendering;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.util.EnumRenderPart;
import nl.melonstudios.create.util.PerFrameDebugInfo;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.util.HashSet;
import java.util.List;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public abstract class TESRKineticBase<T extends TileEntityKinetic> extends TESRBase<T> {
    private static boolean porkchop = false;
    private static final ResourceLocation PORK =
            new ResourceLocation("create", "textures/pork.png");
    public static ResourceLocation bind() {
        return porkchop ? PORK : TextureMap.LOCATION_BLOCKS_TEXTURE;
    }

    public static void pork() {
        porkchop = new File(Minecraft.getMinecraft().mcDataDir, "porkchop.gears").exists();
    }

    public TESRKineticBase() {
        super();
        this.shaftX = BlockInit.SHAFT.getStateFromMeta(0);
        this.shaftY = BlockInit.SHAFT.getStateFromMeta(1);
        this.shaftZ = BlockInit.SHAFT.getStateFromMeta(2);
        for (EnumFacing side : EnumFacing.VALUES) {
            this.halfShafts[side.getIndex()] = BlockRender.byEnum(EnumRenderPart.getHalfShaft(side));
        }
        for (EnumFacing.Axis axis : EnumFacing.Axis.values()) {
            this.shaftlessCogs[axis.ordinal()] = BlockRender.byEnum(EnumRenderPart.getShaftlessCog(axis));
        }
    }
    protected final IBlockState shaftX, shaftY, shaftZ;
    protected final IBlockState[] halfShafts = new IBlockState[6];
    protected final IBlockState[] shaftlessCogs = new IBlockState[3];
    @Override
    public void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (!this.shouldRender(te)) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        this.bindTexture(destroyStage >= 0 ? DESTROY_STAGES[destroyStage] : porkchop ? PORK : TextureMap.LOCATION_BLOCKS_TEXTURE);
        this.render(te, partialTicks, alpha);
        GlStateManager.popMatrix();
        PerFrameDebugInfo.kineticTileEntitiesRendered++;
    }

    protected void rebindTex() {
        this.bindTexture(porkchop ? PORK : TextureMap.LOCATION_BLOCKS_TEXTURE);
    }

    protected abstract void render(T te, float pt, float alpha);

    public boolean shouldRender(T te) {
        return true;
    }

    protected final void spinShaft(TileEntityKinetic te, float pt, EnumFacing.Axis axis) {
        IBlockState state = Utils.axis_choose(axis, this.shaftX, this.shaftY, this.shaftZ);
        IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);
        this.spinModel(te, pt, axis, model, state, 1.0F);
    }

    protected final void spinModel(TileEntityKinetic te, float pt, EnumFacing.Axis axis, IBakedModel model, IBlockState state, float m) {
        this.rotateModel(calculateAngle(te, axis, pt, m, true), axis, model, state, 1.0F);
    }

    protected final void rotateModel(float angle, EnumFacing.Axis axis, IBakedModel model, IBlockState state, float brightness) {
        GlStateManager.pushMatrix();
        this.glRotate(angle, axis);
        this.renderBakedModel(brightness, model, state);
        //this.mc.getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightness(model, state, brightness, true);
        GlStateManager.popMatrix();
    }
    protected final void glRotate(float angle, EnumFacing.Axis axis) {
        if (angle == 0.0F) return; //small optimization
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        GlStateManager.rotate(angle,
                axis == EnumFacing.Axis.X ? 1 : 0,
                axis == EnumFacing.Axis.Y ? 1 : 0,
                axis == EnumFacing.Axis.Z ? 1 : 0
        );
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
    }
    protected final void spinHalfShaft(TileEntityKinetic te, float speed, EnumFacing side, float pt) {
        IBlockState state = this.halfShafts[side.getIndex()];
        IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);
        this.rotateModel(this.calculateAngle(te, side.getAxis(), pt, speed), side.getAxis(), model, state, 1.0F);
    }
    protected final void spinShaftlessCog(TileEntityKinetic te, float speed, EnumFacing.Axis axis, float pt) {
        IBlockState state = this.shaftlessCogs[axis.ordinal()];
        IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);
        this.rotateModel(this.calculateAngle(te, axis, pt, speed), axis, model, state, 1.0F);
    }

    protected final float calculateAngle(TileEntityKinetic te, EnumFacing.Axis axis, float pt, float speed) {
        if (speed == 0) return te.getAxisShift(axis);
        float time = this.kineticTime(pt);

        return ((time * 0.3F * speed) % 360) + te.getAxisShift(axis);
    }
    protected final float calculateAngle(TileEntityKinetic te, EnumFacing.Axis axis, float pt, float m, boolean addOffset) {
        if (te.getSpeed() == 0) return addOffset ? te.getAxisShift(axis) : 0.0F;
        float time = this.kineticTime(pt);

        return ((time * 0.3F * te.getSpeed() * m) % 360) + (addOffset ? te.getAxisShift(axis) : 0.0F);
    }

    /**
     * Clock for spin math, in the same units the reference renderer uses:
     * client world ticks plus the partial tick. The frame-counter clock in the
     * shared base advances once per rendered frame, which would make every
     * shaft spin faster at higher framerates and disagree with the tick-driven
     * bearing plate angle, so kinetics keep their own tick-based clock here.
     */
    protected final float kineticTime(float pt) {
        if (this.getWorld() == null) return this.getAdjustedTime(pt);
        return (float) this.getWorld().getTotalWorldTime() + pt;
    }

    public static boolean isAxisShifted(BlockPos pos, EnumFacing.Axis axis) {
        switch (axis) {
            case X: return (pos.getY() & 1) != (pos.getZ() & 1);
            case Y: return (pos.getX() & 1) != (pos.getZ() & 1);
            case Z: return (pos.getX() & 1) != (pos.getY() & 1);
            default:return false;
        }
    }
}
