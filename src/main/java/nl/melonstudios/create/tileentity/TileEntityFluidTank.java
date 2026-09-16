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
        this.sampleBoiler();
    }

    @Override
    public void tickLazy() {
    }

    // ---- Steam boiler (official rules) ----
    // A tank column adjacent to heat (burner/fire/lava/magma below or beside)
    // with water inside is a boiler. Level = min(heat, size/4, water/10).
    public int boilerHeat;
    public int boilerSize;
    public int boilerWater;
    private int sampleCooldown;

    public void sampleBoiler() {
        if (this.world == null || this.world.isRemote) return;
        if (!this.isBottom()) return;
        if (--this.sampleCooldown > 0) return;
        this.sampleCooldown = 20;
        int size = this.height();
        int heat = 0;
        for (int y = 0; y < size; y++) {
            net.minecraft.util.math.BlockPos p = this.pos.up(y);
            for (net.minecraft.util.EnumFacing f : new net.minecraft.util.EnumFacing[]{
                    net.minecraft.util.EnumFacing.DOWN, net.minecraft.util.EnumFacing.NORTH,
                    net.minecraft.util.EnumFacing.SOUTH, net.minecraft.util.EnumFacing.WEST,
                    net.minecraft.util.EnumFacing.EAST}) {
                net.minecraft.block.state.IBlockState s = this.world.getBlockState(p.offset(f));
                if (s.getBlock() instanceof nl.melonstudios.create.util.interfaces.IHeatProvider) {
                    heat = Math.max(heat, ((nl.melonstudios.create.util.interfaces.IHeatProvider) s.getBlock())
                            .getHeat(this.world, p.offset(f), s));
                } else if (s.getBlock() == net.minecraft.init.Blocks.FIRE
                        || s.getBlock() == net.minecraft.init.Blocks.LAVA
                        || s.getBlock() == net.minecraft.init.Blocks.FLOWING_LAVA
                        || s.getBlock() == net.minecraft.init.Blocks.MAGMA) {
                    heat = Math.max(heat, 1);
                }
            }
        }
        net.minecraftforge.fluids.FluidStack f = this.tank.getFluid();
        int water = 0;
        if (f != null && f.getFluid() == net.minecraftforge.fluids.FluidRegistry.WATER) {
            water = f.amount / 10;
        }
        int sizeHeat = Math.min(18, size / 4);
        int waterHeat = Math.min(18, water / 10);
        this.boilerHeat = heat;
        this.boilerSize = size;
        this.boilerWater = water;
        this.boilerLevel = Math.min(heat, Math.min(sizeHeat, waterHeat));
        this.boilerActive = heat > 0 && sizeHeat > 0 && waterHeat > 0;
    }

    public int boilerLevel;
    public boolean boilerActive;

    public int getBoilerLevel() {
        return this.bottom().boilerLevel;
    }

    public boolean isBoilerActive() {
        return this.bottom().boilerActive;
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
