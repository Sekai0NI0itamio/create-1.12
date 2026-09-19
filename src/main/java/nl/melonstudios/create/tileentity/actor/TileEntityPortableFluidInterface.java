package nl.melonstudios.create.tileentity.actor;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/**
 * Portable fluid interface TE: fluid counterpart to {@link TileEntityStorageInterface}.
 * Shares the contraption-link state machine in {@link TileEntityContraptionInterfaceBase}
 * (connection search, disconnection timer, redstone gating, comparator signal).
 *
 * Reference behavior ({@code PortableFluidInterfaceBlockEntity}): while linked, every
 * fluid call is forwarded to the mounted contraption's tank collection and each real
 * transfer refreshes the link timer; while idle it exposes an empty 0-tank handler.
 *
 * NOTE: the backport {@code ContraptionInventory} currently collects contraption tanks
 * but exposes no fluid getter, so live tank forwarding is pending lead support
 * (see NEEDS-LEAD in the family report). Until then this handler is honestly empty:
 * fill returns 0 and drain returns EMPTY, gated on the link state exactly like the
 * reference gates on {@code isConnected()}/{@code canTransfer()}.
 */
public class TileEntityPortableFluidInterface extends TileEntityContraptionInterfaceBase implements IFluidHandler {
    public TileEntityPortableFluidInterface() {
        super();
    }

    public boolean isFluidConnected() {
        return this.isConnected();
    }

    @Override
    public int getTanks() {
        return 0;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return null;
    }

    @Override
    public int getTankCapacity(int tank) {
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (!this.isConnected()) return 0;
        return this.getInventory().getTankRepresentation().fill(resource, doFill);
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (!this.canTransfer()) return null;
        return this.getInventory().getTankRepresentation().drain(resource, doDrain);
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (!this.canTransfer()) return null;
        return this.getInventory().getTankRepresentation().drain(maxDrain, doDrain);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return (T) this;
        return super.getCapability(capability, facing);
    }
}
