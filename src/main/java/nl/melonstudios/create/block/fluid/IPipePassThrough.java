package nl.melonstudios.create.block.fluid;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Integration point for the pipes worker (owns pipe blocks/transfer).
 *
 * <p>A block implementing this interface is treated by the mechanical pump
 * as a walkable pipe run: the pump scans through consecutive pass-through
 * blocks along its flow axis (up to its range) looking for a fluid-handler
 * endpoint. Implement this on pipe blocks; the pump calls
 * {@link #isPipeOpenAt} per side so elbows/closed ends stop the scan.</p>
 *
 * <p>Pipe tile entities are ALSO expected to expose
 * {@code CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY} so the pump can
 * drain/fill through them with plain Forge calls and no extra API.</p>
 */
public interface IPipePassThrough {
    boolean isPipeOpenAt(World world, BlockPos pos, IBlockState state, EnumFacing side);
}
