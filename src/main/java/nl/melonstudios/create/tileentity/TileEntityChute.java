package nl.melonstudios.create.tileentity;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nl.melonstudios.create.tileentity.marker.ITopOpenInventory;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TileEntityChute extends TileEntityOptimizedBase implements IItemHandler, ITopOpenInventory {
    public float randomizedItemRotation;
    public TileEntityChute() {
        super();

        this.setTickRateLazy(10);
    }

    @Override
    public void initializeClient() {
        super.initializeClient();

        this.randomizedItemRotation = this.world.rand.nextInt(360);
    }

    private boolean itemLocked = false;
    public ItemStack stack = ItemStack.EMPTY;

    @Override
    public void tick() {

    }

    @Override
    public void tickLazy() {
        boolean mod = false;
        if (!this.stack.isEmpty()) {
            IItemHandler below = this.getInv(this.pos.down(), EnumFacing.UP);
            if (below != null) {
                for (int i = 0; i < below.getSlots(); i++) {
                    ItemStack ret = below.insertItem(i, this.stack, false);
                    if (ret != this.stack) {
                        mod = true;
                        this.stack = ret;
                    }
                    if (this.stack.isEmpty()) {
                        break;
                    }
                }
                if (mod && below instanceof TileEntityChute) ((TileEntityChute)below).itemLocked = true;
            }
        }

        this.itemLocked = false;

        if (this.stack.isEmpty()) {
            IItemHandler above = this.getInv(this.pos.up(), EnumFacing.DOWN);
            if (above != null) {
                for (int i = 0; i < above.getSlots(); i++) {
                    this.stack = above.extractItem(i, 16, false);
                    if (!this.stack.isEmpty()) {
                        this.itemLocked = true;
                        mod = true;
                        break;
                    }
                }
            }
        }

        if (mod) this.sync();
    }

    @Nullable
    private IItemHandler getInv(BlockPos pos, EnumFacing side) {
        TileEntity te = this.world.getTileEntity(pos);
        if (te == null || !te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) return null;
        if (te instanceof TileEntityChute && ((TileEntityChute)te).itemLocked) return null;
        return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        if (!this.stack.isEmpty()) {
            nbt.setTag("Stack", this.stack.writeToNBT(new NBTTagCompound()));
        }
        nbt.setBoolean("itemLocked", this.itemLocked);

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("Stack", 10)) {
            this.stack = new ItemStack(nbt.getCompoundTag("Stack"));
        }
        this.itemLocked = nbt.getBoolean("itemLocked");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        if (this.stack.isEmpty()) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            StackUtil.writeItemStack(this.stack, buf, true, true);
        }
        buf.writeBoolean(this.itemLocked);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        if (buf.readBoolean()) {
            this.stack = StackUtil.readItemStack(buf, true, true);
        } else this.stack = ItemStack.EMPTY;
        this.itemLocked = buf.readBoolean();
    }

    @Override
    public void destroy() {
        StackUtil.dropItemsAt(this.world, this.pos, this.stack.copy());
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot != 0) throw  new IndexOutOfBoundsException("I only have one slot!");
        return this.stack;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (slot != 0) throw  new IndexOutOfBoundsException("I only have one slot!");
        if (this.stack.isEmpty()) {
            ItemStack copy = stack.copy();
            ItemStack ret = copy.splitStack(16);
            if (!simulate) {
                this.stack = ret;
                this.sync();
            }
            return copy;
        }
        if (ItemHandlerHelper.canItemStacksStack(this.stack, stack)) {
            ItemStack copy = stack.copy();
            ItemStack ret = copy.splitStack(Math.min(this.stack.getMaxStackSize(), this.getSlotLimit(slot)) - this.stack.getCount());
            if (!simulate) {
                this.stack.grow(ret.getCount());
                this.sync();
            }
            return copy;
        }
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;
        if (slot != 0) throw new IndexOutOfBoundsException("I only have one slot!");
        if (simulate) {
            ItemStack copy = this.stack.copy();
            return copy.splitStack(amount);
        }
        this.sync();
        return this.stack.splitStack(amount);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 16;
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

    @Override
    public ItemStack tryInsertItem(ItemStack stack) {
        if (this.stack.isEmpty()) {
            this.stack = stack.splitStack(16);
            this.sync();
            return stack.isEmpty() ? ItemStack.EMPTY : stack;
        } else if (ItemStack.areItemsEqual(this.stack, stack) && ItemStack.areItemStackTagsEqual(this.stack, stack)) {
            int space = this.getSlotLimit(0) - this.stack.getCount();
            if (space > 0) {
                int rem = Math.min(space, stack.getCount());
                stack.shrink(rem);
                this.stack.grow(rem);
                this.sync();
                return stack.isEmpty() ? ItemStack.EMPTY : stack;
            } else return stack;
        } else return stack;
    }

    @Override
    public boolean isInsertionSlotEmpty(ItemStack stack) {
        return this.stack.isEmpty();
    }
}
