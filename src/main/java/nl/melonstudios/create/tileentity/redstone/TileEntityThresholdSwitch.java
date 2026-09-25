package nl.melonstudios.create.tileentity.redstone;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import nl.melonstudios.create.block.redstone.BlockThresholdSwitch;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.Utils;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Sums the faced inventory or tank against a hysteresis pair (off-below /
 * on-above, defaults 64 / 128 from the reference). Power applies after a
 * 2-tick block delay, translated from the reference scheduled tick.
 */
public class TileEntityThresholdSwitch extends TileEntityOptimizedBase {
    private static final int[][] PRESETS = {{64, 128}, {512, 1024}, {2048, 4096}};

    private int preset = 0;
    private int offBelow = PRESETS[0][0];
    private int onAbove = PRESETS[0][1];
    private boolean inverted = false;

    private int currentLevel = -1;
    private int currentMax = -1;
    private boolean redstoneState = false;
    private boolean poweredAfterDelay = false;

    public TileEntityThresholdSwitch() {
        this.setTickRateLazy(10);
    }

    public int getOnAbove() {
        return this.onAbove;
    }

    public int getOffBelow() {
        return this.offBelow;
    }

    public boolean isInverted() {
        return this.inverted;
    }

    public boolean isPowered() {
        return this.poweredAfterDelay;
    }

    public boolean getState() {
        return this.redstoneState;
    }

    public void cyclePreset() {
        this.preset = (this.preset + 1) % PRESETS.length;
        this.offBelow = PRESETS[this.preset][0];
        this.onAbove = PRESETS[this.preset][1];
        this.markDirty();
    }

    public void setInverted(boolean inverted) {
        if (inverted == this.inverted) return;
        this.inverted = inverted;
        this.updatePowerAfterDelay();
        this.markDirty();
    }

    public void updateCurrentLevel() {
        if (this.world == null || this.world.isRemote) return;
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockThresholdSwitch)) return;
        EnumFacing facing = state.getValue(BlockThresholdSwitch.FACING);
        if (!this.world.isBlockLoaded(this.pos.offset(facing))) return;

        TileEntity target = this.world.getTileEntity(this.pos.offset(facing));
        int prevLevel = this.currentLevel;
        int prevMax = this.currentMax;
        boolean prevPowered = this.redstoneState;

        int level = 0;
        int max = 0;
        boolean found = false;
        if (target != null) {
            if (target.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite())) {
                IItemHandler inv = target.getCapability(
                        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
                if (inv != null) {
                    found = true;
                    for (int i = 0; i < inv.getSlots(); i++) {
                        ItemStack stack = inv.getStackInSlot(i);
                        int space = Math.min(stack.getMaxStackSize(), inv.getSlotLimit(i));
                        if (space == 0) continue;
                        max += space;
                        level += stack.getCount();
                    }
                }
            }
            if (target.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite())) {
                IFluidHandler tank = target.getCapability(
                        CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite());
                if (tank != null) {
                    found = true;
                    for (int i = 0; i < tank.getTanks(); i++) {
                        FluidStack fluid = tank.getFluidInTank(i);
                        int space = tank.getTankCapacity(i);
                        if (space == 0) continue;
                        max += space;
                        if (fluid != null) level += fluid.amount;
                    }
                }
            }
        }

        if (!found) {
            if (this.currentLevel == -1) return;
            this.currentLevel = -1;
            this.currentMax = -1;
            this.redstoneState = false;
            Utils.setBlockTESafe(this.world, this.pos, state.withProperty(BlockThresholdSwitch.POWERED, false), 3);
            this.scheduleBlockTick();
            this.sync();
            this.markDirty();
            return;
        }

        if (max < 0) max = 0;
        if (level < 0) level = 0;
        if (level > max) level = max;
        this.currentLevel = level;
        this.currentMax = max;

        if (this.redstoneState && level <= this.offBelow) this.redstoneState = false;
        else if (!this.redstoneState && level >= this.onAbove) this.redstoneState = true;

        if (prevPowered != this.redstoneState) this.scheduleBlockTick();
        if (prevLevel != this.currentLevel || prevMax != this.currentMax
                || prevPowered != this.redstoneState) {
            this.sync();
            this.markDirty();
        }
    }

    private void scheduleBlockTick() {
        if (this.world == null) return;
        IBlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() instanceof BlockThresholdSwitch
                && !this.world.isBlockTickPending(this.pos, state.getBlock())) {
            this.world.scheduleUpdate(this.pos, state.getBlock(), 2);
        }
    }

    public void updatePowerAfterDelay() {
        if (this.world == null || this.world.isRemote) return;
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockThresholdSwitch)) return;
        this.poweredAfterDelay = this.inverted != this.redstoneState;
        Utils.setBlockTESafe(this.world, this.pos,
                state.withProperty(BlockThresholdSwitch.POWERED, this.poweredAfterDelay), 2);
        this.world.notifyNeighborsOfStateChange(this.pos, state.getBlock(), false);
        this.sync();
        this.markDirty();
    }

    @Override
    public void tick() {
    }

    @Override
    public void tickLazy() {
        this.updateCurrentLevel();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("Preset", this.preset);
        nbt.setInteger("OnAbove", this.onAbove);
        nbt.setInteger("OffBelow", this.offBelow);
        nbt.setInteger("Current", this.currentLevel);
        nbt.setInteger("CurrentMax", this.currentMax);
        nbt.setBoolean("Powered", this.redstoneState);
        nbt.setBoolean("Inverted", this.inverted);
        nbt.setBoolean("Delayed", this.poweredAfterDelay);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.preset = nbt.getInteger("Preset") % PRESETS.length;
        this.onAbove = nbt.getInteger("OnAbove");
        this.offBelow = nbt.getInteger("OffBelow");
        if (this.onAbove <= 0) this.onAbove = PRESETS[this.preset][1];
        if (this.offBelow < 0) this.offBelow = PRESETS[this.preset][0];
        this.currentLevel = nbt.getInteger("Current");
        this.currentMax = nbt.getInteger("CurrentMax");
        this.redstoneState = nbt.getBoolean("Powered");
        this.inverted = nbt.getBoolean("Inverted");
        this.poweredAfterDelay = nbt.getBoolean("Delayed");
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("Current", this.currentLevel);
        nbt.setInteger("CurrentMax", this.currentMax);
        nbt.setBoolean("Delayed", this.poweredAfterDelay);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.currentLevel = nbt.getInteger("Current");
        this.currentMax = nbt.getInteger("CurrentMax");
        this.poweredAfterDelay = nbt.getBoolean("Delayed");
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
