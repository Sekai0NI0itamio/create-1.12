package nl.melonstudios.create.tileentity.logistics;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.item.ItemPackage;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import java.io.IOException;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Frogport: a addressed package outlet. It holds one package at a time and,
 * when the package address matches its filter (empty filter takes any box),
 * empties the box into the inventory below first, then the ones beside it.
 * Anything that does not fit drops into the world. The filter is stamped
 * from a package by sneak-using one on the block.
 */
public class TileEntityFrogport extends TileEntityOptimizedBase {
    private static final int CADENCE = 20;
    private static final EnumFacing[] PUSH_ORDER = {
            EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH,
            EnumFacing.WEST, EnumFacing.EAST, EnumFacing.UP,
    };

    public String addressFilter = "";
    public ItemStack heldPackage = ItemStack.EMPTY;
    private int cooldown;

    private final IItemHandler buffer = new FrogportBuffer();

    public boolean matchesFilter(ItemStack box) {
        if (box.isEmpty() || box.getItem() != ItemInit.PACKAGE) return false;
        if (this.addressFilter.trim().isEmpty()) return true;
        return this.addressFilter.equals(ItemPackage.getAddress(box));
    }

    public boolean acceptsPackage(ItemStack box) {
        return this.heldPackage.isEmpty() && this.matchesFilter(box);
    }

    /** Stores one box; returns true when it fit. Caller shrinks the giver. */
    public boolean insertPackage(ItemStack box) {
        if (!this.acceptsPackage(box)) return false;
        this.heldPackage = box.copy();
        this.heldPackage.setCount(1);
        this.cooldown = 0;
        this.sync();
        return true;
    }

    public int comparatorLevel() {
        return this.heldPackage.isEmpty() ? 0 : 15;
    }

    @Override
    public void tick() {
        if (this.world.isRemote) return;
        if (--this.cooldown > 0) return;
        this.cooldown = CADENCE;
        if (this.heldPackage.isEmpty()) {
            if (this.pullFromNeighbours()) this.sync();
            return;
        }
        if (!this.matchesFilter(this.heldPackage)) return;
        this.emptyHeldBox();
    }

    @Override
    public void tickLazy() {
    }

    /** Unpacks the held box into neighbouring inventories, drops leftovers. */
    private void emptyHeldBox() {
        for (ItemStack contents : ItemPackage.getContents(this.heldPackage)) {
            if (contents.isEmpty()) continue;
            ItemStack rest = this.pushToNeighbours(contents.copy());
            if (!rest.isEmpty()) {
                this.drop(rest);
            }
        }
        // An empty box is still consumed: the delivery happened.
        this.heldPackage = ItemStack.EMPTY;
        this.world.playSound(null, this.pos, SoundInit.frogport_deposit,
                SoundCategory.BLOCKS, 0.9F, 1.0F);
        this.sync();
    }

    private ItemStack pushToNeighbours(ItemStack stack) {
        for (EnumFacing side : PUSH_ORDER) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            TileEntity te = this.world.getTileEntity(this.pos.offset(side));
            if (te == null || te == this) continue;
            if (!te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) continue;
            IItemHandler target = te.getCapability(
                    CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            if (target == null) continue;
            for (int slot = 0; slot < target.getSlots() && !stack.isEmpty(); slot++) {
                stack = target.insertItem(slot, stack, false);
            }
        }
        return stack;
    }

    /** Sips one matching box from a neighbouring inventory into the buffer. */
    private boolean pullFromNeighbours() {
        for (EnumFacing side : EnumFacing.VALUES) {
            TileEntity te = this.world.getTileEntity(this.pos.offset(side));
            if (te == null || te == this) continue;
            if (!te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) continue;
            IItemHandler source = te.getCapability(
                    CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            if (source == null) continue;
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack ghost = source.extractItem(slot, 1, true);
                if (!this.matchesFilter(ghost)) continue;
                ItemStack taken = source.extractItem(slot, 1, false);
                if (taken.isEmpty()) continue;
                this.heldPackage = taken;
                this.cooldown = 0;
                return true;
            }
        }
        return false;
    }

    private void drop(ItemStack stack) {
        EntityItem entity = new EntityItem(this.world,
                this.pos.getX() + 0.5, this.pos.getY() + 1.1, this.pos.getZ() + 0.5, stack);
        entity.motionX = 0;
        entity.motionZ = 0;
        this.world.spawnEntity(entity);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("AddressFilter", this.addressFilter);
        if (!this.heldPackage.isEmpty()) {
            nbt.setTag("HeldPackage", this.heldPackage.writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.addressFilter = nbt.getString("AddressFilter");
        if (nbt.hasKey("HeldPackage", 10)) {
            this.heldPackage = new ItemStack(nbt.getCompoundTag("HeldPackage"));
        } else {
            this.heldPackage = ItemStack.EMPTY;
        }
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("AddressFilter", this.addressFilter);
        nbt.setBoolean("HasPackage", !this.heldPackage.isEmpty());
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.addressFilter = nbt.getString("AddressFilter");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) this.buffer;
        }
        return super.getCapability(capability, facing);
    }

    /** One-slot intake: pipes and hoppers may feed boxes in, never take out. */
    private final class FrogportBuffer implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? TileEntityFrogport.this.heldPackage : ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (slot != 0 || !TileEntityFrogport.this.acceptsPackage(stack)) return stack;
            if (!simulate) {
                TileEntityFrogport.this.heldPackage = stack.copy();
                TileEntityFrogport.this.heldPackage.setCount(1);
                TileEntityFrogport.this.cooldown = 0;
                TileEntityFrogport.this.sync();
            }
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        io.netty.buffer.ByteBuf temp = io.netty.buffer.Unpooled.buffer();
        net.minecraftforge.fml.common.network.ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(net.minecraftforge.fml.common.network.ByteBufUtils.readTag(buf));
    }
}
