package nl.melonstudios.create.tileentity.fluid;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pipe-graph discovery plus pull/push fluid movement, 1.12 edition.
 *
 * <p>Reference model (modern Create, paraphrased): pumps spread pressure
 * over the pipe graph (range 16, split over parallel branches); each pipe
 * side grows an inbound/outbound flow once pressure exists; when a flow
 * completes, a network walk from the endpoint gathers source and target
 * fluid handlers and moves {@code max(1, pressure / 2)} mB per tick,
 * divided evenly across sinks, simulate-then-execute, one fluid type at a
 * time.</p>
 *
 * <p>Backport simplifications: there is no pump block yet, so every pipe
 * reports a passive pressure ({@link #PASSIVE_PRESSURE}) unless a pump
 * worker overrides it via {@link TileEntityFluidPipe#setPumpPressure}.
 * Flows are not visualised per side; the whole connected graph moves fluid
 * in one atomic operation per interval instead of per-endpoint networks.
 * Smart-pipe filtering is a per-pipe fluid allow-list, not a UI slot.</p>
 *
 * <p>Ported reference values:</p>
 * <ul>
 * <li>{@link #PUMP_RANGE} = 16 — mechanical pump range (CFluids).</li>
 * <li>{@link #TRANSFER_DIVISOR} = 2 — transfer {@code = max(1, pressure / 2)}.</li>
 * <li>{@link #TRANSFER_INTERVAL} = 4 ticks between network moves for one
 * graph (reference moves every tick once flow is complete, after a 1-32
 * tick flow-setup; we fold setup into {@link #SETUP_DELAY}).</li>
 * <li>{@link #SETUP_DELAY} = 8 ticks before a fresh pipe may drive (covers
 * the reference 2-tick propagation pause plus short flow progress).</li>
 * <li>{@link #MAX_PIPES} = 256 pipe cap per walk (reference walks up to
 * range 16 with 16 BFS cycles per tick; the cap bounds the same work).</li>
 * </ul>
 */
public final class FluidPipeNetwork {
    private FluidPipeNetwork() {
    }

    /** Reference: mechanicalPumpRange = 16. */
    public static final int PUMP_RANGE = 16;
    /** Reference: transferSpeed = max(1, pressure / 2). */
    public static final int TRANSFER_DIVISOR = 2;
    /** Ticks between moves for one graph. */
    public static final int TRANSFER_INTERVAL = 4;
    /** Ticks a fresh pipe waits before driving a transfer. */
    public static final int SETUP_DELAY = 8;
    /** Passive pressure used while no pump block exists. 40 / 2 = 20 mB per move. */
    public static final int PASSIVE_PRESSURE = 40;
    /** Hard bound on pipes visited per walk; loop/dupe guard. */
    public static final int MAX_PIPES = 256;

    /** One endpoint candidate: a non-pipe handler touching the graph. */
    private static final class Endpoint {
        final BlockPos pipePos;
        final EnumFacing side;
        final TileEntity te;
        final IFluidHandler handler;

        Endpoint(BlockPos pipePos, EnumFacing side, TileEntity te, IFluidHandler handler) {
            this.pipePos = pipePos;
            this.side = side;
            this.te = te;
            this.handler = handler;
        }
    }

    private static final class Walk {
        final Set<BlockPos> pipes = new HashSet<BlockPos>();
        final List<TileEntityFluidPipe> pipeTes = new ArrayList<TileEntityFluidPipe>();
        final List<Endpoint> endpoints = new ArrayList<Endpoint>();
    }

    /**
     * True when the block at pos takes part in the pipe graph: hosts a
     * pipe tile, or its block declares connectivity.
     */
    public static boolean isPipe(World world, BlockPos pos) {
        if (world.getTileEntity(pos) instanceof TileEntityFluidPipe) return true;
        return world.getBlockState(pos).getBlock() instanceof IFluidPipeConnectable;
    }

    /** Fluid handler on the far side of the given face, or null. */
    public static IFluidHandler handlerAt(World world, BlockPos pos, EnumFacing side) {
        if (!world.isBlockLoaded(pos)) return null;
        TileEntity te = world.getTileEntity(pos);
        if (te == null) return null;
        if (te instanceof TileEntityFluidPipe) return null;
        if (!te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite())) return null;
        return te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite());
    }

    /** Side is open for flow if the neighbour is a pipe or a fluid handler. */
    private static boolean sideOpen(World world, BlockPos pipePos, EnumFacing side) {
        BlockPos n = pipePos.offset(side);
        if (!world.isBlockLoaded(n)) return false;
        if (isPipe(world, n)) {
            TileEntity te = world.getTileEntity(n);
            if (te instanceof TileEntityFluidPipe) return true;
            return ((IFluidPipeConnectable) world.getBlockState(n).getBlock())
                    .canConnectPipe(world, n, side.getOpposite());
        }
        return handlerAt(world, n, side) != null;
    }

    /** Breadth-first walk of the connected graph; loops cut by visited set. */
    private static Walk walk(World world, BlockPos start) {
        Walk walk = new Walk();
        Deque<BlockPos> queue = new ArrayDeque<BlockPos>();
        Deque<Integer> depth = new ArrayDeque<Integer>();
        queue.add(start);
        depth.add(0);
        walk.pipes.add(start);
        while (!queue.isEmpty() && walk.pipes.size() < MAX_PIPES) {
            BlockPos at = queue.removeFirst();
            int d = depth.removeFirst();
            TileEntity te = world.isBlockLoaded(at) ? world.getTileEntity(at) : null;
            if (te instanceof TileEntityFluidPipe) walk.pipeTes.add((TileEntityFluidPipe) te);
            for (EnumFacing side : EnumFacing.VALUES) {
                BlockPos n = at.offset(side);
                if (!world.isBlockLoaded(n)) continue;
                if (isPipe(world, n)) {
                    if (d + 1 > PUMP_RANGE) continue;
                    if (walk.pipes.add(n)) {
                        queue.add(n);
                        depth.add(d + 1);
                    }
                } else {
                    IFluidHandler handler = handlerAt(world, n, side);
                    if (handler != null) {
                        walk.endpoints.add(new Endpoint(at, side, world.getTileEntity(n), handler));
                    }
                }
            }
        }
        return walk;
    }

    /**
     * Drive one network move from the given pipe. Server side only.
     * Claims every visited pipe's cooldown so the graph moves once per
     * interval no matter how many pipes tick (single-driver rule).
     *
     * @return amount moved, 0 when nothing happened
     */
    public static int transferTick(World world, TileEntityFluidPipe driver) {
        if (world.isRemote) return 0;
        Walk walk = walk(world, driver.getPos());
        for (TileEntityFluidPipe pipe : walk.pipeTes) {
            pipe.cooldown = TRANSFER_INTERVAL;
        }
        if (walk.endpoints.isEmpty()) {
            setMarker(walk, null);
            return 0;
        }

        int pressure = effectivePressure(walk);
        int budget = Math.max(1, pressure / TRANSFER_DIVISOR);
        if (budget <= 0) return 0;

        FluidStack locked = currentMarker(walk);
        if (locked != null && !passesFilters(walk, locked)) {
            return 0;
        }

        // 1. Pick the fluid: marker wins, else first drainable source sample.
        FluidStack fluid = locked;
        if (fluid == null) {
            for (Endpoint e : walk.endpoints) {
                if (!pipeAllows(walk, e.pipePos, null)) continue;
                FluidStack sample = e.handler.drain(1, false);
                if (sample != null && sample.amount > 0 && passesFilters(walk, sample)) {
                    fluid = new FluidStack(sample.getFluid(), 1);
                    break;
                }
            }
        }
        if (fluid == null) {
            setMarker(walk, null);
            return 0;
        }
        final Fluid fluidType = fluid.getFluid();

        // 2. Simulate sink space (even split computed at execute time).
        List<Endpoint> sinks = new ArrayList<Endpoint>();
        for (Endpoint e : walk.endpoints) {
            if (!pipeAllows(walk, e.pipePos, fluid)) continue;
            FluidStack probe = new FluidStack(fluidType, Integer.MAX_VALUE);
            int room;
            try {
                room = e.handler.fill(probe, false);
            } catch (Exception ex) {
                room = 0;
            }
            if (room > 0) sinks.add(e);
        }
        if (sinks.isEmpty()) {
            setMarker(walk, fluid);
            return 0;
        }

        // 3. Drain from sources, round-robin, never exceeding budget.
        //    Everything drained is tracked so leftovers can be refunded:
        //    conservation is what makes duplication impossible.
        int remaining = budget;
        for (Endpoint e : walk.endpoints) {
            if (remaining <= 0) break;
            if (!pipeAllows(walk, e.pipePos, fluid)) continue;
            FluidStack sample = e.handler.drain(1, false);
            if (sample == null || !sample.isFluidEqual(fluid)) continue;
            FluidStack got = e.handler.drain(remaining, true);
            if (got != null && got.amount > 0 && got.isFluidEqual(fluid)) {
                remaining -= got.amount;
            }
        }
        int drained = budget - remaining;
        if (drained <= 0) {
            setMarker(walk, fluid);
            return 0;
        }

        // 4. Fill sinks evenly with remainder spread (reference split rule).
        FluidStack cargo = new FluidStack(fluidType, drained);
        int moved = 0;
        List<Endpoint> open = new ArrayList<Endpoint>(sinks);
        while (!open.isEmpty() && cargo.amount > 0) {
            int share = cargo.amount / open.size();
            int rest = cargo.amount % open.size();
            boolean acceptedAny = false;
            for (int i = open.size() - 1; i >= 0; i--) {
                int want = share + (rest > 0 ? 1 : 0);
                if (rest > 0) rest--;
                if (want <= 0) {
                    open.remove(i);
                    continue;
                }
                int filled;
                try {
                    filled = open.get(i).handler.fill(new FluidStack(fluidType, want), true);
                } catch (Exception ex) {
                    filled = 0;
                }
                if (filled > 0) {
                    acceptedAny = true;
                    cargo.amount -= filled;
                    moved += filled;
                }
                if (filled < want) open.remove(i);
            }
            if (!acceptedAny) break;
        }

        // 5. Refund anything the sinks could not take.
        if (cargo.amount > 0) {
            for (Endpoint e : walk.endpoints) {
                if (cargo.amount <= 0) break;
                if (!pipeAllows(walk, e.pipePos, fluid)) continue;
                try {
                    int back = e.handler.fill(cargo.copy(), true);
                    cargo.amount -= back;
                } catch (Exception ignored) {
                    // try the next endpoint
                }
            }
            // If even the refund failed the fluid is voided rather than
            // duplicated; this path needs a real source (mid-drain
            // disconnect) and stays conservative by construction.
        }

        setMarker(walk, moved > 0 || cargo.amount > 0 ? new FluidStack(fluidType, 1) : null);
        if (moved > 0) {
            for (TileEntityFluidPipe pipe : walk.pipeTes) pipe.onFluidMoved();
        }
        return moved;
    }

    /** Highest pressure seen on the walk; passive unless a pump overrides. */
    private static int effectivePressure(Walk walk) {
        int pressure = PASSIVE_PRESSURE;
        for (TileEntityFluidPipe pipe : walk.pipeTes) {
            if (pipe.getPumpPressure() >= 0) {
                pressure = Math.max(pressure, pipe.getPumpPressure());
            }
        }
        return pressure;
    }

    /** First non-cleared marker on the walk, or null when idle. */
    private static FluidStack currentMarker(Walk walk) {
        for (TileEntityFluidPipe pipe : walk.pipeTes) {
            FluidStack m = pipe.getMarker();
            if (m != null) return m;
        }
        return null;
    }

    private static void setMarker(Walk walk, FluidStack marker) {
        for (TileEntityFluidPipe pipe : walk.pipeTes) pipe.setMarker(marker);
    }

    /** Every pipe on the walk must allow the fluid (smart-filter rule). */
    private static boolean passesFilters(Walk walk, FluidStack fluid) {
        for (TileEntityFluidPipe pipe : walk.pipeTes) {
            if (!pipe.matchesFilter(fluid)) return false;
        }
        return true;
    }

    /** Pipe adjacent to the endpoint must be open on that side and allow fluid. */
    private static boolean pipeAllows(Walk walk, BlockPos pipePos, FluidStack fluid) {
        for (TileEntityFluidPipe pipe : walk.pipeTes) {
            if (pipe.getPos().equals(pipePos)) return pipe.matchesFilter(fluid);
        }
        return true;
    }

    /** Neighbour-side openness check for the block worker's connection query. */
    public static boolean canFlowToward(World world, BlockPos pipePos, EnumFacing side) {
        return sideOpen(world, pipePos, side);
    }

    /** Exposed for the pipe block's connection query API. */
    public static boolean connectsTo(World world, BlockPos pipePos, EnumFacing side) {
        return sideOpen(world, pipePos, side);
    }
}
