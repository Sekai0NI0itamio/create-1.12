package nl.melonstudios.create.tileentity.logistics;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.block.logistics.BlockPostbox;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;

/**
 * Postbox TE: a single box slot with lid + flag state. Ports the reference
 * PostboxBlockEntity observable behaviour: the lid OPEN blockstate follows
 * whether a box sits inside, the signal flag reads raised (1) while filled,
 * contents persist, automation can insert/extract through the item handler.
 */
public class TileEntityPostbox extends TileEntityOptimizedBase {
    private ItemStack box = ItemStack.EMPTY;
    /** Signal flag 0/1 for renderers; raised while a box sits inside. */
    public int flag = 0;

    public ItemStack getBox() {
        return this.box.copy();
    }

    public boolean isFilled() {
        return !this.box.isEmpty();
    }

    public boolean insertBox(ItemStack stack) {
        if (stack.isEmpty() || !this.box.isEmpty()) return false;
        ItemStack one = stack.copy();
        one.setCount(1);
        this.box = one;
        this.afterChange();
        return true;
    }

    public ItemStack takeBox() {
        ItemStack taken = this.box.copy();
        this.box = ItemStack.EMPTY;
        this.afterChange();
        return taken;
    }

    public void dropContents() {
        if (!this.box.isEmpty() && this.world != null) {
            StackUtil.dropItemsAt(this.world, this.pos, this.box.copy());
            this.box = ItemStack.EMPTY;
        }
    }

    private void afterChange() {
        boolean filled = !this.box.isEmpty();
        this.flag = filled ? 1 : 0;
        if (this.world != null && !this.world.isRemote) {
            IBlockState state = this.world.getBlockState(this.pos);
            if (state.getBlock() instanceof BlockPostbox
                    && state.getValue(BlockPostbox.OPEN) != filled) {
                Utils.setBlockTESafe(this.world, this.pos,
                        state.withProperty(BlockPostbox.OPEN, filled), 3);
            }
        }
        this.markDirty();
        this.sync();
    }

    private final IItemHandler handler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? TileEntityPostbox.this.getBox() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !TileEntityPostbox.this.box.isEmpty()) return stack;
            if (simulate) return ItemStack.EMPTY;
            TileEntityPostbox.this.insertBox(stack);
            ItemStack remainder = stack.copy();
            remainder.shrink(1);
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || TileEntityPostbox.this.box.isEmpty()) return ItemStack.EMPTY;
            if (simulate) return TileEntityPostbox.this.getBox();
            return TileEntityPostbox.this.takeBox();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    @Override
    public void tick() {
    }

    @Override
    public void tickLazy() {
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (!this.box.isEmpty()) nbt.setTag("Box", this.box.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("Flag", this.flag);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.box = nbt.hasKey("Box", 10) ? new ItemStack(nbt.getCompoundTag("Box")) : ItemStack.EMPTY;
        this.flag = nbt.getInteger("Flag");
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        if (!this.box.isEmpty()) nbt.setTag("Box", this.box.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("Flag", this.flag);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.box = nbt.hasKey("Box", 10) ? new ItemStack(nbt.getCompoundTag("Box")) : ItemStack.EMPTY;
        this.flag = nbt.getInteger("Flag");
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
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
