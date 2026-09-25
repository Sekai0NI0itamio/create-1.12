package nl.melonstudios.create.entity.train.graph;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Pathfinding over a {@link TrackGraph}.
 *
 * <p>Two searches, both returning an ordered rail-position list from start to
 * destination (inclusive) or an empty list when unreachable:</p>
 * <ul>
 * <li>{@link #findPathBfs} — breadth-first, fewest rails, best for small yards.</li>
 * <li>{@link #findPathAStar} — A* with a Manhattan heuristic, better on long
 * lines. Junction crossings add a small penalty so routes prefer plain
 * through track, echoing the reference turnout cost without its signals.</li>
 * </ul>
 *
 * <p>Endpoints snap to the nearest graph node (exact hit first, then the
 * closest node within a few blocks) so station blocks beside the line still
 * route.</p>
 */
public final class TrackPathfinder {
    /** Extra cost added per junction node entered during A*. */
    public static final double JUNCTION_PENALTY = 0.5;
    /** Snap radius (blocks) from a requested endpoint to the nearest node. */
    public static final double SNAP_RADIUS = 4.0;

    private TrackPathfinder() {
        throw new AssertionError("no");
    }

    public static List<BlockPos> findPathBfs(TrackGraph graph, BlockPos from, BlockPos to) {
        if (graph == null || graph.isEmpty() || from == null || to == null) {
            return Collections.emptyList();
        }
        TrackGraphNode start = snapToNode(graph, from);
        TrackGraphNode goal = snapToNode(graph, to);
        if (start == null || goal == null) return Collections.emptyList();
        if (start.equals(goal)) {
            List<BlockPos> single = new ArrayList<>();
            single.add(start.getPos());
            return single;
        }
        Map<TrackGraphNode, TrackGraphNode> prev = new HashMap<>();
        Set<TrackGraphNode> seen = new HashSet<>();
        Deque<TrackGraphNode> open = new ArrayDeque<>();
        open.add(start);
        seen.add(start);
        while (!open.isEmpty()) {
            TrackGraphNode at = open.poll();
            if (at.equals(goal)) break;
            for (TrackGraphEdge edge : graph.edgesFrom(at)) {
                TrackGraphNode next = edge.other(at);
                if (seen.add(next)) {
                    prev.put(next, at);
                    open.add(next);
                }
            }
        }
        if (!seen.contains(goal)) return Collections.emptyList();
        return rebuild(prev, start, goal);
    }

    public static List<BlockPos> findPathAStar(TrackGraph graph, BlockPos from, BlockPos to) {
        if (graph == null || graph.isEmpty() || from == null || to == null) {
            return Collections.emptyList();
        }
        TrackGraphNode start = snapToNode(graph, from);
        TrackGraphNode goal = snapToNode(graph, to);
        if (start == null || goal == null) return Collections.emptyList();
        if (start.equals(goal)) {
            List<BlockPos> single = new ArrayList<>();
            single.add(start.getPos());
            return single;
        }
        Map<TrackGraphNode, Double> best = new HashMap<>();
        Map<TrackGraphNode, TrackGraphNode> prev = new HashMap<>();
        Set<TrackGraphNode> closed = new HashSet<>();
        PriorityQueue<Entry> open = new PriorityQueue<>();
        best.put(start, 0.0);
        open.add(new Entry(start, heuristic(start, goal)));
        while (!open.isEmpty()) {
            Entry entry = open.poll();
            TrackGraphNode at = entry.node;
            if (!closed.add(at)) continue;
            if (at.equals(goal)) break;
            double atCost = best.get(at);
            for (TrackGraphEdge edge : graph.edgesFrom(at)) {
                TrackGraphNode next = edge.other(at);
                if (closed.contains(next)) continue;
                double step = edge.getCost() + (next.isJunction() ? JUNCTION_PENALTY : 0.0);
                double candidate = atCost + step;
                Double known = best.get(next);
                if (known == null || candidate < known) {
                    best.put(next, candidate);
                    prev.put(next, at);
                    open.add(new Entry(next, candidate + heuristic(next, goal)));
                }
            }
        }
        if (!best.containsKey(goal)) return Collections.emptyList();
        return rebuild(prev, start, goal);
    }

    /** Total centre distance along a route; 0 for routes under two points. */
    public static double pathLength(List<BlockPos> route) {
        if (route == null || route.size() < 2) return 0.0;
        double total = 0.0;
        for (int i = 1; i < route.size(); i++) {
            BlockPos a = route.get(i - 1);
            BlockPos b = route.get(i);
            double dx = a.getX() - b.getX();
            double dy = a.getY() - b.getY();
            double dz = a.getZ() - b.getZ();
            total += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        return total;
    }

    static TrackGraphNode snapToNode(TrackGraph graph, BlockPos pos) {
        TrackGraphNode exact = graph.nodeAt(pos);
        if (exact != null) return exact;
        // Stations sit beside the line, so also try one block down (rail under the platform edge).
        TrackGraphNode below = graph.nodeAt(pos.down());
        if (below != null) return below;
        TrackGraphNode best = null;
        double bestSq = SNAP_RADIUS * SNAP_RADIUS;
        for (TrackGraphNode node : graph.nodes()) {
            double d = node.getPos().distanceSq(pos);
            if (d < bestSq) {
                bestSq = d;
                best = node;
            }
        }
        return best;
    }

    private static List<BlockPos> rebuild(Map<TrackGraphNode, TrackGraphNode> prev,
            TrackGraphNode start, TrackGraphNode goal) {
        List<BlockPos> out = new ArrayList<>();
        TrackGraphNode at = goal;
        while (at != null) {
            out.add(at.getPos());
            if (at.equals(start)) break;
            at = prev.get(at);
        }
        Collections.reverse(out);
        if (out.isEmpty() || !out.get(0).equals(start.getPos())) return Collections.emptyList();
        return out;
    }

    private static double heuristic(TrackGraphNode a, TrackGraphNode b) {
        BlockPos pa = a.getPos();
        BlockPos pb = b.getPos();
        return Math.abs(pa.getX() - pb.getX()) + Math.abs(pa.getY() - pb.getY()) + Math.abs(pa.getZ() - pb.getZ());
    }

    private static final class Entry implements Comparable<Entry> {
        final TrackGraphNode node;
        final double priority;

        Entry(TrackGraphNode node, double priority) {
            this.node = node;
            this.priority = priority;
        }

        @Override
        public int compareTo(Entry o) {
            return Double.compare(this.priority, o.priority);
        }
    }
}
