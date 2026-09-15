package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import nl.melonstudios.create.block.actor.BlockPortableFluidInterface;
import nl.melonstudios.create.tileentity.TileEntityKinetic;

import javax.annotation.Nullable;

/**
 * Portable fluid interface TE: looks 1-2 blocks in facing direction for a
 * matching interface on a contraption; if found, forwards all fluid calls to
 * the contraption-side tank (or its own 1000 mB buffer when docked empty).
 */
public class TileEntityPortableFluidInterface extends TileEntityKinetic {
    public boolean connected;

    public EnumFacing facing() {
        return EnumFacing.NORTH;
    }

    @Nullable
    public IFluidHandler target() {
        if (this.world == null) return null;
        for (int d = 1; d <= 2; d++) {
            for (EnumFacing f : EnumFacing.VALUES) {
                BlockPos p = this.pos.offset(f, d);
                if (!this.world.isBlockLoaded(p)) continue;
                if (!(this.world.getBlockState(p).getBlock() instanceof BlockPortableFluidInterface)) continue;
                TileEntity te = this.world.getTileEntity(p);
                if (te != null && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, f.getOpposite())) {
                    this.connected = true;
                    return te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, f.getOpposite());
                }
            }
        }
        this.connected = false;
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        this.target();
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

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            IFluidHandler t = this.target();
            if (t != null) return (T) t;
            return (T) new net.minecraftforge.fluids.FluidTank(1000);
        }
        return super.getCapability(capability, facing);
    }
}
