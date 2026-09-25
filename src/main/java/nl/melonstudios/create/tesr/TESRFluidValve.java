package nl.melonstudios.create.tesr;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.fluid.BlockFluidValve;
import nl.melonstudios.create.tileentity.TileEntityFluidValve;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Fluid valve visuals.
 * Mirrors the reference FluidValveRenderer in 1.12 terms: the kinetic
 * half-shaft spins on the shaft (FACING) axis, and a pointer cog turns
 * from 0 to -90 degrees on the pipe axis as the TE pointer opens.
 * The static body (open/shut faces) comes from the block model.
 */
@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class TESRFluidValve extends TESRKineticBase<TileEntityFluidValve> {
    public TESRFluidValve() {
        super();
    }

    @Override
    protected void render(TileEntityFluidValve te, float pt, float alpha) {
        EnumFacing facing = EnumFacing.NORTH;
        if (te.getWorld() != null) {
            IBlockState state = te.getWorld().getBlockState(te.getPos());
            if (state.getBlock() instanceof BlockFluidValve)
                facing = state.getValue(BlockFluidValve.FACING);
        }

        // Kinetic input spins like any shaft end.
        this.spinHalfShaft(te, te.getSpeed(), facing, pt);

        // Pointer: 0 deg shut -> -90 deg open, on the pipe axis.
        EnumFacing.Axis pipeAxis = facing.getAxis() == EnumFacing.Axis.X
                ? EnumFacing.Axis.Z : EnumFacing.Axis.X;
        IBlockState cogState = this.shaftlessCogs[pipeAxis.ordinal()];
        IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(cogState);
        this.rotateModel(-90.0F * te.pointer, pipeAxis, model, cogState, 1.0F);
    }
}
