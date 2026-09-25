package nl.melonstudios.create.tileentity.redstone;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import nl.melonstudios.create.block.redstone.BlockSmartObserver;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.Utils;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Polls the faced inventory or tank and pulses the block for 6 ticks per
 * detection, sustained while stock remains. The reference filter slot and
 * belt/pipe behaviours have no backport equivalent, so every stored stack or
 * fluid unit counts.
 */
public class TileEntitySmartObserver extends TileEntityOptimizedBase {
    private static final int PULSE_TICKS = 6;

    private int turnOffTicks = 0;
    private String lastSignature = "";
    private boolean sustainSignal = false;

    public TileEntitySmartObserver() {
        this.setTickRateLazy(20);
    }

    public void observe() {
        if (this.world == null || this.world.isRemote) return;
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockSmartObserver)) return;
        EnumFacing facing = state.getValue(BlockSmartObserver.FACING);
        if (!this.world.isBlockLoaded(this.pos.offset(facing))) return;

        TileEntity target = this.world.getTileEntity(this.pos.offset(facing));
        String signature = "";
        boolean stocked = false;
        if (target != null) {
            if (target.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite())) {
                IItemHandler inv = target.getCapability(
                        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
                if (inv != null) {
                    StringBuilder sb = new StringBuilder("i");
                    for (int i = 0; i < inv.getSlots(); i++) {
                        net.minecraft.item.ItemStack stack = inv.getStackInSlot(i);
                        sb.append('#').append(stack.isEmpty() ? 0 : (Item.getIdFromItem(stack.getItem())
                                + ":" + stack.getMetadata() + "x" + stack.getCount()));
                        if (!stack.isEmpty()) stocked = true;
                    }
                    signature = sb.toString();
                }
            }
            if (target.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite())) {
                IFluidHandler tank = target.getCapability(
                        CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite());
                if (tank != null) {
                    StringBuilder sb = new StringBuilder(signature + "|f");
                    net.minecraftforge.fluids.capability.IFluidTankProperties[] tanks = tank.getTankProperties();
                    for (int i = 0; i < tanks.length; i++) {
                        net.minecraftforge.fluids.FluidStack fluid = tanks[i].getContents();
                        sb.append('#').append(fluid == null ? 0 : (fluid.amount + "@"
                                + fluid.getFluid().getName()));
                        if (fluid != null && fluid.amount > 0) stocked = true;
                    }
                    signature = sb.toString();
                }
            }
        }

        if (!signature.equals(this.lastSignature)) {
            this.lastSignature = signature;
            this.sustainSignal = stocked;
            if (stocked) this.activate(PULSE_TICKS);
        } else if (stocked && this.sustainSignal) {
            this.turnOffTicks = PULSE_TICKS;
        }
    }

    public void activate(int ticks) {
        if (this.world == null || this.world.isRemote) return;
        this.turnOffTicks = ticks;
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockSmartObserver)) return;
        if (!state.getValue(BlockSmartObserver.POWERED)) {
            Utils.setBlockTESafe(this.world, this.pos,
                    state.withProperty(BlockSmartObserver.POWERED, true), 2);
            this.world.notifyNeighborsOfStateChange(this.pos, state.getBlock(), false);
        }
        this.sync();
    }

    @Override
    public void tick() {
        if (this.world != null && !this.world.isRemote && this.turnOffTicks > 0) {
            this.turnOffTicks--;
            if (this.turnOffTicks == 0) {
                IBlockState state = this.world.getBlockState(this.pos);
                if (state.getBlock() instanceof BlockSmartObserver
                        && !this.world.isBlockTickPending(this.pos, state.getBlock())) {
                    this.world.scheduleUpdate(this.pos, state.getBlock(), 1);
                }
            }
        }
    }

    @Override
    public void tickLazy() {
        this.observe();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("TurnOff", this.turnOffTicks);
        nbt.setString("Signature", this.lastSignature);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.turnOffTicks = Math.max(0, nbt.getInteger("TurnOff"));
        this.lastSignature = nbt.getString("Signature");
    }

    @Override
    public NBTTagCompound writePacket() {
        return new NBTTagCompound();
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        io.netty.buffer.ByteBuf temp = io.netty.buffer.Unpooled.buffer();
        net.minecraftforge.fml.common.network.ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(net.minecraftforge.fml.common.network.ByteBufUtils.readTag(buf));
    }
}
