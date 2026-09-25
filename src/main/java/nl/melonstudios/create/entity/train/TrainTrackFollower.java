package nl.melonstudios.create.entity.train;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Rail-following helpers for {@link EntityTrain}.
 *
 * Backport note: modern Create drives trains along a track graph with
 * travelling points that walk bezier edges node to node. 1.12 has no graph,
 * so the equivalent rule set is expressed against vanilla rail shapes
 * ({@link BlockRailBase.EnumRailDirection}): resolve the rail tangent under
 * the cart, keep motion projected on it, and pick the exit arm most aligned
 * with current travel at corners and switches.
 */
public final class TrainTrackFollower {
    private TrainTrackFollower() {
    }

    /** Arrival radius (blocks, horizontal) used for station braking. */
    public static final double ARRIVE_RADIUS = 2.5;

    /** Return the rail shape at the cart's feet, or null when off rails. */
    public static BlockRailBase.EnumRailDirection railShapeAt(World world, double posX, double posY, double posZ) {
        BlockPos feet = new BlockPos((int) Math.floor(posX), (int) Math.floor(posY), (int) Math.floor(posZ));
        BlockRailBase.EnumRailDirection shape = shapeAt(world, feet);
        if (shape != null) {
            return shape;
        }
        return shapeAt(world, feet.down());
    }

    private static BlockRailBase.EnumRailDirection shapeAt(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) {
            return null;
        }
        try {
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (!(block instanceof BlockRailBase)) {
                return null;
            }
            return state.getValue(((BlockRailBase) block).getShapeProperty());
        } catch (Exception e) {
            return null;
        }
    }

    /** True when a rail block exists at or one below the given position. */
    public static boolean hasRail(World world, double posX, double posY, double posZ) {
        return railShapeAt(world, posX, posY, posZ) != null;
    }

    /**
     * Unit tangent (tx, tz) for a rail shape, oriented so it agrees with the
     * reference vector (current motion when rolling, direction to the next
     * stop when standing still). Corners and switch shapes expose two arms;
     * the arm most aligned with travel wins, which is the 1.12 equivalent of
     * the reference follow-through turnout selector.
     */
    public static double[] orientedTangent(BlockRailBase.EnumRailDirection shape, double refX, double refZ) {
        double len = Math.sqrt(refX * refX + refZ * refZ);
        double rx = refX;
        double rz = refZ;
        if (len < 1.0e-4) {
            // Standing still with no hint: fall back to the shape's long axis.
            rx = 0.0;
            rz = 1.0;
        } else {
            rx /= len;
            rz /= len;
        }

        double[][] arms = armsFor(shape);
        double[] best = arms[0];
        double bestDot = best[0] * rx + best[1] * rz;
        // Arms are bidirectional; score each arm in both orientations so the
        // returned tangent never fights current travel direction.
        for (double[] arm : arms) {
            double dot = arm[0] * rx + arm[1] * rz;
            if (dot > bestDot) {
                bestDot = dot;
                best = arm;
            }
            if (-dot > bestDot) {
                bestDot = -dot;
                best = new double[]{-arm[0], -arm[1]};
            }
        }
        if (bestDot < 0) {
            best = new double[]{-best[0], -best[1]};
        }
        return best;
    }

    /** Exit-arm direction vectors for each vanilla rail shape. */
    private static double[][] armsFor(BlockRailBase.EnumRailDirection shape) {
        switch (shape) {
            case EAST_WEST:
            case ASCENDING_EAST:
            case ASCENDING_WEST:
                return new double[][]{{1.0, 0.0}};
            case NORTH_SOUTH:
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
                return new double[][]{{0.0, 1.0}};
            case SOUTH_EAST:
                return new double[][]{{0.0, 1.0}, {1.0, 0.0}};
            case SOUTH_WEST:
                return new double[][]{{0.0, 1.0}, {-1.0, 0.0}};
            case NORTH_WEST:
                return new double[][]{{0.0, -1.0}, {-1.0, 0.0}};
            case NORTH_EAST:
            default:
                return new double[][]{{0.0, -1.0}, {1.0, 0.0}};
        }
    }

    /**
     * Braking envelope: full cruise far away, tapering to a stop inside the
     * arrival radius. Mirrors the reference braking-distance rule
     * (v^2 / 2a) with a slow creep so the cart actually reaches the stop.
     */
    public static double approachAllowed(double dist, double cruise, double brake, double creep) {
        if (dist <= ARRIVE_RADIUS) {
            return 0.0;
        }
        double over = Math.max(0.0, dist - ARRIVE_RADIUS);
        double allowed = Math.sqrt(2.0 * brake * over) + creep;
        return Math.min(cruise, allowed);
    }
}
