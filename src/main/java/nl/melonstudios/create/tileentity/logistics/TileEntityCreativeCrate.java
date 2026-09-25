package nl.melonstudios.create.tileentity.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import java.io.IOException;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Creative crate tile entity. Holds a single ghost filter item and exposes a
 * bottomless supply of it, mirroring the 1.20.1 CreativeCrateBlockEntity
 * through 1.12 capabilities.
 *
 * Slot layout (matches BottomlessItemHandler: 2 slots):
 * - slot 0: reads as a full stack of the filter item; extraction yields up to
 *   the requested amount; any insertion is absorbed (remainder EMPTY).
 * - slot 1: always empty utility slot.
 */
public class TileEntityCreativeCrate extends TileEntityOptimizedBase {
    private ItemStack filter = ItemStack.EMPTY;

    private final BottomlessHandler handler = new BottomlessHandler();

    @Nonnull
    public ItemStack getFilter() {
        return this.filter;
    }

    public void setFilter(@Nonnull ItemStack filter) {
        if (filter.isEmpty()) {
            this.filter = ItemStack.EMPTY;
        } else {
            ItemStack ghost = filter.copy();
            ghost.setCount(1);
            this.filter = ghost;
        }
        this.markDirty();
        this.sync();
    }

    public boolean matchesFilter(@Nonnull ItemStack stack) {
        if (this.filter.isEmpty() || stack.isEmpty()) {
            return false;
        }
        return ItemHandlerHelper.canItemStacksStack(this.filter, stack);
    }

    private final class BottomlessHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 0) {
                return ItemStack.EMPTY;
            }
            ItemStack supplied = TileEntityCreativeCrate.this.filter;
            if (supplied.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return ItemHandlerHelper.copyStackWithSize(supplied, supplied.getMaxStackSize());
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0) {
                return ItemStack.EMPTY;
            }
            ItemStack supplied = TileEntityCreativeCrate.this.filter;
            if (supplied.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return ItemHandlerHelper.copyStackWithSize(supplied,
                    Math.min(supplied.getMaxStackSize(), amount));
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return true;
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public void tickLazy() {
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (!this.filter.isEmpty()) {
            nbt.setTag("Filter", this.filter.writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("Filter", 10)) {
            this.filter = new ItemStack(nbt.getCompoundTag("Filter"));
        } else {
            this.filter = ItemStack.EMPTY;
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) this.handler;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        if (!this.filter.isEmpty()) {
            nbt.setTag("Filter", this.filter.writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        if (nbt.hasKey("Filter", 10)) {
            this.filter = new ItemStack(nbt.getCompoundTag("Filter"));
        } else {
            this.filter = ItemStack.EMPTY;
        }
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        ByteBufUtils.writeTag(buf, this.writePacket());
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(ByteBufUtils.readTag(buf));
    }
}
