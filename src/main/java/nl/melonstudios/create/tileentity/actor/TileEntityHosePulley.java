package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import nl.melonstudios.create.tileentity.TileEntityKinetic;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Hose pulley: hose extends 1 block per 16 ticks of runtime down to 32.
 * Draining (positive speed): consumes the fluid block at the tip (water /
 * lava source) into the 4000 mB tank. Ocean/river water (adjacent water at
 * tip) is infinite. Placing (negative speed): deposits the stored fluid as
 * blocks at the tip if replaceable.
 */
public class TileEntityHosePulley extends TileEntityKinetic {
    public float hoseLength;
    public final FluidTank tank = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() {
            TileEntityHosePulley.this.sync();
        }
    };

    private int tickCounter;

    @Override
    public void tick() {
        super.tick();
        float speed = this.getSpeed();
        if (speed == 0) return;
        this.tickCounter++;
        if (this.tickCounter % 16 != 0) return;

        if (speed > 0) {
            // Extend hose.
            if (this.hoseLength < 32) {
                BlockPos tip = this.pos.down((int) this.hoseLength + 1);
                if (!this.world.isBlockLoaded(tip)) return;
                IBlockState s = this.world.getBlockState(tip);
                if (s.getMaterial().isReplaceable() || s.getMaterial().isLiquid()) {
                    this.hoseLength += 1;
                    this.sync();
                }
            }
            // Drain at tip.
            BlockPos tip = this.pos.down(Math.max(1, (int) this.hoseLength));
            if (!this.world.isBlockLoaded(tip)) return;
            IBlockState s = this.world.getBlockState(tip);
            Fluid fluid = fluidOf(s);
            if (fluid != null && this.tank.getFluidAmount() < this.tank.getCapacity()) {
                boolean infinite = isInfiniteSource(tip, fluid);
                FluidStack stack = new FluidStack(fluid, 1000);
                int filled = this.tank.fillInternal(stack, false);
                if (filled >= 1000) {
                    this.tank.fillInternal(stack, true);
                    if (!infinite) {
                        this.world.setBlockToAir(tip);
                    }
                    this.sync();
                }
            }
        } else {
            // Retract hose + place fluid.
            FluidStack stored = this.tank.getFluid();
            if (stored != null && stored.amount >= 1000 && this.hoseLength >= 1) {
                BlockPos tip = this.pos.down(Math.max(1, (int) this.hoseLength));
                if (this.world.isBlockLoaded(tip)) {
                    IBlockState s = this.world.getBlockState(tip);
                    if (s.getMaterial().isReplaceable() && !s.getMaterial().isLiquid()) {
                        net.minecraft.block.Block fluidBlock = stored.getFluid().getBlock();
                        if (fluidBlock != null && fluidBlock != Blocks.AIR) {
                            this.world.setBlockState(tip, fluidBlock.getDefaultState());
                            this.tank.drainInternal(1000, true);
                        }
                    }
                }
            }
            if (this.hoseLength > 0) {
                this.hoseLength -= 1;
                this.sync();
            }
        }
        this.markDirty();
    }

    private static Fluid fluidOf(IBlockState s) {
        if (s.getMaterial() == Material.WATER) return FluidRegistry.WATER;
        if (s.getMaterial() == Material.LAVA) return FluidRegistry.LAVA;
        return null;
    }

    private boolean isInfiniteSource(BlockPos tip, Fluid fluid) {
        if (fluid != FluidRegistry.WATER) return false;
        // 2+ adjacent water sources = infinite (ocean/river rule).
        int adj = 0;
        for (EnumFacing f : EnumFacing.HORIZONTALS) {
            IBlockState s = this.world.getBlockState(tip.offset(f));
            if (s.getMaterial() == Material.WATER) adj++;
        }
        return adj >= 2;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setFloat("hose", this.hoseLength);
        if (this.tank.getFluidAmount() > 0) nbt.setTag("Tank", this.tank.writeToNBT(new NBTTagCompound()));
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.hoseLength = nbt.getFloat("hose");
        if (nbt.hasKey("Tank", 10)) this.tank.readFromNBT(nbt.getCompoundTag("Tank"));
        else this.tank.setFluid(null);
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeFloat(this.hoseLength);
        buf.writeBoolean(this.tank.getFluidAmount() > 0);
        if (this.tank.getFluidAmount() > 0) {
            FluidStack f = this.tank.getFluid();
            buf.writeUTF(FluidRegistry.getFluidName(f));
            buf.writeInt(f.amount);
        }
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        this.hoseLength = buf.readFloat();
        if (buf.readBoolean()) {
            Fluid fluid = FluidRegistry.getFluid(buf.readUTF());
            int amount = buf.readInt();
            this.tank.setFluid(fluid == null ? null : new FluidStack(fluid, amount));
        } else {
            this.tank.setFluid(null);
        }
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 4);
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
