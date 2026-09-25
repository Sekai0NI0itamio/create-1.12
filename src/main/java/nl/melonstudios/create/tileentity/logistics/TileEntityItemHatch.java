package nl.melonstudios.create.tileentity.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemHandlerHelper;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import java.io.IOException;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.Nonnull;

/**
 * Item hatch tile entity. Stores only the ghost filter item; the hatch block
 * itself holds no inventory and pushes player items straight into the facing
 * inventory, mirroring the 1.20.1 ItemHatchBlockEntity behaviour.
 */
public class TileEntityItemHatch extends TileEntityOptimizedBase {
    private ItemStack filter = ItemStack.EMPTY;

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

    public boolean test(@Nonnull ItemStack stack) {
        if (this.filter.isEmpty()) {
            return true;
        }
        if (stack.isEmpty()) {
            return false;
        }
        return ItemHandlerHelper.canItemStacksStack(this.filter, stack);
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
        io.netty.buffer.ByteBuf temp = io.netty.buffer.Unpooled.buffer();
        net.minecraftforge.fml.common.network.ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(net.minecraftforge.fml.common.network.ByteBufUtils.readTag(buf));
    }
}
