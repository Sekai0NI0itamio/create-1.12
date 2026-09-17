package nl.melonstudios.create.tileentity.logistics;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Combined view over every vault block in a multiblock, mirroring the
 * 1.20.1 SameSizeCombinedInvWrapper behaviour (slots laid out member by
 * member in a stable order).
 */
final class VaultCombinedHandler implements IItemHandlerModifiable {
    private final List<ItemStackHandler> parts = new ArrayList<>();

    VaultCombinedHandler(List<TileEntityVault> members) {
        members.sort((a, b) -> a.getPos().compareTo(b.getPos()));
        for (TileEntityVault te : members) {
            this.parts.add(te.getOwnInventory());
        }
    }

    private static final class SlotRef {
        final ItemStackHandler handler;
        final int slot;

        SlotRef(ItemStackHandler handler, int slot) {
            this.handler = handler;
            this.slot = slot;
        }
    }

    private SlotRef locate(int slot) {
        int i = slot;
        for (ItemStackHandler part : this.parts) {
            if (i < part.getSlots()) {
                return new SlotRef(part, i);
            }
            i -= part.getSlots();
        }
        return null;
    }

    @Override
    public int getSlots() {
        int n = 0;
        for (ItemStackHandler part : this.parts) {
            n += part.getSlots();
        }
        return n;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        SlotRef ref = this.locate(slot);
        return ref == null ? ItemStack.EMPTY : ref.handler.getStackInSlot(ref.slot);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        SlotRef ref = this.locate(slot);
        return ref == null ? stack : ref.handler.insertItem(ref.slot, stack, simulate);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        SlotRef ref = this.locate(slot);
        return ref == null ? ItemStack.EMPTY : ref.handler.extractItem(ref.slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        SlotRef ref = this.locate(slot);
        return ref == null ? 0 : ref.handler.getSlotLimit(ref.slot);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        SlotRef ref = this.locate(slot);
        return ref != null && ref.handler.isItemValid(ref.slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        SlotRef ref = this.locate(slot);
        if (ref != null) {
            ref.handler.setStackInSlot(ref.slot, stack);
        }
    }

    static int calcRedstone(IItemHandler inv) {
        int slots = inv.getSlots();
        if (slots <= 0) {
            return 0;
        }
        float fullness = 0.0F;
        int nonEmpty = 0;
        for (int i = 0; i < slots; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                fullness += (float) stack.getCount() / (float) Math.min(inv.getSlotLimit(i), stack.getMaxStackSize());
                nonEmpty++;
            }
        }
        fullness /= (float) slots;
        return net.minecraft.util.math.MathHelper.floor(fullness * 14.0F) + (nonEmpty > 0 ? 1 : 0);
    }
}
