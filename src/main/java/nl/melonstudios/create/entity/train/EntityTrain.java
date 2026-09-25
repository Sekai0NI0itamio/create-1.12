package nl.melonstudios.create.entity.train;

import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
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
 *
 * Movement model (track following): vanilla minecart physics rides the rails
 * (curves, slopes and switches included); this entity only commands speed
 * along the rail tangent resolved by {@link TrainTrackFollower}. Each tick the
 * current velocity is projected onto the tangent so the cart never drifts
 * sideways off the line, then accelerated toward a target speed that tapers
 * near the scheduled station via a braking envelope. Following the reference
 * design, travel halts (blocked) at dead ends and off rails (derailed), and
 * trailing carriages sharing a train id match the leader's pace at a fixed
 * spacing instead of pathing independently.
 */
public class EntityTrain extends EntityMinecartEmpty {
    public UUID trainId = UUID.randomUUID();
    public List<BlockPos> schedule = new ArrayList<>();
    /** Per-stop dwell ticks, parallel to {@link #schedule}; empty means legacy data. */
    public List<Integer> scheduleDwell = new ArrayList<>();
    /** Per-stop wait condition ({@link TrainScheduleData#COND_NONE}/{@link TrainScheduleData#COND_TIMED}), parallel to {@link #schedule}. */
    public List<Integer> scheduleCondition = new ArrayList<>();
    public int stopIndex;
    public int dwellTicks;
    public boolean running;
    public double cruiseSpeed = 0.3;

    /** Signed speed along the current rail tangent (blocks/tick). */
    public double railSpeed;
    /** Total distance rolled; used to order carriages front-to-back. */
    public double odometer;
    /** True while off rails; speed target is forced to zero until re-railed. */
    public boolean derailed;

    private int offRailTicks;
    private int endStopTicks;

    private static final double ACCEL = 0.02;
    private static final double BRAKE = 0.06;
    private static final double CREEP = 0.04;
    private static final int DWELL_DEFAULT = 100;
    private static final double SPACING = 2.5;

    public EntityTrain(World world) {
        super(world);
    }

    public void addStop(BlockPos station) {
        this.schedule.add(station.toImmutable());
    }

    /**
     * Load an extended schedule (stops plus per-stop dwell and wait
     * condition, as written by the schedule item). Additive: replaces only
     * the schedule fields, never movement state.
     */
    public void applyScheduleData(List<BlockPos> stops, List<Integer> dwells, List<Integer> conds) {
        this.schedule.clear();
        this.schedule.addAll(TrainScheduleData.copyStops(stops));
        this.scheduleDwell.clear();
        this.scheduleCondition.clear();
        for (int i = 0; i < stops.size(); i++) {
            this.scheduleDwell.add(dwells != null && dwells.size() > i ? dwells.get(i) : TrainScheduleData.DWELL_DEFAULT);
            this.scheduleCondition.add(conds != null && conds.size() > i ? conds.get(i) : TrainScheduleData.COND_TIMED);
        }
        this.stopIndex = 0;
        this.dwellTicks = this.getDwellForStop(0);
    }

    /** Dwell waited at a stop; missing data falls back to the legacy default. */
    public int getDwellForStop(int index) {
        int dwell = (index >= 0 && index < this.scheduleDwell.size())
                ? this.scheduleDwell.get(index) : DWELL_DEFAULT;
        int cond = this.getConditionForStop(index);
        return TrainScheduleData.effectiveDwell(dwell, cond);
    }

    /** Wait condition at a stop; missing data means a legacy timed stop. */
    public int getConditionForStop(int index) {
        if (index >= 0 && index < this.scheduleCondition.size()) return this.scheduleCondition.get(index);
        return TrainScheduleData.COND_TIMED;
    }

    public void start() {
        if (!this.schedule.isEmpty()) {
            this.running = true;
            this.stopIndex = 0;
            this.dwellTicks = this.getDwellForStop(0);
            this.derailed = false;
            this.offRailTicks = 0;
            this.endStopTicks = 0;
        }
    }

    public void halt() {
        this.running = false;
        this.railSpeed = 0.0;
        this.motionX = 0;
        this.motionZ = 0;
    }

    public boolean isDerailed() {
        return this.derailed;
    }

