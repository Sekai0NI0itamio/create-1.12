package nl.melonstudios.create.tileentity.logistics;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nl.melonstudios.create.block.logistics.BlockVault;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Item vault storage cell. Forms axis-aligned multiblocks (up to 3x3 cross
 * section, length up to 3x width) with a shared inventory, mirroring the
 * 1.20.1 ItemVaultBlockEntity behaviour through 1.12 capabilities.
 */
public class TileEntityVault extends TileEntityOptimizedBase {
    public static final int SLOTS_PER_BLOCK = 20;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS_PER_BLOCK) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            TileEntityVault.this.markDirty();
            TileEntityVault.this.updateComparator();
        }
    };

    private BlockPos controller;
    private int radius = 1;
    private int length = 1;
    private boolean needsForm = true;
    private IItemHandler combinedCache;

    public ItemStackHandler getOwnInventory() {
        return this.inventory;
    }

    public boolean isController() {
        return this.controller == null || this.controller.equals(this.pos);
    }

    @Nullable
    public TileEntityVault getControllerTE() {
        if (this.isController()) {
            return this;
        }
        if (this.world == null || this.controller == null) {
            return null;
        }
        TileEntity te = this.world.getTileEntity(this.controller);
        return te instanceof TileEntityVault ? (TileEntityVault) te : null;
    }

    public EnumFacing.Axis getAxis() {
        if (this.world == null) {
            return EnumFacing.Axis.X;
        }
        if (!(this.world.getBlockState(this.pos).getBlock() instanceof BlockVault)) {
            return EnumFacing.Axis.X;
        }
        return this.world.getBlockState(this.pos).getValue(BlockVault.AXIS);
    }

    public void requestForm() {
        this.needsForm = true;
    }

    void clearNeedsForm() {
        this.needsForm = false;
    }

    public void setMultiblock(@Nullable BlockPos controller, int radius, int length) {
        boolean changed = (this.controller == null ? controller != null : !this.controller.equals(controller))
                || this.radius != radius || this.length != length;
        this.controller = controller;
        this.radius = radius;
        this.length = length;
        this.combinedCache = null;
        if (changed) {
            this.markDirty();
            this.sync();
            if (this.world != null && !this.world.isRemote) {
                BlockVault.updateLargeFlag(this.world, this.pos, radius > 2);
                this.updateComparator();
            }
        }
    }

    public IItemHandler getHandler() {
        if (this.isController()) {
            if (this.combinedCache == null) {
                this.combinedCache = this.buildCombined();
            }
            return this.combinedCache;
        }
        TileEntityVault controllerTE = this.getControllerTE();
        if (controllerTE == null) {
            return this.inventory;
        }
        return controllerTE.getHandler();
    }

    private IItemHandler buildCombined() {
        List<TileEntityVault> members = new ArrayList<>();
        if (this.world == null) {
            members.add(this);
            return new VaultCombinedHandler(members);
        }
        EnumFacing.Axis axis = this.getAxis();
        BlockPos min = this.isController() ? this.pos : this.controller;
        for (int i = 0; i < this.radius; i++) {
            for (int j = 0; j < this.radius; j++) {
                for (int k = 0; k < this.length; k++) {
                    BlockPos p = axis == EnumFacing.Axis.X
                            ? min.add(k, i, j)
                            : min.add(i, j, k);
                    TileEntity te = this.world.getTileEntity(p);
                    if (te instanceof TileEntityVault) {
                        members.add((TileEntityVault) te);
                    }
                }
            }
        }
        if (members.isEmpty()) {
            members.add(this);
        }
        return new VaultCombinedHandler(members);
    }

    public int getComparatorSignal() {
        TileEntityVault controllerTE = this.getControllerTE();
        IItemHandler handler = controllerTE == null ? this.inventory : controllerTE.getHandler();
        return VaultCombinedHandler.calcRedstone(handler);
    }

    private void updateComparator() {
        if (this.world != null && !this.world.isRemote
                && this.world.getBlockState(this.pos).getBlock() instanceof BlockVault) {
            this.world.updateComparatorOutputLevel(this.pos,
                    this.world.getBlockState(this.pos).getBlock());
        }
    }

    @Override
    public void tick() {
        if (this.needsForm && this.world != null && !this.world.isRemote) {
            VaultConnectivity.form(this);
        }
    }

    @Override
    public void tickLazy() {
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setTag("Inventory", this.inventory.serializeNBT());
        if (this.controller != null) {
            nbt.setLong("Controller", this.controller.toLong());
        }
        nbt.setInteger("Size", this.radius);
        nbt.setInteger("Length", this.length);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.inventory.deserializeNBT(nbt.getCompoundTag("Inventory"));
        this.controller = nbt.hasKey("Controller") ? BlockPos.fromLong(nbt.getLong("Controller")) : null;
        this.radius = nbt.hasKey("Size") ? nbt.getInteger("Size") : 1;
        this.length = nbt.hasKey("Length") ? nbt.getInteger("Length") : 1;
        this.combinedCache = null;
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        if (this.controller != null) {
            nbt.setLong("Controller", this.controller.toLong());
        }
        nbt.setInteger("Size", this.radius);
        nbt.setInteger("Length", this.length);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.controller = nbt.hasKey("Controller") ? BlockPos.fromLong(nbt.getLong("Controller")) : null;
        this.radius = nbt.hasKey("Size") ? nbt.getInteger("Size") : 1;
        this.length = nbt.hasKey("Length") ? nbt.getInteger("Length") : 1;
        this.combinedCache = null;
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        BlockPos c = this.controller == null ? this.pos : this.controller;
        buf.writeInt(c.getX());
        buf.writeInt(c.getY());
        buf.writeInt(c.getZ());
        buf.writeInt(this.radius);
        buf.writeInt(this.length);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.controller = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        if (this.controller.equals(this.pos)) {
            this.controller = null;
        }
        this.radius = buf.readInt();
        this.length = buf.readInt();
        this.combinedCache = null;
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return new AxisAlignedBB(this.pos.getX(), this.pos.getY(), this.pos.getZ(),
                this.pos.getX() + 1, this.pos.getY() + 1, this.pos.getZ() + 1);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) this.getHandler();
        }
        return super.getCapability(capability, facing);
    }

    public static void dropContents(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityVault)) {
            return;
        }
        ItemStackHandler handler = ((TileEntityVault) te).inventory;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.inventory.InventoryHelper.spawnItemStack(world,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
            }
        }
    }
}
