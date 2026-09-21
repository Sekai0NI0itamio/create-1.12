package nl.melonstudios.create.tesr.actor;

import com.melonstudios.melonlib.misc.BlockStateProperties;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockRender;
import nl.melonstudios.create.block.actor.BlockGauge;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.TileEntityGaugeBase;
import nl.melonstudios.create.util.EnumRenderPart;
import nl.melonstudios.create.util.Utils;

@SideOnly(Side.CLIENT)
public class TESRGauge<T extends TileEntityGaugeBase> extends TESRKineticBase<T> {
    public TESRGauge(BlockGauge.Type type) {
        this.type = type;
    }

    protected final BlockGauge.Type type;
    @Override
    protected void render(T te, float pt, float alpha) {
        IBlockState gaugeState = te.getState();
        this.spinShaft(te, pt, gaugeState.getValue(BlockStateProperties.HORIZONTAL_AXIS));

        float dialPivot = 5.75F / 16.0F;
        float progress = Utils.lerp(pt, te.prevDialState, te.dialState) * 90;

        IBlockState dialState = BlockRender.byEnum(EnumRenderPart.DIAL);
        IBakedModel dialModel = this.mc.getBlockRendererDispatcher().getModelForState(dialState);

        for (EnumFacing facing : EnumFacing.VALUES) {
            if (!((BlockGauge)gaugeState.getBlock()).renderHeadOnFace(te.getWorld(), te.getPos(), gaugeState, facing)) continue;

            GlStateManager.pushMatrix();
            glRotate(-facing.getHorizontalAngle() - 90, EnumFacing.Axis.Y);
            GlStateManager.translate(0, dialPivot, dialPivot);
            GlStateManager.rotate(-progress, 1, 0, 0);
            GlStateManager.translate(0, -dialPivot, -dialPivot);
            this.renderBakedModel(1.0F, dialModel, dialState);
            GlStateManager.popMatrix();
        }
    }
}
