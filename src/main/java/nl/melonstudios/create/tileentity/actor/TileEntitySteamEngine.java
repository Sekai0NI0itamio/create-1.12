package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import nl.melonstudios.create.block.BlockFluidTank;
import nl.melonstudios.create.tileentity.TileEntityFluidTank;
import nl.melonstudios.create.tileentity.TileEntityKineticGeneratorBase;

import java.util.HashSet;
import java.util.Set;

/**
 * Steam engine generator: finds an adjacent boiler tank, outputs 16 RPM at
 * 16 SU/RPM * efficiency (splitting heat across attached engines, official
 * formula). No boiler = no power.
 */
public class TileEntitySteamEngine extends TileEntityKineticGeneratorBase {
    @Override
    public float getGeneratedSpeed() {
        TileEntityFluidTank boiler = this.findBoiler();
        if (boiler == null || !boiler.isBoilerActive()) return 0.0F;
        return 16.0F;
    }

    @Override
    public float calculateCapacity() {
        TileEntityFluidTank boiler = this.findBoiler();
        if (boiler == null || !boiler.isBoilerActive()) return 0.0F;
        int engines = this.countEngines(boiler);
        int level = boiler.getBoilerLevel();
        float eff = engines <= 0 ? 1.0F : (engines <= level ? 1.0F : (float) level / engines);
        return 16.0F * eff;
    }

    @Override
    public float calculateImpact() {
        return 0.0F;
    }

    private TileEntityFluidTank findBoiler() {
        if (this.world == null) return null;
        for (EnumFacing f : EnumFacing.VALUES) {
            BlockPos p = this.pos.offset(f);
            if (!this.world.isBlockLoaded(p)) continue;
            TileEntity te = this.world.getTileEntity(p);
            if (te instanceof TileEntityFluidTank) {
                TileEntityFluidTank tank = ((TileEntityFluidTank) te).bottom();
                if (tank.isBoilerActive()) return tank;
            }
        }
        return null;
    }

    private int countEngines(TileEntityFluidTank boiler) {
        // Official BoilerData.evaluate counts only engines touching this tank;
        // distant or foreign-boiler engines must not dilute the split.
        Set<BlockPos> found = new HashSet<>();
        TileEntityFluidTank bottom = boiler.bottom();
        int h = Math.max(1, bottom.height());
        for (int i = 0; i < h; i++) {
            BlockPos p = bottom.getPos().up(i);
            if (!this.world.isBlockLoaded(p)) continue;
            if (!(this.world.getBlockState(p).getBlock() instanceof BlockFluidTank)) break;
            for (EnumFacing f : EnumFacing.VALUES) {
                BlockPos q = p.offset(f);
                if (!this.world.isBlockLoaded(q)) continue;
                if (this.world.getTileEntity(q) instanceof TileEntitySteamEngine) found.add(q);
            }
        }
        return Math.max(1, found.size());
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
