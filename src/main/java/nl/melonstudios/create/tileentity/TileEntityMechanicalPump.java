package nl.melonstudios.create.tileentity;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import nl.melonstudios.create.block.fluid.BlockMechanicalPump;
import nl.melonstudios.create.block.fluid.IPipePassThrough;
import nl.melonstudios.create.tileentity.fluid.IFluidPipeConnectable;
import nl.melonstudios.create.tileentity.fluid.TileEntityFluidPipe;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Mechanical pump tile entity.
 * Translated from the reference PumpBlockEntity (never pasted):
 * <ul>
 * <li>No internal tank; the pump is a pressure source, not storage.</li>
 * <li>Requires rotation: nothing moves while {@code getSpeed() == 0}.</li>
 * <li>No minimum speed: pressure equals {@code |speed|}, so any spin pumps.</li>
 * <li>Pulls from the back face, pushes out the front face
 * (reference {@code isPullingOnSide(!front)}), regardless of spin sign.</li>
 * <li>Pump reach is 16 blocks (reference default
 * {@code mechanicalPumpRange = 16}).</li>
 * </ul>
 * Each pump cycle (every 10 ticks) moves up to
 * {@code clamp(|speed| * 4, 50..1000)} mB from the pull endpoint to the
 * push endpoint, so faster spin means faster transfer, matching the
 * reference pressure model in 1.12 Forge-capability terms.
 */
public class TileEntityMechanicalPump extends TileEntityKinetic {
    /** Reference default for mechanicalPumpRange. */
    public static final int PUMP_RANGE = 16;
    /** Ticks between transfer attempts. */
    public static final int PUMP_INTERVAL = 10;
    /** mB moved per cycle at 250+ RPM (one bucket max, like a bucket pull). */
    public static final int MAX_MB_PER_CYCLE = 1000;
    /** mB moved per cycle floor so slow shafts still visibly pump. */
    public static final int MIN_MB_PER_CYCLE = 50;

    private int tickCounter;

    /**
     * Pipe positions currently stamped with our pressure via
     * {@link TileEntityFluidPipe#setPumpPressure}. Tracked so stale runs
     * (rebuilt pipework, removed pump) can be reset to passive (-1).
     */
    private final Set<BlockPos> stampedPipes = new HashSet<BlockPos>();

    @Override
    public void tick() {
        super.tick();
        if (this.world == null || this.world.isRemote)
            return;
        if (this.getSpeed() == 0)
            return;
        if (++this.tickCounter < PUMP_INTERVAL)
            return;
        this.tickCounter = 0;
        this.pumpOnce();
        this.refreshPressures();
    }

