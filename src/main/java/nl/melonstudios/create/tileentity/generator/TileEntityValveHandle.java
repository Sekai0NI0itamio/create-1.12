package nl.melonstudios.create.tileentity.generator;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;

/**
 * Valve handle tile: a hand crank with a dialled turn angle.
 *
 * <p>Translated from the reference ValveHandleBlockEntity (never pasted):
 * each click spins the handle toward a target angle (default 45° dial,
 * snapping to the next 90°/180° stop), then the crank idles again after a
 * short cooldown. The reference scroll-value dialogue has no backport UI
 * yet, so the dialled angle persists as plain NBT (wrench-scroll wiring is
 * NEEDS-LEAD); the motion values below mirror the reference activation
 * math in shape: target magnitude from the dial, overshoot run length plus
 * two settling ticks, four-tick cooldown.</p>
 */
public class TileEntityValveHandle extends TileEntityHandCrank {
    /** Ticks before the handle may be cranked again. */
    public int cooldown;
    /** Dialled turn angle in degrees, -180..180 (default 45). */
    public int angleInput = 45;
    public int startAngle;
    public int targetAngle;
    public int totalUseTicks;

    public TileEntityValveHandle() {
        super();
    }

    /**
     * Reference {@code activate}: refuse while the shaft already turns or
     * the handle is busy/cooling; otherwise run the crank long enough to
     * cover the dialled angle and settle.
     */
    public boolean activate(boolean sneak) {
        if (this.getGeneratedSpeed() != 0) return false;
        if (this.inUse > 0 || this.cooldown > 0) return false;
        if (this.world != null && this.world.isRemote) return true;

        int target = Math.abs(MathHelper.clamp(this.angleInput, -180, 180));
        // 32 RPM ≈ 192°/s ≈ 9.6°/tick; run length covers the target plus
        // the reference two settling ticks.
        this.inUse = Math.max(10, (int) Math.ceil(target / 9.6D) + 2);
        this.startAngle = ((int) (this.independentAngle % 90) + 360) % 90;
        int step = (target > 135 ? 180 : 90) * (int) Math.signum(this.angleInput == 0 ? 1 : this.angleInput);
        this.targetAngle = Math.round((this.startAngle + step) / 90.0F) * 90;
        this.totalUseTicks = this.inUse;
        this.backwards = sneak;
        this.cooldown = 4;
        if (this.world != null && !this.world.isRemote) {
            this.updateGeneratedRotation();
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.world != null && !this.world.isRemote) {
            if (this.inUse == 0 && this.cooldown > 0) this.cooldown--;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("TotalUseTicks", this.totalUseTicks);
        compound.setInteger("StartAngle", this.startAngle);
        compound.setInteger("TargetAngle", this.targetAngle);
        compound.setInteger("AngleInput", this.angleInput);
        compound.setInteger("Cooldown", this.cooldown);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.totalUseTicks = compound.getInteger("TotalUseTicks");
        this.startAngle = compound.getInteger("StartAngle");
        this.targetAngle = compound.getInteger("TargetAngle");
        if (compound.hasKey("AngleInput", 3)) {
            this.angleInput = MathHelper.clamp(compound.getInteger("AngleInput"), -180, 180);
        }
        this.cooldown = compound.getInteger("Cooldown");
    }
}
