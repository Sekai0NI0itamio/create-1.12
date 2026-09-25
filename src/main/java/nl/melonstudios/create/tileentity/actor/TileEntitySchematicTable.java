package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.items.ItemStackHandler;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Two-slot workstation inventory behind the schematic table, translated
 * from the reference table storage. Automation can push and pull both
 * slots; the comparator reads how many slots are filled.
 */
public class TileEntitySchematicTable extends TileEntityOptimizedBase {
    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            TileEntitySchematicTable.this.sync();
            TileEntitySchematicTable.this.markDirty();
        }
    };

    /** Comparator fill level: 0 empty, 7 half, 15 full. */
    public int getFillLevel() {
        int filled = 0;
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            if (!this.inventory.getStackInSlot(i).isEmpty()) filled++;
        }
        return filled == 0 ? 0 : filled == 1 ? 7 : 15;
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
        nbt.setTag("Inventory", this.inventory.serializeNBT());
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.inventory.deserializeNBT(nbt.getCompoundTag("Inventory"));
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Inventory", this.inventory.serializeNBT());
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.inventory.deserializeNBT(nbt.getCompoundTag("Inventory"));
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        ByteBuf temp = Unpooled.buffer();
        ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(ByteBufUtils.readTag(buf));
    }
}
