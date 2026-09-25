package nl.melonstudios.create.entity.train.graph;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only routing queries the train movement can call later.
 *
 * <p>Nothing here touches {@code EntityTrain} state or movement: callers pass
 * positions in and get routes back. {@code EntityTrain.onUpdate} keeps its
 * current local rail-following behaviour by default; a future step can
 * optionally consult {@link #planSchedule} to order stops or
 * {@link #nextWaypoint} to steer at junctions.</p>
 */
public final class TrackRouting {
    private TrackRouting() {
        throw new AssertionError("no");
    }

    /** Nearest graph node to a rail position, or null when the graph is empty. */
    public static TrackGraphNode nearestNode(TrackGraph graph, BlockPos pos) {
        if (graph == null || graph.isEmpty() || pos == null) return null;
        return TrackPathfinder.snapToNode(graph, pos);
    }

    /** Nearest graph node to an entity position. */
    public static TrackGraphNode nearestNode(TrackGraph graph, double x, double y, double z) {
        return nearestNode(graph, new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
    }

    /** One leg of rail route between two stops (empty when unreachable). */
    public static List<BlockPos> planLeg(TrackGraph graph, BlockPos from, BlockPos to) {
        return TrackPathfinder.findPathAStar(graph, from, to);
    }

    /**
     * Route a full schedule: builds one graph around the train, then chains
     * A* legs stop to stop. Returns the concatenated rail path plus the stops
     * in schedule order; legs that do not connect are recorded, never thrown.
     */
    public static RoutePlan planSchedule(World world, BlockPos trainPos, List<BlockPos> stations) {
        return planSchedule(world, trainPos, stations,
                TrackGraphBuilder.DEFAULT_RADIUS, TrackGraphBuilder.DEFAULT_MAX_NODES);
    }

    public static RoutePlan planSchedule(World world, BlockPos trainPos,
            List<BlockPos> stations, int radius, int maxNodes) {
        List<BlockPos> stops = stations == null
                ? Collections.<BlockPos>emptyList()
                : new ArrayList<>(stations);
        TrackGraph graph = TrackGraphBuilder.build(world, trainPos, radius, maxNodes);
        List<BlockPos> path = new ArrayList<>();
        List<Integer> missingLegs = new ArrayList<>();
        if (!graph.isEmpty() && !stops.isEmpty()) {
            BlockPos legStart = trainPos;
            for (int i = 0; i < stops.size(); i++) {
                List<BlockPos> leg = TrackPathfinder.findPathAStar(graph, legStart, stops.get(i));
                if (leg.isEmpty()) {
                    missingLegs.add(i);
                } else {
                    if (!path.isEmpty()) leg.remove(0);
                    path.addAll(leg);
                }
                legStart = stops.get(i);
            }
        } else if (!stops.isEmpty()) {
            for (int i = 0; i < stops.size(); i++) missingLegs.add(i);
        }
        return new RoutePlan(graph, stops, path, missingLegs);
    }

    /**
     * Next rail waypoint after the route point closest to the train. Returns
     * null on empty routes or when already at the final point; callers keep
     * current behaviour in that case.
     */
    public static BlockPos nextWaypoint(List<BlockPos> route, BlockPos trainPos) {
        if (route == null || route.isEmpty() || trainPos == null) return null;
        int best = 0;
        double bestSq = Double.MAX_VALUE;
        for (int i = 0; i < route.size(); i++) {
            double d = route.get(i).distanceSq(trainPos);
            if (d < bestSq) {
                bestSq = d;
                best = i;
            }
        }
        int next = best + 1;
        return next < route.size() ? route.get(next) : null;
    }

    /** True when every schedule leg connected through the graph. */
    public static boolean isFullyRouted(RoutePlan plan) {
        return plan != null && plan.isFullyRouted();
    }

    /** Schedule routing answer: ordered stops, concatenated rail path, gaps. */
    public static final class RoutePlan {
        private final TrackGraph graph;
        private final List<BlockPos> stops;
        private final List<BlockPos> railPath;
        private final List<Integer> missingLegs;

        RoutePlan(TrackGraph graph, List<BlockPos> stops, List<BlockPos> railPath, List<Integer> missingLegs) {
            this.graph = graph;
            this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
            this.railPath = Collections.unmodifiableList(new ArrayList<>(railPath));
            this.missingLegs = Collections.unmodifiableList(new ArrayList<>(missingLegs));
        }

        public TrackGraph getGraph() {
            return this.graph;
        }

        public List<BlockPos> getStops() {
            return this.stops;
        }

        public List<BlockPos> getRailPath() {
            return this.railPath;
        }

        public List<Integer> getMissingLegs() {
            return this.missingLegs;
        }

        public boolean isFullyRouted() {
            return !this.stops.isEmpty() && this.missingLegs.isEmpty() && !this.railPath.isEmpty();
        }

        public double pathLength() {
            return TrackPathfinder.pathLength(this.railPath);
        }
    }
}
