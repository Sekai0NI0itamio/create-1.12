package nl.melonstudios.create.tileentity;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import nl.melonstudios.create.block.fluid.BlockFluidValve;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Fluid valve tile entity.
 * Translated from the reference FluidValveBlockEntity (never pasted):
 * <ul>
 * <li>A {@code pointer} float (0 = shut, 1 = open) chases 1 while speed is
 * strictly positive and 0 otherwise; per-tick chase rate is
 * {@code clamp(|speed| / 16 / 20, 0, 1)}, so a 16 RPM shaft opens the
 * valve in ~20 ticks and faster shafts open it quicker.</li>
 * <li>The ENABLED block state follows the pointer: set when it reaches 1,
 * cleared when it reaches 0.</li>
 * <li>Flow gating mirrors the reference ValvePipeBehaviour: fluid may pass
 * along the pipe axis only while ENABLED; a shut valve exposes no fluid
 * handler at all, so neighbouring tanks and (once written) pipes see a
 * closed end.</li>
 * </ul>
 */
public class TileEntityFluidValve extends TileEntityKinetic {
    /** 0 = shut, 1 = fully open; rendered as the pointer angle. */
    public float pointer;

    @Override
    public void tick() {
        super.tick();
        if (this.world == null)
            return;

        float target = this.getSpeed() > 0 ? 1.0F : 0.0F;
        float rate = this.chaseRate();
        if (this.pointer < target)
            this.pointer = Math.min(target, this.pointer + rate);
        else if (this.pointer > target)
            this.pointer = Math.max(target, this.pointer - rate);

        if (this.world.isRemote)
            return;

        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockFluidValve))
            return;
        boolean open = state.getValue(BlockFluidValve.ENABLED);
        if (!open && this.pointer >= 1.0F) {
            this.world.setBlockState(this.pos, state.withProperty(BlockFluidValve.ENABLED, true), 3);
            this.markDirty();
        } else if (open && this.pointer <= 0.0F) {
            this.world.setBlockState(this.pos, state.withProperty(BlockFluidValve.ENABLED, false), 3);
            this.markDirty();
        }
    }

    /** Per-tick pointer rate; reference getChaseSpeed verbatim in spirit. */
    public float chaseRate() {
        return MathHelper.clamp(Math.abs(this.getSpeed()) / 16.0F / 20.0F, 0.0F, 1.0F);
    }

    /** True while fluid may pass (block state ENABLED). */
    public boolean isOpen() {
        if (this.world == null)
            return false;
        IBlockState state = this.world.getBlockState(this.pos);
        return state.getBlock() instanceof BlockFluidValve && state.getValue(BlockFluidValve.ENABLED);
    }

    // ---- fluid pass-through: forward to the handler beyond the valve ----

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                && facing != null && this.isValveFace(facing) && this.isOpen())
            return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                && facing != null && this.isValveFace(facing) && this.isOpen())
            return (T) new ValveForwardingHandler(this, facing);
        return super.getCapability(capability, facing);
    }

    private boolean isValveFace(EnumFacing facing) {
        if (this.world == null)
            return false;
        IBlockState state = this.world.getBlockState(this.pos);
        return state.getBlock() instanceof BlockFluidValve
                && BlockFluidValve.isOpenAt(state, facing);
    }

    /** Handler that delegates every call to the neighbour past the valve. */
    private static class ValveForwardingHandler implements IFluidHandler {
        private final TileEntityFluidValve valve;
        private final EnumFacing face;

        ValveForwardingHandler(TileEntityFluidValve valve, EnumFacing face) {
            this.valve = valve;
            this.face = face;
        }

        @Nullable
        private IFluidHandler target() {
            if (valve.world == null || !valve.isOpen())
                return null;
            BlockPos next = valve.pos.offset(this.face);
            if (!valve.world.isBlockLoaded(next))
                return null;
            TileEntity te = valve.world.getTileEntity(next);
            if (te == null || te == valve)
                return null;
            if (!te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this.face.getOpposite()))
                return null;
            return te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this.face.getOpposite());
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            IFluidHandler t = this.target();
            return t == null ? new IFluidTankProperties[0] : t.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            IFluidHandler t = this.target();
            return t == null ? 0 : t.fill(resource, doFill);
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            IFluidHandler t = this.target();
            return t == null ? null : t.drain(resource, doDrain);
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            IFluidHandler t = this.target();
            return t == null ? null : t.drain(maxDrain, doDrain);
        }
    }

    // ---- persistence / sync (same shape as the hose pulley TE) ----

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setFloat("pointer", this.pointer);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.pointer = compound.getFloat("pointer");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeFloat(this.pointer);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        this.pointer = buf.readFloat();
    }
}
