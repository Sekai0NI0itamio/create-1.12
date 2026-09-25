package nl.melonstudios.create.tileentity.fluid;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;

import javax.annotation.Nullable;

/**
 * Smart pipe tile: a relay pipe with a ghost fluid filter.
 *
 * <p>Translated from the reference SmartFluidPipeBlockEntity (never
 * pasted): the reference keeps a fluid filter slot and refuses to pull any
 * fluid that fails the test. The backport pipe network already consults
 * {@link TileEntityFluidPipe#matchesFilter} on every walk, so this class
 * only adds the ghost container item behind the allow-listed name (for
 * goggles/rendering) plus persistence of both halves.</p>
 */
public class TileEntitySmartFluidPipe extends TileEntityFluidPipe {
    /** Ghost container the filter was taken from; never consumed. */
    private ItemStack filterGhost = ItemStack.EMPTY;

    public TileEntitySmartFluidPipe() {
        super();
    }

    /**
     * Set the allow-listed fluid. A null fluid clears the filter back to
     * "any", mirroring the reference empty filter slot.
     */
    public void setFluidFilter(@Nullable Fluid fluid, ItemStack ghost) {
        this.setFilter(fluid);
        this.filterGhost = fluid == null || ghost.isEmpty() ? ItemStack.EMPTY : ghost.copy();
        if (!this.filterGhost.isEmpty()) this.filterGhost.setCount(1);
        this.markDirty();
    }

    public ItemStack getFilterGhost() {
        return this.filterGhost;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (!this.filterGhost.isEmpty()) {
            nbt.setTag("FilterGhost", this.filterGhost.writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("FilterGhost", 10)) {
            this.filterGhost = new ItemStack(nbt.getCompoundTag("FilterGhost"));
        } else {
            this.filterGhost = ItemStack.EMPTY;
        }
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = super.writePacket();
        String name = this.getFilterName();
        if (name != null) nbt.setString("Filter", name);
        if (!this.filterGhost.isEmpty()) {
            nbt.setTag("FilterGhost", this.filterGhost.writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        super.readPacket(nbt);
        if (nbt.hasKey("FilterGhost", 10)) {
            this.filterGhost = new ItemStack(nbt.getCompoundTag("FilterGhost"));
        } else {
            this.filterGhost = ItemStack.EMPTY;
        }
        // Client-side copy of the allow-list; the server copy persists via
        // NBT. Only touch it when it actually changed to avoid needless
        // re-sync flags from inside a packet read.
        String name = nbt.hasKey("Filter", 8) ? nbt.getString("Filter") : null;
        String current = this.getFilterName();
        if (name == null ? current != null : !name.equals(current)) {
            this.setFilter(name == null ? null : net.minecraftforge.fluids.FluidRegistry.getFluid(name));
        }
    }
}