    @Override
    public void onUpdate() {
        // Vanilla rail riding first: curves, slopes and switch guidance.
        super.onUpdate();

        double planar = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.odometer += planar;

        if (this.world.isRemote || !this.running || this.schedule.isEmpty()) {
            return;
        }

        BlockPos target = this.schedule.get(this.stopIndex % this.schedule.size());
        double tx = target.getX() + 0.5 - this.posX;
        double tz = target.getZ() + 0.5 - this.posZ;
        double dist = Math.sqrt(tx * tx + tz * tz);

        BlockRailBase.EnumRailDirection shape =
                TrainTrackFollower.railShapeAt(this.world, this.posX, this.posY, this.posZ);
        if (shape == null) {
            // Off rails: coast down and flag derailed until pushed back on.
            this.offRailTicks++;
            this.motionX *= 0.9;
            this.motionZ *= 0.9;
            if (this.offRailTicks > 10) {
                this.derailed = true;
                this.railSpeed = planar;
            }
            return;
        }
        this.offRailTicks = 0;
        this.derailed = false;

        // Reference vector: keep rolling the way we roll; only aim at the
        // station when standing still.
        double refX = planar > 0.02 ? this.motionX : tx;
        double refZ = planar > 0.02 ? this.motionZ : tz;
        double[] tangent = TrainTrackFollower.orientedTangent(shape, refX, refZ);
        double ux = tangent[0];
        double uz = tangent[1];

        // Station dwell: inside the arrival radius, hold position, count down,
        // then advance the schedule and depart.
        if (dist < TrainTrackFollower.ARRIVE_RADIUS) {
            this.motionX *= 0.7;
            this.motionZ *= 0.7;
            this.railSpeed = 0.0;
            this.endStopTicks = 0;
            if (--this.dwellTicks <= 0) {
                this.stopIndex++;
                this.dwellTicks = this.getDwellForStop(this.stopIndex % this.schedule.size());
            }
            return;
        }

        double allowed = TrainTrackFollower.approachAllowed(dist, this.cruiseSpeed, BRAKE, CREEP);

        // Dead end ahead: no rail where travel is heading, so stop (blocked)
        // instead of running off. After a pause, reverse out automatically.
        double aheadX = this.posX + ux * 1.5;
        double aheadZ = this.posZ + uz * 1.5;
        boolean ends = !TrainTrackFollower.hasRail(this.world, aheadX, this.posY, aheadZ)
                && !TrainTrackFollower.hasRail(this.world, aheadX, this.posY - 1.0, aheadZ);
        if (ends) {
            allowed = 0.0;
        }

        // Follow-the-leader: trailing carriages of the same train match the
        // leader's pace, closing or opening the gap toward fixed spacing.
        EntityTrain leader = this.findLeader();
        if (leader != null && leader != this) {
            double gap = this.getDistance(leader);
            double follow = leader.railSpeed;
            if (gap > SPACING + 1.0) {
                follow = Math.min(this.cruiseSpeed, follow + 0.06);
            } else if (gap < SPACING - 0.5) {
                follow = Math.max(0.0, follow - 0.08);
            }
            allowed = Math.min(allowed, follow);
        }

        // Speed control toward the target along the oriented tangent.
        double current = this.motionX * ux + this.motionZ * uz;
        if (current < 0) {
            current = 0;
        }
        if (current < allowed) {
            current = Math.min(allowed, current + ACCEL);
        } else {
            current = Math.max(allowed, current - BRAKE);
        }
        this.motionX = ux * current;
        this.motionZ = uz * current;
        this.railSpeed = current;

        if (planar > 0.01 || current > 0.01) {
            this.rotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0 / Math.PI);
        }

        if (ends && current < 0.02) {
            if (++this.endStopTicks > 60) {
                // Back out of the terminus so the schedule can be approached
                // from the live side of the line.
                this.motionX = -ux * 0.08;
                this.motionZ = -uz * 0.08;
                this.endStopTicks = 0;
            }
        } else if (!ends) {
            this.endStopTicks = 0;
        }
    }

    /** Front of the consist: the live sibling (same train id) with the greatest odometer. */
    private EntityTrain findLeader() {
        List<EntityTrain> kin = this.world.getEntitiesWithinAABB(EntityTrain.class,
                new AxisAlignedBB(this.posX - 24, this.posY - 6, this.posZ - 24,
                        this.posX + 24, this.posY + 6, this.posZ + 24),
                e -> e != null && e != this && e.isEntityAlive() && this.trainId.equals(e.trainId));
        if (kin.isEmpty()) {
            return this;
        }
        EntityTrain best = this;
        for (EntityTrain other : kin) {
            if (other.odometer > best.odometer) {
                best = other;
            }
        }
        return best;
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
        // Extended schedule data; always written, read back with guards below.
        int[] dwellArray = new int[this.schedule.size()];
        int[] condArray = new int[this.schedule.size()];
        for (int i = 0; i < this.schedule.size(); i++) {
            dwellArray[i] = i < this.scheduleDwell.size() ? this.scheduleDwell.get(i) : DWELL_DEFAULT;
            condArray[i] = i < this.scheduleCondition.size() ? this.scheduleCondition.get(i) : TrainScheduleData.COND_TIMED;
        }
        nbt.setIntArray("SchedDwell", dwellArray);
        nbt.setIntArray("SchedCond", condArray);
        nbt.setDouble("RailSpeed", this.railSpeed);
        nbt.setDouble("Odo", this.odometer);
        nbt.setBoolean("Derailed", this.derailed);
        nbt.setInteger("Dwell", this.dwellTicks);
        nbt.setInteger("OffRail", this.offRailTicks);
        nbt.setInteger("EndStop", this.endStopTicks);
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
        // Backward-compatible guards: legacy trains predate these keys.
        this.scheduleDwell.clear();
        this.scheduleCondition.clear();
        if (nbt.hasKey("SchedDwell")) {
            int[] dwellArray = nbt.getIntArray("SchedDwell");
            for (int i = 0; i < n; i++) {
                this.scheduleDwell.add(i < dwellArray.length ? dwellArray[i] : DWELL_DEFAULT);
            }
        }
        if (nbt.hasKey("SchedCond")) {
            int[] condArray = nbt.getIntArray("SchedCond");
            for (int i = 0; i < n; i++) {
                this.scheduleCondition.add(i < condArray.length ? condArray[i] : TrainScheduleData.COND_TIMED);
            }
        }
        if (nbt.hasKey("RailSpeed")) this.railSpeed = nbt.getDouble("RailSpeed");
        if (nbt.hasKey("Odo")) this.odometer = nbt.getDouble("Odo");
        if (nbt.hasKey("Derailed")) this.derailed = nbt.getBoolean("Derailed");
        if (nbt.hasKey("Dwell")) {
            this.dwellTicks = nbt.getInteger("Dwell");
        } else {
            this.dwellTicks = DWELL_DEFAULT;
        }
        this.offRailTicks = nbt.getInteger("OffRail");
        this.endStopTicks = nbt.getInteger("EndStop");
    }

    @Override
    public EntityMinecart.Type getType() {
        return EntityMinecart.Type.RIDEABLE;
    }
}
