package nl.melonstudios.create.entity.train;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Schedule data helpers shared by the schedule item and the train entity.
 *
 * A schedule is an ordered stop list. Each stop holds a station position, a
 * dwell time in ticks, and a wait condition. This mirrors the reference
 * design (ordered destination entries each carrying wait conditions) without
 * porting its UI, graph or per-tick runtime: the 1.12 train keeps looping its
 * stops and simply waits per-stop dwell ticks.
 *
 * NBT layout (both on the item stack tag and, in array form, on the train):
 * item tag "Stops": list of compounds {X, Y, Z, Dwell, Cond}.
 */
public final class TrainScheduleData {
    /** Depart as soon as the arrival frame completes (no timed wait). */
    public static final int COND_NONE = 0;
    /** Hold at the station for the stop's dwell ticks. */
    public static final int COND_TIMED = 1;

    public static final int DWELL_DEFAULT = 100;
    public static final int DWELL_NONE_PAUSE = 10;
    public static final int MAX_STOPS = 16;

    private TrainScheduleData() {
        throw new AssertionError("no");
    }

    public static NBTTagList writeStops(List<BlockPos> stops, List<Integer> dwells, List<Integer> conds) {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < stops.size(); i++) {
            BlockPos p = stops.get(i);
            NBTTagCompound e = new NBTTagCompound();
            e.setInteger("X", p.getX());
            e.setInteger("Y", p.getY());
            e.setInteger("Z", p.getZ());
            e.setInteger("Dwell", (dwells != null && dwells.size() > i) ? dwells.get(i) : DWELL_DEFAULT);
            e.setInteger("Cond", (conds != null && conds.size() > i) ? conds.get(i) : COND_TIMED);
            list.appendTag(e);
        }
        return list;
    }

    public static void readStops(NBTTagCompound tag, List<BlockPos> stops, List<Integer> dwells, List<Integer> conds) {
        stops.clear();
        if (dwells != null) dwells.clear();
        if (conds != null) conds.clear();
        if (tag == null || !tag.hasKey("Stops", 9)) return;
        NBTTagList list = tag.getTagList("Stops", 10);
        for (int i = 0; i < list.tagCount() && i < MAX_STOPS; i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            // Missing-key guards: tolerate hand-written or older tags.
            int x = e.hasKey("X") ? e.getInteger("X") : 0;
            int y = e.hasKey("Y") ? e.getInteger("Y") : 0;
            int z = e.hasKey("Z") ? e.getInteger("Z") : 0;
            stops.add(new BlockPos(x, y, z));
            if (dwells != null) dwells.add(e.hasKey("Dwell") ? e.getInteger("Dwell") : DWELL_DEFAULT);
            if (conds != null) conds.add(e.hasKey("Cond") ? e.getInteger("Cond") : COND_TIMED);
        }
    }

    /** Dwell actually waited at a stop: NONE waits only a short pause. */
    public static int effectiveDwell(int dwell, int cond) {
        if (cond == COND_NONE) return DWELL_NONE_PAUSE;
        return Math.max(0, dwell);
    }

    public static List<BlockPos> copyStops(List<BlockPos> stops) {
        List<BlockPos> out = new ArrayList<>(stops.size());
        for (BlockPos p : stops) out.add(p.toImmutable());
        return out;
    }
}
