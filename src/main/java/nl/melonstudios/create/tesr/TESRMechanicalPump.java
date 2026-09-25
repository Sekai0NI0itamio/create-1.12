package nl.melonstudios.create.tesr;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.fluid.BlockMechanicalPump;
import nl.melonstudios.create.tileentity.TileEntityMechanicalPump;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Mechanical pump visuals.
 * Mirrors the reference PumpRenderer (single rotating cog on the pump
 * axis): spins the shared shaftless-cog model around the FACING axis at
 * the TE speed. The static body comes from the block model.
 */
@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class TESRMechanicalPump extends TESRKineticBase<TileEntityMechanicalPump> {
    public TESRMechanicalPump() {
        super();
    }

    @Override
    protected void render(TileEntityMechanicalPump te, float pt, float alpha) {
        EnumFacing.Axis axis = EnumFacing.Axis.Y;
        if (te.getWorld() != null) {
            IBlockState state = te.getWorld().getBlockState(te.getPos());
            if (state.getBlock() instanceof BlockMechanicalPump)
                axis = state.getValue(BlockMechanicalPump.FACING).getAxis();
        }
        this.spinShaftlessCog(te, te.getSpeed(), axis, pt);
    }
}
