package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.tileentity.marker.IDepot;
import nl.melonstudios.create.tileentity.marker.IHaltBeltContents;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

/**
 * Spout: fills fluid containers. Internal 1000 mB tank (refilled from pipes
 * via capability, or right-click with a fluid container). When a fillable
 * item sits on the depot/belt two below (or as EntityItem), and speed != 0,
 * it fills over ~20 ticks then releases the filled container.
 */
public class TileEntitySpout extends TileEntityKinetic implements IHaltBeltContents {
    public final FluidTank tank = new FluidTank(1000) {
        @Override
        protected void onContentsChanged() {
            TileEntitySpout.this.sync();
        }
    };

    public int fillTimer;
    private ItemStack filling = ItemStack.EMPTY;

    @Override
    public void tick() {
        super.tick();
        if (this.getSpeed() == 0) return;
        if (this.tank.getFluidAmount() <= 0) {
            this.fillTimer = 0;
            return;
        }

        if (!this.filling.isEmpty()) {
            this.fillTimer -= Math.abs(this.getSpeed() / 8.0F) + 1;
            this.markDirty();
            FluidStack pouring = this.tank.getFluid();
            if (pouring != null && pouring.getFluid() != null) {
                CreateLegacy.proxy.spoutFX(this.world,
                        this.pos.getX() + 0.5D, this.pos.getY() - 0.2D, this.pos.getZ() + 0.5D,
                        pouring.getFluid().getColor(pouring));
            }
            if (this.fillTimer <= 0) {
                this.finishFill();
            }
            return;
        }

        // Depot two below (official spout sits above the target).
        IDepot depot = IDepot.get(this.world, this.pos.down(2));
        if (depot != null) {
            ItemStack presented = depot.getPresentedItem();
            if (!presented.isEmpty() && this.canFill(presented)) {
                this.filling = presented.copy();
                this.filling.setCount(1);
                depot.decreasePresentedAndAddOutput(ItemStack.EMPTY);
                this.fillTimer = 20;
                this.sync();
                return;
            }
        }

        // Loose EntityItems two below.
        List<EntityItem> items = this.world.getEntitiesWithinAABB(EntityItem.class,
                new AxisAlignedBB(this.pos.down(2)), EntityItem::isEntityAlive);
        for (EntityItem ei : items) {
            ItemStack stack = ei.getItem();
            if (!stack.isEmpty() && this.canFill(stack)) {
                this.filling = stack.copy();
                this.filling.setCount(1);
                stack.shrink(1);
                if (stack.isEmpty()) ei.setDead();
                else ei.setItem(stack);
                this.fillTimer = 20;
                this.sync();
                return;
            }
        }
    }

    private boolean canFill(ItemStack stack) {
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(stack);
        if (handler == null) return false;
        FluidStack inTank = this.tank.getFluid();
        if (inTank == null) return false;
        int filled = handler.fill(inTank, false);
        return filled > 0;
    }

    private void finishFill() {
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(this.filling);
        if (handler == null) {
            this.filling = ItemStack.EMPTY;
            return;
        }
        FluidStack inTank = this.tank.getFluid();
        int filled = handler.fill(inTank, true);
        if (filled > 0) {
            this.tank.drainInternal(filled, true);
            FluidStack f = this.tank.getFluid();
            if (f != null) {
                this.world.playSound(null, this.pos, f.getFluid().getFillSound(f), SoundCategory.BLOCKS, 0.8F, 1.0F);
            }
        }
        ItemStack out = handler.getContainer();
        this.filling = ItemStack.EMPTY;
        if (!this.world.isRemote && !out.isEmpty()) {
            StackUtil.spawnItemNoVelocity(this.world, this.pos.getX() + 0.5, this.pos.getY() - 1.6, this.pos.getZ() + 0.5, out);
        }
        this.sync();
    }

    public boolean activate(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty()) return false;
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(held);
        if (handler == null) return false;
        // Pour held container into the tank.
        FluidStack drained = handler.drain(this.tank.getCapacity() - this.tank.getFluidAmount(), false);
        if (drained == null || drained.amount <= 0) return false;
        handler.drain(drained.amount, true);
        this.tank.fillInternal(drained, true);
        ItemStack container = handler.getContainer();
        player.setHeldItem(hand, container);
        this.sync();
        return true;
    }

    @Override
    public void destroy() {
        super.destroy();
        StackUtil.dropItemsAt(this.world, this.pos, this.filling);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (this.tank.getFluidAmount() > 0) nbt.setTag("Tank", this.tank.writeToNBT(new NBTTagCompound()));
        if (!this.filling.isEmpty()) nbt.setTag("Filling", this.filling.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("fillTimer", this.fillTimer);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("Tank", 10)) this.tank.readFromNBT(nbt.getCompoundTag("Tank"));
        else this.tank.setFluid(null);
        if (nbt.hasKey("Filling", 10)) this.filling = new ItemStack(nbt.getCompoundTag("Filling"));
        else this.filling = ItemStack.EMPTY;
        this.fillTimer = nbt.getInteger("fillTimer");
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        if (this.tank.getFluidAmount() > 0) {
            nbt.setTag("Tank", this.tank.writeToNBT(new NBTTagCompound()));
        }
        nbt.setInteger("fillTimer", this.fillTimer);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        if (nbt.hasKey("Tank", 10)) this.tank.readFromNBT(nbt.getCompoundTag("Tank"));
        else this.tank.setFluid(null);
        this.fillTimer = nbt.getInteger("fillTimer");
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

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }

    @Override
    public boolean shouldHaltItem(ItemStack stack) {
        return this.getSpeed() != 0 && this.tank.getFluidAmount() > 0 && this.canFill(stack);
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
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return (T) this.tank;
        return super.getCapability(capability, facing);
    }
}
