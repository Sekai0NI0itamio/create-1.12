package nl.melonstudios.create.tileentity.logistics;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.util.SoundCategory;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.item.ItemPackage;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Packager TE: holds up to 9 requested item types; when powered, pulls them
 * from the inventory below and emits package items (NBT: 9 slots + address).
 * Stock ticker writes requests here (same-column ticker below/above).
 */
public class TileEntityPackager extends TileEntityKinetic {
    public final List<ItemStack> requests = new ArrayList<>();
    public String address = "";
    private int cooldown;

    public TileEntityPackager() {
        for (int i = 0; i < 9; i++) requests.add(ItemStack.EMPTY);
    }

    public void packageAll() {
        this.cooldown = 0;
        this.markDirty();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getSpeed() == 0) return;
        if (this.world.isRemote) return;
        if (--this.cooldown > 0) return;
        this.cooldown = 20;

        IItemHandler source = this.attachedInventory();
        if (source == null) return;
        boolean made = false;
        for (int r = 0; r < 9; r++) {
            ItemStack want = this.requests.get(r);
            if (want.isEmpty()) continue;
            ItemStack gathered = this.pull(source, want);
            if (gathered.isEmpty()) continue;
            ItemStack box = new ItemStack(ItemInit.PACKAGE, 1, 0);
            ItemPackage.setContents(box, gathered);
            if (!this.address.isEmpty()) ItemPackage.setAddress(box, this.address);
            StackUtil.spawnItemNoVelocity(this.world, this.pos.getX() + 0.5, this.pos.getY() + 1.1, this.pos.getZ() + 0.5, box);
            this.world.playSound(null, this.pos, SoundInit.packager, SoundCategory.BLOCKS, 1.0F, 1.0F);
            made = true;
        }
        if (made) this.sync();
    }

    private IItemHandler attachedInventory() {
        TileEntity te = this.world.getTileEntity(this.pos.down());
        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP)) {
            return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        }
        return null;
    }

    private ItemStack pull(IItemHandler source, ItemStack want) {
        int need = Math.min(64, want.getCount() <= 0 ? 64 : want.getCount());
        ItemStack gathered = ItemStack.EMPTY;
        for (int i = 0; i < source.getSlots() && need > 0; i++) {
            ItemStack inSlot = source.getStackInSlot(i);
            if (inSlot.isEmpty()) continue;
            if (!ItemStack.areItemsEqual(inSlot, want) || !ItemStack.areItemStackTagsEqual(inSlot, want)) continue;
            ItemStack taken = source.extractItem(i, need, false);
            if (taken.isEmpty()) continue;
            if (gathered.isEmpty()) gathered = taken.copy();
            else gathered.grow(taken.getCount());
            need -= taken.getCount();
        }
        return gathered;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        NBTTagList list = new NBTTagList();
        for (ItemStack s : this.requests) {
            list.appendTag(s.isEmpty() ? new NBTTagCompound() : s.writeToNBT(new NBTTagCompound()));
        }
        nbt.setTag("Requests", list);
        nbt.setString("Address", this.address);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.requests.clear();
        if (nbt.hasKey("Requests", 9)) {
            NBTTagList list = nbt.getTagList("Requests", 10);
            for (int i = 0; i < list.tagCount() && i < 9; i++) {
                this.requests.add(new ItemStack(list.getCompoundTagAt(i)));
            }
        }
        while (this.requests.size() < 9) this.requests.add(ItemStack.EMPTY);
        this.address = nbt.getString("Address");
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("Address", this.address);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.address = nbt.getString("Address");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        ByteBuf temp = Unpooled.buffer();
        ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(ByteBufUtils.readTag(buf));
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return super.hasCapability(capability, facing);
    }
}
