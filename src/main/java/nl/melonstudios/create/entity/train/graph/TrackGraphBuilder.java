package nl.melonstudios.create.entity.train.graph;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.block.train.BlockStation;
import nl.melonstudios.create.entity.train.TrainTrackFollower;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Bounded flood that turns nearby vanilla-shape rails into a {@link TrackGraph}.
 *
 * <p>Walks out from a seed position, following each rail shape's exit arms
 * (straights run along one axis, corners expose two arms, slopes are found by
 * probing one block up and down at each arm offset). A node counts as a
 * junction when its shape is a corner or when more than two physical rail
 * neighbours exist (a placed switch). Rails next to a station block are
 * flagged so routing can snap schedule stops to the graph.</p>
 *
 * <p>Read-only against the world: never places, breaks or updates blocks.</p>
 */
public final class TrackGraphBuilder {
    public static final int DEFAULT_RADIUS = 64;
    public static final int DEFAULT_MAX_NODES = 1024;

    private TrackGraphBuilder() {
        throw new AssertionError("no");
    }

    public static TrackGraph build(World world, BlockPos origin) {
        return build(world, origin, DEFAULT_RADIUS, DEFAULT_MAX_NODES);
    }

    public static TrackGraph build(World world, BlockPos origin, int radius, int maxNodes) {
        TrackGraph graph = new TrackGraph();
        if (world == null || origin == null) return graph;
        if (radius < 1) radius = 1;
        if (maxNodes < 1) maxNodes = 1;

        BlockPos seed = nearestRail(world, origin, 4);
        if (seed == null) return graph;

        double radiusSq = (double) radius * (double) radius;
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        open.add(seed);
        seen.add(seed);

        while (!open.isEmpty() && graph.nodeCount() < maxNodes) {
            BlockPos at = open.poll();
            if (at.distanceSq(origin) > radiusSq + 64.0) continue;

            BlockRailBase.EnumRailDirection shape = railShape(world, at);
            if (shape == null) continue;

            Set<BlockPos> neighbours = railNeighbours(world, at, shape);
            boolean junction = isCorner(shape) || neighbours.size() > 2;
            TrackGraphNode node = new TrackGraphNode(at, shape, junction);
            node.setStation(nearStation(world, at));
            graph.addNode(node);

            for (BlockPos next : neighbours) {
                if (next.distanceSq(origin) > radiusSq) continue;
                if (railShape(world, next) == null) continue;
                BlockRailBase.EnumRailDirection nextShape = railShape(world, next);
                Set<BlockPos> nextNeighbours = railNeighbours(world, next, nextShape);
                boolean nextJunction = isCorner(nextShape) || nextNeighbours.size() > 2;
                TrackGraphNode nextNode = new TrackGraphNode(next, nextShape, nextJunction);
                nextNode.setStation(nearStation(world, next));
                graph.addNode(nextNode);
                graph.link(node, graph.nodeAt(next));
                if (!seen.contains(next) && graph.nodeCount() + open.size() < maxNodes + 64) {
                    seen.add(next);
                    open.add(next);
                }
                if (graph.nodeCount() >= maxNodes) break;
            }
        }
        return graph;
    }

    /** Exit-arm offsets (dx, dz) per vanilla rail shape. Slopes share the flat axis. */
    static int[][] armsFor(BlockRailBase.EnumRailDirection shape) {
        switch (shape) {
            case EAST_WEST:
            case ASCENDING_EAST:
            case ASCENDING_WEST:
                return new int[][]{{1, 0}, {-1, 0}};
            case NORTH_SOUTH:
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
                return new int[][]{{0, 1}, {0, -1}};
            case SOUTH_EAST:
                return new int[][]{{0, 1}, {1, 0}};
            case SOUTH_WEST:
                return new int[][]{{0, 1}, {-1, 0}};
            case NORTH_WEST:
                return new int[][]{{0, -1}, {-1, 0}};
            case NORTH_EAST:
            default:
                return new int[][]{{0, -1}, {1, 0}};
        }
    }

    private static boolean isCorner(BlockRailBase.EnumRailDirection shape) {
        return shape == BlockRailBase.EnumRailDirection.SOUTH_EAST
                || shape == BlockRailBase.EnumRailDirection.SOUTH_WEST
                || shape == BlockRailBase.EnumRailDirection.NORTH_WEST
                || shape == BlockRailBase.EnumRailDirection.NORTH_EAST;
    }

    /** Physical rail neighbours: each arm probed at the same level, one up and one down. */
    private static Set<BlockPos> railNeighbours(World world, BlockPos at,
            BlockRailBase.EnumRailDirection shape) {
        Set<BlockPos> out = new HashSet<>();
        for (int[] arm : armsFor(shape)) {
            BlockPos base = at.add(arm[0], 0, arm[1]);
            if (railShape(world, base) != null) {
                out.add(base);
            } else if (railShape(world, base.up()) != null) {
                out.add(base.up());
            } else if (railShape(world, base.down()) != null) {
                out.add(base.down());
            }
        }
        return out;
    }

    private static BlockRailBase.EnumRailDirection railShape(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) return null;
        try {
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (!(block instanceof BlockRailBase)) return null;
            return state.getValue(((BlockRailBase) block).getShapeProperty());
        } catch (Exception e) {
            return null;
        }
    }

    /** True when a station block touches the rail (face neighbours plus one down). */
    private static boolean nearStation(World world, BlockPos railPos) {
        BlockPos[] around = new BlockPos[]{
                railPos.north(), railPos.south(), railPos.east(), railPos.west(),
                railPos.up(), railPos.down(), railPos.down().north(),
                railPos.down().south(), railPos.down().east(), railPos.down().west()};
        for (BlockPos p : around) {
            if (!world.isBlockLoaded(p)) continue;
            try {
                if (world.getBlockState(p).getBlock() instanceof BlockStation) return true;
            } catch (Exception e) {
                // Treat unreadable blocks as non-stations.
            }
        }
        return false;
    }

    /** Closest rail block within {@code range} of origin (cube scan), or null. */
    private static BlockPos nearestRail(World world, BlockPos origin, int range) {
        if (railShape(world, origin) != null) return origin;
        // Reuse the follower's feet/down probe so carts standing between levels seed correctly.
        BlockRailBase.EnumRailDirection under =
                TrainTrackFollower.railShapeAt(world, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        if (under != null) {
            if (railShape(world, origin) != null) return origin;
            if (railShape(world, origin.down()) != null) return origin.down();
        }
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos p = origin.add(dx, dy, dz);
                    if (railShape(world, p) == null) continue;
                    double d = p.distanceSq(origin);
                    if (d < bestSq) {
                        bestSq = d;
                        best = p.toImmutable();
                    }
                }
            }
        }
        return best;
    }
}