    /** One transfer step: pull endpoint -> push endpoint. */
    protected void pumpOnce() {
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockMechanicalPump))
            return;
        EnumFacing front = state.getValue(BlockMechanicalPump.FACING);
        EnumFacing back = front.getOpposite();

        IFluidHandler source = this.findEndpoint(back);
        IFluidHandler sink = this.findEndpoint(front);
        if (source == null || sink == null)
            return;
        if (source == sink)
            return;

        int budget = this.mbPerCycle();
        FluidStack probe = source.drain(budget, false);
        if (probe == null || probe.amount <= 0)
            return;
        int accepted = sink.fill(probe.copy(), false);
        if (accepted <= 0)
            return;
        FluidStack moved = source.drain(accepted, true);
        if (moved != null && moved.amount > 0)
            sink.fill(moved.copy(), true);
    }

    /** mB per cycle from current spin, mirroring pressure = |speed|. */
    public int mbPerCycle() {
        int amount = (int) (Math.abs(this.getSpeed()) * 4.0F);
        return Math.max(MIN_MB_PER_CYCLE, Math.min(MAX_MB_PER_CYCLE, amount));
    }

    /**
     * Walk from the pump along {@code dir} (up to {@link #PUMP_RANGE}) and
     * return the first fluid handler found. Walkable positions are pipe
     * tiles ({@link TileEntityFluidPipe}), blocks implementing
     * {@link IFluidPipeConnectable} that accept the connection, and
     * {@link IPipePassThrough} blocks open on the walked axis. A directly
     * adjacent tank/handler with no pipe in between works too.
     */
    @Nullable
    protected IFluidHandler findEndpoint(EnumFacing dir) {
        World world = this.world;
        BlockPos p = this.pos.offset(dir);
        for (int i = 0; i < PUMP_RANGE; i++) {
            if (!world.isBlockLoaded(p))
                return null;
            TileEntity te = world.getTileEntity(p);
            boolean isPipePos = te instanceof TileEntityFluidPipe;
            if (!isPipePos && te != null && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite()))
                return te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite());
            if (!this.isWalkable(p, dir))
                return null;
            p = p.offset(dir);
        }
        return null;
    }

    /**
     * True when the pump run may continue through {@code p} along
     * {@code dir}: a pipe-tile position, an
     * {@link IFluidPipeConnectable} block accepting the pump-side face, or
     * an {@link IPipePassThrough} block open on the walked axis.
     */
    protected boolean isWalkable(BlockPos p, EnumFacing dir) {
        World world = this.world;
        TileEntity te = world.getTileEntity(p);
        if (te instanceof TileEntityFluidPipe)
            return true;
        IBlockState state = world.getBlockState(p);
        if (state.getBlock() instanceof IFluidPipeConnectable
                && ((IFluidPipeConnectable) state.getBlock()).canConnectPipe(world, p, dir.getOpposite()))
            return true;
        return state.getBlock() instanceof IPipePassThrough
                && ((IPipePassThrough) state.getBlock()).isPipeOpenAt(world, p, state, dir);
    }

    /**
     * Pressure this pump contributes to the sibling pipe network, mirroring
     * the reference {@code pressure = |speed|} model in whole mB units.
     */
    public int pumpPressure() {
        return (int) Math.abs(this.getSpeed());
    }

    /**
     * Stamp {@link #pumpPressure()} onto every pipe tile along both flow
     * runs so the sibling transfer engine ({@code FluidPipeNetwork}, which
     * reads pressure via {@code TileEntityFluidPipe.getPumpPressure})
     * moves fluid faster under pump pressure. Runs that went stale since
     * the last cycle are reset to passive (-1).
     */
    public void refreshPressures() {
        if (this.world == null || this.world.isRemote)
            return;
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockMechanicalPump) || this.getSpeed() == 0) {
            this.clearPumpPressures();
            return;
        }
        EnumFacing front = state.getValue(BlockMechanicalPump.FACING);
        int pressure = this.pumpPressure();
        Set<BlockPos> now = new HashSet<BlockPos>();
        this.collectPipeRun(front, now);
        this.collectPipeRun(front.getOpposite(), now);
        for (BlockPos p : now) {
            TileEntity te = this.world.getTileEntity(p);
            if (te instanceof TileEntityFluidPipe)
                ((TileEntityFluidPipe) te).setPumpPressure(pressure);
        }
        for (BlockPos p : this.stampedPipes) {
            if (!now.contains(p) && this.world.isBlockLoaded(p)) {
                TileEntity te = this.world.getTileEntity(p);
                if (te instanceof TileEntityFluidPipe)
                    ((TileEntityFluidPipe) te).setPumpPressure(-1);
            }
        }
        this.stampedPipes.clear();
        this.stampedPipes.addAll(now);
    }

    /** Collect pipe-tile positions along the run in {@code dir}. */
    protected void collectPipeRun(EnumFacing dir, Set<BlockPos> out) {
        World world = this.world;
        BlockPos p = this.pos.offset(dir);
        for (int i = 0; i < PUMP_RANGE; i++) {
            if (!world.isBlockLoaded(p))
                return;
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileEntityFluidPipe) {
                out.add(p.toImmutable());
            } else if (te != null && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite())) {
                return;
            }
            if (!this.isWalkable(p, dir))
                return;
            p = p.offset(dir);
        }
    }

    /** Reset every stamped pipe to passive pressure (-1). */
    public void clearPumpPressures() {
        if (this.world == null || this.world.isRemote) {
            this.stampedPipes.clear();
            return;
        }
        for (BlockPos p : this.stampedPipes) {
            if (!this.world.isBlockLoaded(p))
                continue;
            TileEntity te = this.world.getTileEntity(p);
            if (te instanceof TileEntityFluidPipe)
                ((TileEntityFluidPipe) te).setPumpPressure(-1);
        }
        this.stampedPipes.clear();
    }

    /** True while the pump can move fluid (spinning and not overstressed). */
    public boolean isPumping() {
        return this.getSpeed() != 0;
    }

    /** Called by the block when a neighbour on the flow axis changes. */
    public void onPipeChanged() {
        this.markDirty();
        this.refreshPressures();
    }

    @Override
    public void onSpeedChanged(float lastSpeed) {
        super.onSpeedChanged(lastSpeed);
        if (this.world == null || this.world.isRemote)
            return;
        // Same intent as the reference updatePressureChange: neighbours and
        // pipe endpoints must re-evaluate now that pressure (|speed|) moved.
        if (Math.abs(lastSpeed) != Math.abs(this.getSpeed())) {
            this.markDirty();
            this.refreshPressures();
            this.sync();
            this.world.notifyNeighborsOfStateChange(this.pos, this.blockType, true);
        }
    }

    @Override
    public void remove() {
        this.clearPumpPressures();
        super.remove();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
    }
}
