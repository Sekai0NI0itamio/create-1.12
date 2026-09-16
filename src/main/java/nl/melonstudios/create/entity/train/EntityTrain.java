package nl.melonstudios.create.entity.train;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Train: a coupled line of minecart-based carriages running a rail loop.
 * Schedule = ordered station stops with dwell ticks; loops forever.
 * Carriage visuals: the assembled frame blocks ride as passengers' context
 * (backport simplification: frames stored as block list, re-placed on
 * disassemble; carts carry players/chests visually).
 */
public class EntityTrain extends EntityMinecartEmpty {
    public UUID trainId = UUID.randomUUID();
    public List<BlockPos> schedule = new ArrayList<>();
    public int stopIndex;
    public int dwellTicks;
    public boolean running;
    public double cruiseSpeed = 0.3;

    public EntityTrain(World world) {
        super(world);
    }

    public void addStop(BlockPos station) {
        this.schedule.add(station.toImmutable());
    }

    public void start() {
        if (!this.schedule.isEmpty()) {
            this.running = true;
            this.stopIndex = 0;
            this.dwellTicks = 100;
        }
    }

    public void halt() {
        this.running = false;
        this.setMotion(0, this.motionY, 0);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote || !this.running || this.schedule.isEmpty()) return;
        BlockPos target = this.schedule.get(this.stopIndex % this.schedule.size());
        double dx = target.getX() + 0.5 - this.posX;
        double dz = target.getZ() + 0.5 - this.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 2.0) {
            // Dwell at station.
            this.setMotion(this.motionX * 0.8, this.motionY, this.motionZ * 0.8);
            if (--this.dwellTicks <= 0) {
                this.stopIndex++;
                this.dwellTicks = 100;
            }
            return;
        }
        // Drive along motion clamped to cruise speed (rails guide direction).
        double mx = this.motionX;
        double mz = this.motionZ;
        double sp = Math.sqrt(mx * mx + mz * mz);
        if (sp < 0.01) {
            // Nudge toward the target.
            this.setMotion(dx / dist * 0.05, this.motionY, dz / dist * 0.05);
        } else if (sp > this.cruiseSpeed) {
            this.setMotion(mx / sp * this.cruiseSpeed, this.motionY, mz / sp * this.cruiseSpeed);
        } else {
            this.setMotion(mx / sp * Math.min(this.cruiseSpeed, sp + 0.01), this.motionY, mz / sp * Math.min(this.cruiseSpeed, sp + 0.01));
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setUniqueId("TrainId", this.trainId);
        nbt.setInteger("StopIndex", this.stopIndex);
        nbt.setBoolean("Running", this.running);
        nbt.setDouble("Cruise", this.cruiseSpeed);
        nbt.setInteger("Stops", this.schedule.size());
        for (int i = 0; i < this.schedule.size(); i++) {
            nbt.setLong("Stop" + i, this.schedule.get(i).toLong());
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        if (nbt.hasUniqueId("TrainId")) this.trainId = nbt.getUniqueId("TrainId");
        this.stopIndex = nbt.getInteger("StopIndex");
        this.running = nbt.getBoolean("Running");
        if (nbt.hasKey("Cruise")) this.cruiseSpeed = nbt.getDouble("Cruise");
        this.schedule.clear();
        int n = nbt.getInteger("Stops");
        for (int i = 0; i < n; i++) {
            this.schedule.add(BlockPos.fromLong(nbt.getLong("Stop" + i)));
        }
    }

    @Override
    public EntityMinecart.Type getType() {
        return EntityMinecart.Type.RIDEABLE;
    }
}
