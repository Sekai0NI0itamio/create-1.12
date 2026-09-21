package nl.melonstudios.create.tesr.actor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockRender;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityEncasedFan;
import nl.melonstudios.create.util.EnumRenderPart;

@SideOnly(Side.CLIENT)
public class TESREncasedFan extends TESRKineticBase<TileEntityEncasedFan> {
    @Override
    protected void render(TileEntityEncasedFan te, float pt, float alpha) {
        EnumFacing facing = te.getAirFacing();
        // Rear half-shaft spins at normal kinetic speed, like the reference SHAFT_HALF.
        this.spinHalfShaft(te, te.getSpeed(), facing.getOpposite(), pt);
        IBlockState state = BlockRender.byEnum(EnumRenderPart.FAN_PROPELLER);
        IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);
        // Reference clamps the blade rate (rpm * 5 into +/-80..1280); translate as
        // a multiplier on shaft speed so blades blur at speed yet stop dead at 0.
        float speed = te.getSpeed();
        float m = 1.0F;
        if (speed != 0.0F) {
            m = MathHelper.clamp(Math.abs(speed) * 5.0F, 80.0F, 64.0F * 20.0F) / Math.abs(speed);
        }
        this.spinModel(te, pt, facing.getAxis(), model, state, m);
    }
}
