package nl.melonstudios.create.entity.train.graph;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight rail graph: nodes keyed by rail block position plus an
 * adjacency list of edges.
 *
 * <p>Pure data, no world reference and no ticking. The builder fills one in a
 * bounded flood; the pathfinder reads it; the routing API answers train
 * questions from it. Deliberately far smaller than the reference graph (no
 * bezier edges, signals, sync or per-dimension bookkeeping).</p>
 */
public final class TrackGraph {
    private final Map<BlockPos, TrackGraphNode> nodes = new LinkedHashMap<>();
    private final Map<BlockPos, List<TrackGraphEdge>> adjacency = new LinkedHashMap<>();

    public void addNode(TrackGraphNode node) {
        BlockPos key = node.getPos();
        if (!this.nodes.containsKey(key)) {
            this.nodes.put(key, node);
            this.adjacency.put(key, new ArrayList<>());
        }
    }

    /** Link two known nodes; silently ignores unknown endpoints. */
    public void link(TrackGraphNode a, TrackGraphNode b) {
        if (!this.nodes.containsKey(a.getPos()) || !this.nodes.containsKey(b.getPos())) return;
        List<TrackGraphEdge> from = this.adjacency.get(a.getPos());
        for (TrackGraphEdge e : from) {
            if (e.other(a).equals(b)) return;
        }
        double dx = a.getPos().getX() - b.getPos().getX();
        double dz = a.getPos().getZ() - b.getPos().getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.5) length = 1.0;
        TrackGraphEdge edge = new TrackGraphEdge(a, b, length);
        this.adjacency.get(a.getPos()).add(edge);
        this.adjacency.get(b.getPos()).add(edge);
    }

    public TrackGraphNode nodeAt(BlockPos pos) {
        return this.nodes.get(pos);
    }

    public List<TrackGraphEdge> edgesFrom(TrackGraphNode node) {
        List<TrackGraphEdge> edges = this.adjacency.get(node.getPos());
        return edges == null ? Collections.<TrackGraphEdge>emptyList() : edges;
    }

    public Collection<TrackGraphNode> nodes() {
        return Collections.unmodifiableCollection(this.nodes.values());
    }

    public int nodeCount() {
        return this.nodes.size();
    }

    public int edgeCount() {
        int total = 0;
        for (List<TrackGraphEdge> edges : this.adjacency.values()) total += edges.size();
        return total / 2;
    }

    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }
}
