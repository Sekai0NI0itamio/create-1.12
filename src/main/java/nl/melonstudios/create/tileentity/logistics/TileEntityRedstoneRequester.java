package nl.melonstudios.create.tileentity.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.init.SoundInit;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import java.io.IOException;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Redstone requester: holds a small encoded order (up to 9 stacks) plus the
 * address it is meant for. A rising redstone edge files that order with the
 * first packager serving the address, directly or through a packager link,
 * following the same path a stock ticker order would take.
 */
public class TileEntityRedstoneRequester extends TileEntityOptimizedBase {
    private static final int RANGE = 3;

    public String address = "";
    public final List<ItemStack> encoded = new ArrayList<>();
    public boolean lastSuccess;

    public TileEntityRedstoneRequester() {
        for (int i = 0; i < 9; i++) this.encoded.add(ItemStack.EMPTY);
    }

    public boolean hasOrder() {
        for (ItemStack stack : this.encoded) {
            if (!stack.isEmpty()) return true;
        }
        return false;
    }

    public int encodedCount() {
        int n = 0;
        for (ItemStack stack : this.encoded) {
            if (!stack.isEmpty()) n++;
        }
        return n;
    }

    public int comparatorLevel() {
        return this.hasOrder() ? 15 : 0;
    }

    @Override
    public void tick() {
    }

    @Override
    public void tickLazy() {
    }

    /** Files the encoded order; called on a rising redstone edge. */
    public void triggerRequest() {
        if (this.world.isRemote || !this.hasOrder()) return;
        TileEntityPackager packager = TileEntityPackagerLink.packagerForAddress(
                this.world, this.pos, this.address, RANGE);
        if (packager == null) {
            this.lastSuccess = false;
            this.world.playSound(null, this.pos, SoundInit.deny, SoundCategory.BLOCKS, 1.0F, 1.0F);
            this.sync();
            return;
        }
        int n = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack want = this.encoded.get(i);
            if (want.isEmpty()) continue;
            ItemStack request = want.copy();
            request.setCount(Math.min(64, want.getCount()));
            packager.requests.set(n++, request);
        }
        while (n < 9) packager.requests.set(n++, ItemStack.EMPTY);
        packager.address = this.address == null ? "" : this.address;
        packager.packageAll();
        packager.sync();
        this.lastSuccess = true;
        this.world.playSound(null, this.pos, SoundInit.stock_ticker_request,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
        this.sync();
    }

    /**
     * Copies up to 9 stacks from the inventory below as the encoded order and
     * adopts the address of the nearest packager or link. Returns the count.
     */
    public int captureFromBelow() {
        if (this.world.isRemote) return 0;
        TileEntity te = this.world.getTileEntity(this.pos.down());
        if (te == null
                || !te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP)) return 0;
        IItemHandler stock = te.getCapability(
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        if (stock == null) return 0;
        int n = 0;
        for (int slot = 0; slot < stock.getSlots() && n < 9; slot++) {
            ItemStack inSlot = stock.getStackInSlot(slot);
            if (inSlot.isEmpty()) continue;
            ItemStack request = inSlot.copy();
            request.setCount(Math.min(64, inSlot.getCount()));
            this.encoded.set(n++, request);
        }
        while (n < 9) this.encoded.set(n++, ItemStack.EMPTY);
        TileEntityPackager near = TileEntityPackagerLink.packagerForAddress(
                this.world, this.pos, this.address, 2);
        if (near == null) near = TileEntityPackagerLink.packagerForAddress(this.world, this.pos, "", 2);
        if (near != null) this.address = near.address;
        this.lastSuccess = false;
        this.sync();
        return this.encodedCount();
    }

    /** Nearest stock position, used only for status text. */
    public BlockPos stockPos() {
        TileEntity te = this.world.getTileEntity(this.pos.down());
        return te == null ? null : this.pos.down();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("Address", this.address == null ? "" : this.address);
        nbt.setBoolean("Success", this.lastSuccess);
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : this.encoded) {
            list.appendTag(stack.isEmpty()
                    ? new NBTTagCompound() : stack.writeToNBT(new NBTTagCompound()));
        }
        nbt.setTag("Encoded", list);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.address = nbt.getString("Address");
        this.lastSuccess = nbt.getBoolean("Success");
        this.encoded.clear();
        if (nbt.hasKey("Encoded", 9)) {
            NBTTagList list = nbt.getTagList("Encoded", 10);
            for (int i = 0; i < list.tagCount() && i < 9; i++) {
                this.encoded.add(new ItemStack(list.getCompoundTagAt(i)));
            }
        }
        while (this.encoded.size() < 9) this.encoded.add(ItemStack.EMPTY);
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("Address", this.address == null ? "" : this.address);
        nbt.setBoolean("Success", this.lastSuccess);
        nbt.setInteger("EncodedCount", this.encodedCount());
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.address = nbt.getString("Address");
        this.lastSuccess = nbt.getBoolean("Success");
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
