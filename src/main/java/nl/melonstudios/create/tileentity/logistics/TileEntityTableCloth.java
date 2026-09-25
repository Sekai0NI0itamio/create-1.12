package nl.melonstudios.create.tileentity.logistics;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import java.io.IOException;

/**
 * Holds the up-to-4 single-item display stacks shown on a table cloth.
 */
public class TileEntityTableCloth extends TileEntityOptimizedBase {
    public ItemStack[] displayed = {
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
    };

    public int filledSlots() {
        int n = 0;
        for (ItemStack stack : displayed) {
            if (!stack.isEmpty()) n++;
        }
        return n;
    }

    public void addShown(ItemStack stack) {
        for (int i = 0; i < displayed.length; i++) {
            if (displayed[i].isEmpty()) {
                displayed[i] = stack;
                return;
            }
        }
    }

    public ItemStack takeLast() {
        for (int i = displayed.length - 1; i >= 0; i--) {
            if (!displayed[i].isEmpty()) {
                ItemStack taken = displayed[i];
                displayed[i] = ItemStack.EMPTY;
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound nbt = super.writeToNBT(compound);
        for (int i = 0; i < displayed.length; i++) {
            if (!displayed[i].isEmpty()) nbt.setTag("Shown" + i, displayed[i].writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        for (int i = 0; i < displayed.length; i++) {
            if (nbt.hasKey("Shown" + i, 10)) {
                displayed[i] = new ItemStack(nbt.getCompoundTag("Shown" + i));
            } else displayed[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        for (ItemStack stack : displayed) {
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
        for (int i = 0; i < displayed.length; i++) {
            if (buf.readBoolean()) {
                displayed[i] = StackUtil.readItemStack(buf, true, true);
            } else displayed[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        for (int i = 0; i < displayed.length; i++) {
            if (!displayed[i].isEmpty()) nbt.setTag("Shown" + i, displayed[i].writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        for (int i = 0; i < displayed.length; i++) {
            if (nbt.hasKey("Shown" + i, 10)) {
                displayed[i] = new ItemStack(nbt.getCompoundTag("Shown" + i));
            } else displayed[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public void tickLazy() {
    }
}
