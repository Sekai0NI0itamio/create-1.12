package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import nl.melonstudios.create.tileentity.TileEntityFluidTank;
import nl.melonstudios.create.tileentity.TileEntityKineticGeneratorBase;

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
        int n = 0;
        BlockPos bp = boiler.getPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos p = bp.add(dx, dy, dz);
                    if (!this.world.isBlockLoaded(p)) continue;
                    if (this.world.getTileEntity(p) instanceof TileEntitySteamEngine) n++;
                }
            }
        }
        return Math.max(1, n);
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
