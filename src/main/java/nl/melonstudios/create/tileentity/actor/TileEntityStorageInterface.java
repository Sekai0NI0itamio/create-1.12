package nl.melonstudios.create.tileentity.actor;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public class TileEntityStorageInterface extends TileEntityContraptionInterfaceBase implements IItemHandler {
    public TileEntityStorageInterface() {
        super();
    }

    @Override
    public int getSlots() {
        return this.getInventory().getInventoryRepresentation().getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.getInventory().getInventoryRepresentation().getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!this.canTransfer()) return stack;
        ItemStack result = this.getInventory().getInventoryRepresentation().insertItem(slot, stack, simulate);
        if (!simulate && !this.getInventory().hasNoInventories()
                && result.getCount() != stack.getCount()) this.setDisconnectionTimer(20);
        return result;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!this.canTransfer()) return ItemStack.EMPTY;
        ItemStack result = this.getInventory().getInventoryRepresentation().extractItem(slot, amount, simulate);
        if (!simulate && !result.isEmpty()) this.setDisconnectionTimer(20);
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.getInventory().getInventoryRepresentation().getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.getInventory().getInventoryRepresentation().isItemValid(slot, stack);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T)this;
        return super.getCapability(capability, facing);
    }
}
