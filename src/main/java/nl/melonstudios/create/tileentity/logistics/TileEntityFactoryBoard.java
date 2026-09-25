package nl.melonstudios.create.tileentity.logistics;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import java.io.IOException;

/**
 * Holds the 4 ghost filter stacks pinned on a factory board.
 * Ghosts are display-only copies; pinning never consumes items.
 */
public class TileEntityFactoryBoard extends TileEntityOptimizedBase {
    public ItemStack[] filters = {
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
    };

    public int filledSlots() {
        int n = 0;
        for (ItemStack stack : filters) {
            if (!stack.isEmpty()) n++;
        }
        return n;
    }

    public void pinGhost(ItemStack ghost) {
        for (int i = 0; i < filters.length; i++) {
            if (filters[i].isEmpty()) {
                filters[i] = ghost;
                return;
            }
        }
    }

    public void clear() {
        for (int i = 0; i < filters.length; i++) {
            filters[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound nbt = super.writeToNBT(compound);
        for (int i = 0; i < filters.length; i++) {
            if (!filters[i].isEmpty()) nbt.setTag("Filter" + i, filters[i].writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        for (int i = 0; i < filters.length; i++) {
            if (nbt.hasKey("Filter" + i, 10)) {
                filters[i] = new ItemStack(nbt.getCompoundTag("Filter" + i));
            } else filters[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        for (ItemStack stack : filters) {
            if (stack.isEmpty()) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                StackUtil.writeItemStack(stack, buf, true, true);
            }
        }
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        for (int i = 0; i < filters.length; i++) {
            if (buf.readBoolean()) {
                filters[i] = StackUtil.readItemStack(buf, true, true);
            } else filters[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        for (int i = 0; i < filters.length; i++) {
            if (!filters[i].isEmpty()) nbt.setTag("Filter" + i, filters[i].writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        for (int i = 0; i < filters.length; i++) {
            if (nbt.hasKey("Filter" + i, 10)) {
                filters[i] = new ItemStack(nbt.getCompoundTag("Filter" + i));
            } else filters[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public void tickLazy() {
    }
}
