package nl.melonstudios.create.entity.train.graph;

import net.minecraft.block.BlockRailBase;
import net.minecraft.util.math.BlockPos;

/**
 * One rail block inside a track graph.
 *
 * <p>Backport note: the modern reference models nodes as bezier endpoints
 * with normals and per-dimension locations. The 1.12 rail network has no
 * beziers, so a node is simply the rail block position plus its vanilla
 * shape. Corner and switch shapes are flagged as junctions (turnouts) so
 * routing can prefer or penalise them later.</p>
 */
public final class TrackGraphNode {
    private final BlockPos pos;
    private final BlockRailBase.EnumRailDirection shape;
    private final boolean junction;
    private boolean station;

    public TrackGraphNode(BlockPos pos, BlockRailBase.EnumRailDirection shape, boolean junction) {
        this.pos = pos.toImmutable();
        this.shape = shape;
        this.junction = junction;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public BlockRailBase.EnumRailDirection getShape() {
        return this.shape;
    }

    /** True for corners and multi-neighbour switches (turnouts). */
    public boolean isJunction() {
        return this.junction;
    }

    /** True when a station block sits next to this rail. Set post-build. */
    public boolean isStation() {
        return this.station;
    }

    public void setStation(boolean station) {
        this.station = station;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrackGraphNode)) return false;
        return this.pos.equals(((TrackGraphNode) o).pos);
    }

    @Override
    public int hashCode() {
        return this.pos.hashCode();
    }

    @Override
    public String toString() {
        return "TrackGraphNode" + this.pos + " " + this.shape + (this.junction ? " J" : "");
    }
}
