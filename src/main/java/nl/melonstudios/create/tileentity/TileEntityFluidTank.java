package nl.melonstudios.create.tileentity;

import com.melonstudios.melonlib.misc.AABB;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import nl.melonstudios.create.block.BlockFluidTank;

import javax.annotation.Nullable;

/**
 * Fluid tank TE. Tanks stack: the bottom-most tank block in a vertical column
 * owns capacity = 1000 * height. Non-bottom tanks delegate to the bottom.
 */
public class TileEntityFluidTank extends TileEntityOptimizedBase {
    private final FluidTank tank = new FluidTank(1000) {
        @Override
        protected void onContentsChanged() {
            TileEntityFluidTank.this.markDirty();
            TileEntityFluidTank.this.sync();
        }
    };

    @Override
    public void tick() {
    }

    @Override
    public void tickLazy() {
    }

    public boolean isBottom() {
        if (this.world == null) return true;
        return !(this.world.getBlockState(this.pos.down()).getBlock() instanceof BlockFluidTank);
    }

    public TileEntityFluidTank bottom() {
        if (this.isBottom()) return this;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(this.pos);
        for (int i = 0; i < 32; i++) {
            p.move(EnumFacing.DOWN);
            if (!(this.world.getBlockState(p).getBlock() instanceof BlockFluidTank)) break;
            if (this.world.getTileEntity(p) instanceof TileEntityFluidTank) {
                TileEntityFluidTank te = (TileEntityFluidTank) this.world.getTileEntity(p);
                if (te.isBottom()) return te;
            }
        }
        return this;
    }

    public int height() {
        int h = 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(this.pos);
        for (int i = 0; i < 32; i++) {
            p.move(EnumFacing.UP);
            if (!(this.world.getBlockState(p).getBlock() instanceof BlockFluidTank)) break;
            h++;
        }
        return h;
    }

    public FluidTank effectiveTank() {
        TileEntityFluidTank b = this.bottom();
        b.tank.setCapacity(1000 * Math.max(1, b.height()));
        return b.tank;
    }

    public FluidStack getFluid() {
        return this.bottom().tank.getFluid();
    }

    public int getCapacity() {
        TileEntityFluidTank b = this.bottom();
        return 1000 * Math.max(1, b.height());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (this.isBottom()) {
            NBTTagCompound t = new NBTTagCompound();
            this.tank.writeToNBT(t);
            nbt.setTag("Tank", t);
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("Tank", 10)) {
            this.tank.readFromNBT(nbt.getCompoundTag("Tank"));
        }
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        FluidStack f = this.getFluid();
        if (f != null) nbt.setTag("Fluid", f.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("Capacity", this.getCapacity());
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        if (nbt.hasKey("Fluid", 10)) {
            this.tank.setFluid(FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("Fluid")));
        } else {
            this.tank.setFluid(null);
        }
        this.tank.setCapacity(nbt.getInteger("Capacity"));
    }

    @Override
    public void writePacket(com.melonstudios.melonlib.network.TrackedByteBuf buf) throws java.io.IOException {
        io.netty.buffer.ByteBuf temp = Unpooled.buffer();
        ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @Override
    public void readPacket(io.netty.buffer.ByteBuf buf) throws java.io.IOException {
        this.readPacket(ByteBufUtils.readTag(buf));
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) this.effectiveTank();
        }
        return super.getCapability(capability, facing);
    }
}
