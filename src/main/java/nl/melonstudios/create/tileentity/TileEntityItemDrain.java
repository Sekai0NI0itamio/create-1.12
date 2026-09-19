package nl.melonstudios.create.tileentity;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import nl.melonstudios.create.tileentity.marker.ITopOpenInventory;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

public class TileEntityItemDrain extends TileEntityOptimizedBase implements ITopOpenInventory {
    public TileEntityItemDrain() {
        super();
    }

    public int roll = 0;
    public EnumFacing rollingDirection = null;
    public ItemStack draining = ItemStack.EMPTY;
    public final FluidTank tank = new FluidTank(1500) {
        @Override
        protected void onContentsChanged() {
            TileEntityItemDrain.this.sync();
        }
    };

    @Override
    public void tick() {
        if (!this.draining.isEmpty()) {
            this.markDirty();
            if (this.roll == 30) {
                IFluidHandlerItem handler = FluidUtil.getFluidHandler(this.draining);
                if (handler != null) {
                    FluidStack drained = handler.drain(this.tank.getCapacity() - this.tank.getFluidAmount(), false);
                    if (drained != null) {
                        FluidStack stored = this.tank.getFluid();
                        if (stored != null) {
                            if (stored.isFluidEqual(drained)) {
                                this.world.playSound(null, this.pos, drained.getFluid().getEmptySound(drained), SoundCategory.BLOCKS, 1.0F, 1.0F);
                                this.tank.fillInternal(handler.drain(this.tank.getCapacity() - this.tank.getFluidAmount(), true), true);
                                this.roll++;
                            }
                        } else {
                            this.world.playSound(null, this.pos, drained.getFluid().getEmptySound(drained), SoundCategory.BLOCKS, 1.0F, 1.0F);
                            this.tank.fillInternal(handler.drain(this.tank.getCapacity(), true), true);
                            this.roll++;
                        }
                        this.draining = handler.getContainer();
                    } else this.roll++;
                } else this.roll++;
                this.sync();
            } else if (this.roll > 60) {
                if (this.rollingDirection != null) {
                    ITopOpenInventory inventory = Utils.cast(this.world.getTileEntity(this.pos.offset(this.rollingDirection)), ITopOpenInventory.class);
                    if (inventory != null) {
                        this.draining = inventory.tryInsertItem(this.draining, this.rollingDirection.getOpposite());
                        if (this.draining.isEmpty()) {
                            this.roll = 0;
                            this.sync();
                        }
                    } else {
                        StackUtil.spawnItemNoVelocity(this.world,
                                this.pos.getX() + 0.5 + this.rollingDirection.getFrontOffsetX()*0.5,
                                this.pos.getY() + 0.8,
                                this.pos.getZ() + 0.5 + this.rollingDirection.getFrontOffsetZ()*0.5,
                                this.draining.copy()
                        );
                        this.draining = ItemStack.EMPTY;
                        this.roll = 0;
                        this.sync();
                    }
                } else {
                    // This should not happen but who knows
                    StackUtil.spawnItemNoVelocity(this.world,
                            this.pos.getX() + 0.5,
                            this.pos.getY() - 0.1,
                            this.pos.getZ() + 0.5,
                            this.draining.copy()
                    );
                    this.draining = ItemStack.EMPTY;
                    this.roll = 0;
                    this.sync();
                }
            } else this.roll++;
        } else this.roll = 0;
    }

    @Override
    public void tickLazy() {

    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        if (this.tank.getFluidAmount() > 0) {
            nbt.setTag("Tank", this.tank.writeToNBT(new NBTTagCompound()));
        }

        if (!this.draining.isEmpty()) {
            nbt.setTag("Draining", this.draining.writeToNBT(new NBTTagCompound()));
        }

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        if (nbt.hasKey("Tank", 10)) {
            this.tank.readFromNBT(nbt.getCompoundTag("Tank"));
        } else this.tank.setFluid(null);

        if (nbt.hasKey("Draining", 10)) {
            this.draining = new ItemStack(nbt.getCompoundTag("Draining"));
        } else this.draining = ItemStack.EMPTY;
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

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();

        if (this.tank.getFluidAmount() > 0) {
            nbt.setTag("Tank", this.tank.writeToNBT(new NBTTagCompound()));
        }

        if (!this.draining.isEmpty()) {
            nbt.setTag("Draining", this.draining.writeToNBT(new NBTTagCompound()));
        }

        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        if (nbt.hasKey("Tank", 10)) {
            this.tank.readFromNBT(nbt.getCompoundTag("Tank"));
        } else this.tank.setFluid(null);

        if (nbt.hasKey("Draining", 10)) {
            this.draining = new ItemStack(nbt.getCompoundTag("Draining"));
        } else this.draining = ItemStack.EMPTY;
    }

    @Override
    public ItemStack tryInsertItem(ItemStack stack) {
        if (this.draining.isEmpty()) {
            this.rollingDirection = null;
            this.draining = stack;
            this.sync();
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public boolean isInsertionSlotEmpty(ItemStack stack) {
        return this.draining.isEmpty();
    }

    @Override
    public ItemStack tryInsertItem(ItemStack stack, @Nullable EnumFacing side) {
        if (this.draining.isEmpty()) {
            this.rollingDirection = side != null ? side.getOpposite() : null;
            this.draining = stack;
            this.sync();
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return (T)this.tank;
        return super.getCapability(capability, facing);
    }
}
