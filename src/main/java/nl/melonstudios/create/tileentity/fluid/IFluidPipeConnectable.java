package nl.melonstudios.create.tileentity.fluid;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Minimal connectivity contract for fluid pipe blocks.
 *
 * <p>The pipe BLOCK worker owns connection shapes and rendering. This
 * interface is the only thing the transfer engine needs from a block:
 * "may fluid flow between this block and the neighbour on this side?"
 * Any block that returns true here (or hosts a
 * {@link TileEntityFluidPipe}) is treated as pipe graph by
 * {@link FluidPipeNetwork}.</p>
 */
public interface IFluidPipeConnectable {
    /**
     * @param world world the block sits in
     * @param pos   position of this block
     * @param side  side toward the neighbour asking
     * @return true if a pipe at the neighbour may connect through this side
     */
    boolean canConnectPipe(World world, BlockPos pos, EnumFacing side);
}
