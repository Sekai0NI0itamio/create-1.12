package nl.melonstudios.create.kinetics.contraption;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.util.filter.IItemFilter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ContraptionInventory {
    private final List<IItemHandler> inventories = new ArrayList<>();
    private IItemHandler inventoryRepresentation = new InventoryRepresentation(this.inventories);
    private final List<IFluidHandler> tanks = new ArrayList<>();
    private IFluidHandler tankRepresentation = null;

    public void reindex(Contraption contraption) {
        this.inventories.clear();
        this.tanks.clear();
        for (TileEntity te : contraption.tileEntities.values()) {
            if (Contraption.isValidInventory(te)) {
                if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                    IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                    this.inventories.add(handler);
                }
                if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
                    IFluidHandler handler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
                    this.tanks.add(handler);
                }
            }
        }
        this.inventoryRepresentation = new InventoryRepresentation(this.inventories);
        this.tankRepresentation = new CombinedTanks(this.tanks);
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (IItemHandler handler : this.inventories) {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (handler.isItemValid(i, stack)) {
                    stack = handler.insertItem(i, stack, simulate);
                    if (stack.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }
    public ItemStack retrieveItem(@Nullable Item item, int metadata, int max, boolean simulate) {
        if (max <= 0) return ItemStack.EMPTY;
        boolean ignoreDamage = item == null || !item.getHasSubtypes() || metadata == -1;
        for (IItemHandler handler : this.inventories) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (item == null) return handler.extractItem(i, max, simulate);
                if (stack.getItem() == item && (ignoreDamage || stack.getMetadata() == metadata)) {
                    return handler.extractItem(i, max, simulate);
                }
            }
        }
        return ItemStack.EMPTY;
    }
    public ItemStack retrieveItem(@Nullable IItemFilter filter, int max, boolean simulate) {
        if (max <= 0) return ItemStack.EMPTY;
        for (IItemHandler handler : this.inventories) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (filter == null) return handler.extractItem(i, max, simulate);
                if (filter.matches(stack)) {
                    return handler.extractItem(i, max, simulate);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean hasNoInventories() {
        return this.inventories.isEmpty();
    }

    public boolean hasNoTanks() {
        return this.tanks.isEmpty();
    }

    public IItemHandler getInventoryRepresentation() {
        return this.inventoryRepresentation;
    }

    public IFluidHandler getTankRepresentation() {
        return this.tankRepresentation;
    }

    private static class CombinedTanks implements IFluidHandler {
        private final List<IFluidHandler> tanks;

        CombinedTanks(List<IFluidHandler> tanks) {
            this.tanks = tanks;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (resource == null || resource.amount <= 0) {
                return 0;
            }
            int filled = 0;
            FluidStack remaining = resource.copy();
            for (IFluidHandler tank : this.tanks) {
                int n = tank.fill(remaining, doFill);
                filled += n;
                remaining.amount -= n;
                if (remaining.amount <= 0) {
                    break;
                }
            }
            return filled;
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || resource.amount <= 0) {
                return null;
            }
            FluidStack drained = null;
            int remaining = resource.amount;
            for (IFluidHandler tank : this.tanks) {
                FluidStack part = tank.drain(new FluidStack(resource, remaining), doDrain);
                if (part == null || part.amount <= 0) {
                    continue;
                }
                if (drained == null) {
                    drained = part.copy();
                } else {
                    drained.amount += part.amount;
                }
                remaining -= part.amount;
                if (remaining <= 0) {
                    break;
                }
            }
            return drained;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (maxDrain <= 0) {
                return null;
            }
            FluidStack drained = null;
            int remaining = maxDrain;
            for (IFluidHandler tank : this.tanks) {
                FluidStack part = tank.drain(remaining, doDrain);
                if (part == null || part.amount <= 0) {
                    continue;
                }
                if (drained == null) {
                    drained = part.copy();
                } else if (drained.isFluidEqual(part)) {
                    drained.amount += part.amount;
                } else {
                    break;
                }
                remaining -= part.amount;
                if (remaining <= 0) {
                    break;
                }
            }
            return drained;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            List<IFluidTankProperties> props = new ArrayList<>();
            for (IFluidHandler tank : this.tanks) {
                IFluidTankProperties[] sub = tank.getTankProperties();
                if (sub != null) {
                    for (IFluidTankProperties p : sub) {
                        props.add(p);
                    }
                }
            }
            return props.toArray(new IFluidTankProperties[0]);
        }
    }

    public static ContraptionInventory empty() {
        return Empty.INSTANCE;
    }

    private static class Empty extends ContraptionInventory {
        private static final ContraptionInventory INSTANCE = new Empty();

        private Empty() {

        }

        @Override
        public void reindex(Contraption contraption) {

        }

        @Override
        public ItemStack insertItem(ItemStack stack, boolean simulate) {
            return stack;
        }
        @Override
        public ItemStack retrieveItem(@Nullable Item item, int metadata, int max, boolean simulate) {
            return ItemStack.EMPTY;
        }
        @Override
        public ItemStack retrieveItem(@Nullable IItemFilter filter, int max, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean hasNoInventories() {
            return true;
        }

        @Override
        public IItemHandler getInventoryRepresentation() {
            return Inventory.INSTANCE;
        }

        @Override
        public IFluidHandler getTankRepresentation() {
            return EmptyTanks.INSTANCE;
        }

        private static class EmptyTanks implements IFluidHandler {
            private static final IFluidHandler INSTANCE = new EmptyTanks();

            @Override
            public int fill(FluidStack resource, boolean doFill) {
                return 0;
            }

            @Nullable
            @Override
            public FluidStack drain(FluidStack resource, boolean doDrain) {
                return null;
            }

            @Nullable
            @Override
            public FluidStack drain(int maxDrain, boolean doDrain) {
                return null;
            }

            @Override
            public IFluidTankProperties[] getTankProperties() {
                return new IFluidTankProperties[0];
            }
        }

        private static class Inventory implements IItemHandler {
            private static final IItemHandler INSTANCE = new Inventory();

            @Override
            public int getSlots() {
                return 0;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return ItemStack.EMPTY;
            }
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 0;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        }
    }

    private static class InventoryRepresentation implements IItemHandler {
        private final List<IItemHandler> inventories;
        private final int size;
        private InventoryRepresentation(List<IItemHandler> inventories) {
            this.inventories = inventories;
            int _size = 0;
            for (IItemHandler inventory : inventories) {
                _size += inventory.getSlots();
            }
            this.size = _size;
        }

        @Override
        public int getSlots() {
            return this.size;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            for (IItemHandler inventory : this.inventories) {
                int s = inventory.getSlots();
                if (slot < s) return inventory.getStackInSlot(slot);
                slot -= s;
            }
            throw new IndexOutOfBoundsException(slot + " out of bounds for " + this.size);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            for (IItemHandler inventory : this.inventories) {
                int s = inventory.getSlots();
                if (slot < s) return inventory.insertItem(slot, stack, simulate);
                slot -= s;
            }
            throw new IndexOutOfBoundsException(slot + " out of bounds for " + this.size);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            for (IItemHandler inventory : this.inventories) {
                int s = inventory.getSlots();
                if (slot < s) return inventory.extractItem(slot, amount, simulate);
                slot -= s;
            }
            throw new IndexOutOfBoundsException(slot + " out of bounds for " + this.size);
        }

        @Override
        public int getSlotLimit(int slot) {
            for (IItemHandler inventory : this.inventories) {
                int s = inventory.getSlots();
                if (slot < s) return inventory.getSlotLimit(slot);
                slot -= s;
            }
            throw new IndexOutOfBoundsException(slot + " out of bounds for " + this.size);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            for (IItemHandler inventory : this.inventories) {
                int s = inventory.getSlots();
                if (slot < s) return inventory.isItemValid(slot, stack);
                slot -= s;
            }
            throw new IndexOutOfBoundsException(slot + " out of bounds for " + this.size);
        }
    }
}
