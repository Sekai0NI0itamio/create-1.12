package nl.melonstudios.create.tesr.actor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockRender;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityPress;
import nl.melonstudios.create.util.EnumRenderPart;
import nl.melonstudios.create.util.Utils;

@SideOnly(Side.CLIENT)
public class TESRPress<T extends TileEntityPress> extends TESRKineticBase<T> {
    @Override
    protected void render(T te, float pt, float alpha) {
        GlStateManager.pushMatrix();
        float rawProgress = MathHelper.clamp(Utils.lerp(pt, te.lastProgress, te.progress), Math.min(te.lastProgress, te.progress), Math.max(te.lastProgress, te.progress));
        float dip;
        if (rawProgress < 4000.0F / 3.0F) {
            float up = rawProgress / 2000.0F * 2.0F;
            dip = MathHelper.clamp(up * up * up, 0.0F, 1.0F);
        } else {
            dip = MathHelper.clamp((2000.0F - rawProgress) / 2000.0F * 3.0F, 0.0F, 1.0F);
        }
        GlStateManager.translate(0.0F, -dip * te.multiplier(), 0.0F);
        IBlockState state = BlockRender.byEnum(te.getRenderAxis() == EnumFacing.Axis.X ? EnumRenderPart.PRESS_X : EnumRenderPart.PRESS_Z);
        IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);
        this.renderBakedModel(1.0F, model, state);
        GlStateManager.popMatrix();

        this.spinShaft(te, pt, te.getRenderAxis());
    }
}
