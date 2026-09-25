package nl.melonstudios.create.tileentity.redstone;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Stores the linked-controller stack slotted into the lectern. The
 * reference online-user tracking (who is seated at the lectern) has no
 * backport equivalent, so this keeps the persistent part: the controller
 * item itself.
 */
public class TileEntityLecternController extends TileEntityOptimizedBase {
    private ItemStack controller = ItemStack.EMPTY;

    public ItemStack getController() {
        return this.controller;
    }

    public void setController(ItemStack stack) {
        this.controller = stack;
        this.sync();
        this.markDirty();
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
        nbt.setTag("Controller", this.controller.writeToNBT(new NBTTagCompound()));
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.controller = new ItemStack(nbt.getCompoundTag("Controller"));
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Controller", this.controller.writeToNBT(new NBTTagCompound()));
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.controller = new ItemStack(nbt.getCompoundTag("Controller"));
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
