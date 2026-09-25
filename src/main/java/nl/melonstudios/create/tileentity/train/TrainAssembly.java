package nl.melonstudios.create.tileentity.train;

import com.melonstudios.melonlib.misc.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.entity.train.EntityTrain;
import nl.melonstudios.create.kinetics.contraption.ContraptionAssembly;
import nl.melonstudios.create.util.CreateTagHelper;
import nl.melonstudios.create.util.interfaces.ISelectiveImmovable;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Station-triggered train assembly and disassembly.
 *
 * Rules ported from the modern reference (station assembly + carriage
 * capture), translated to 1.12.2 idioms:
 *
 * <ul>
 *   <li>The station must stand next to a straight run of train track. The
 *   run sets the assembly direction and every bogey must sit directly
 *   above that run, one block over the rail.</li>
 *   <li>The run is at most MAX_ASSEMBLY_LENGTH rails long and a train holds
 *   at most MAX_BOGEYS bogeys in at most MAX_CARRIAGES carriages.</li>
 *   <li>The bogey closest to the station must be on the first rail past it
 *   (offset 0); any two bogeys need a gap of at least MIN_BOGEY_GAP rails.</li>
 *   <li>Each carriage floods out from its bogey anchor over 6-neighbour
 *   connected blocks. A flood holding more than two bogeys fails
 *   ("too_many_bogeys"); a flood holding only its anchor fails
 *   ("nothing_attached"). A carriage holds at most
 *   MAX_BLOCKS_PER_CARRIAGE blocks.</li>
 *   <li>Joinable: any solid movable block, tile entities kept with NBT.
 *   Never captured: air, liquids, rails, station blocks. Rejected with an
 *   error: unbreakable blocks (hardness &lt; 0), piston-immovable blocks,
 *   blocks tagged create:immovable, selectively immovable blocks.</li>
 *   <li>Assembly validates every carriage before touching the world, then
 *   removes the captured blocks, spawns one EntityTrain per carriage and
 *   links each cart to the station. Disassembly only restores a stopped
 *   linked train and puts every block (bogeys included) back.</li>
 * </ul>
 *
 * The flood uses a ContraptionAssembly counter (same structure the
 * contraption core threads through StickinessPropagator) so per-state
 * statistics stay available to future checkers.
 */
public final class TrainAssembly {
    private TrainAssembly() {
    }

    /** Longest track run a station can assemble from (reference: maxAssemblyLength). */
    public static final int MAX_ASSEMBLY_LENGTH = 32;
    /** Most bogeys on one assembled train (reference: maxBogeyCount). */
    public static final int MAX_BOGEYS = 6;
    /** Most carriages on one assembled train. */
    public static final int MAX_CARRIAGES = 6;
    /** Most captured blocks per carriage flood. */
    public static final int MAX_BLOCKS_PER_CARRIAGE = 256;
    /** Minimum rail gap between two bogey offsets (reference rule). */
    public static final int MIN_BOGEY_GAP = 3;
    /** Search radius for a linked train when disassembling. */
    public static final int DISASSEMBLE_RANGE = 16;

    /** Entity-data tag linking a spawned cart to its station. */
    public static final String TAG_STATION = "CreateTrainStation";
    /** Entity-data tag holding the cart's carriage copy. */
    public static final String TAG_CARRIAGE = "CreateCarriage";
    public static final String TAG_CARRIAGE_INDEX = "CreateCarriageIndex";

    /** Outcome of an assembly attempt. */
    public static final class Result {
        public final boolean ok;
        public final String message;
        public final List<TrainCarriageData> carriages = new ArrayList<>();

        private Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }

        public static Result ok(String message, List<TrainCarriageData> carriages) {
            Result r = new Result(true, message);
            r.carriages.addAll(carriages);
            return r;
        }
    }

    // ------------------------------------------------------------------
    // Block classification
    // ------------------------------------------------------------------

    public static boolean isBogey(IBlockState state) {
        ResourceLocation name = state.getBlock().getRegistryName();
        return name != null && name.toString().contains("bogey");
    }

    public static boolean isStationBlock(IBlockState state) {
        ResourceLocation name = state.getBlock().getRegistryName();
        return name != null && "create:station".equals(name.toString());
    }

    public static boolean isTrack(IBlockState state) {
        return state.getBlock() instanceof BlockRailBase;
    }

    private static boolean isSkipped(IBlockState state, World world, BlockPos pos) {
        if (state.getBlock().isAir(state, world, pos))
            return true;
        if (state.getMaterial().isLiquid())
            return true;
        if (isTrack(state))
            return true;
        return isStationBlock(state);
    }

    /** Null when the block may join a carriage, else the failure reason. */
    private static String immovableReason(World world, BlockPos pos, IBlockState state) {
        if (state.getBlockHardness(world, pos) < 0)
            return "Unmovable block at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        if (state.getMobilityFlag() == EnumPushReaction.BLOCK)
            return "Block cannot be moved at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        if (CreateTagHelper.isImmovable(state))
            return "Block pinned at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        if (state.getBlock() instanceof ISelectiveImmovable
                && ((ISelectiveImmovable) state.getBlock()).isImmovable(world, pos, state))
            return "Block refuses assembly at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        return null;
    }

    // ------------------------------------------------------------------
    // Track run + bogey scan
    // ------------------------------------------------------------------

    private static final class TrackRun {
        final EnumFacing direction;
        final List<BlockPos> rails = new ArrayList<>();

        TrackRun(EnumFacing direction) {
            this.direction = direction;
        }
    }

    /** Straight rail run starting next to the station, if any. */
    private static TrackRun findTrackRun(World world, BlockPos stationPos) {
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos start = stationPos.offset(facing);
            if (!world.isBlockLoaded(start))
                continue;
            if (!isTrack(world.getBlockState(start))) {
                // A rail directly under the neighbour also counts (sunk track).
                BlockPos below = start.down();
                if (world.isBlockLoaded(below) && isTrack(world.getBlockState(below)))
                    start = below;
                else
                    continue;
            }
            TrackRun run = new TrackRun(facing);
            BlockPos cursor = start;
            for (int i = 0; i < MAX_ASSEMBLY_LENGTH; i++) {
                if (!world.isBlockLoaded(cursor))
                    break;
                if (!isTrack(world.getBlockState(cursor)))
                    break;
                run.rails.add(cursor.toImmutable());
                cursor = cursor.offset(facing);
            }
            if (!run.rails.isEmpty())
                return run;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Assembly
    // ------------------------------------------------------------------

    public static Result assemble(World world, BlockPos stationPos) {
        TrackRun run = findTrackRun(world, stationPos);
        if (run == null)
            return Result.fail("No track by station");

        // Bogeys must sit directly above the run, one per rail at most.
        List<BlockPos> bogeyAnchors = new ArrayList<>();
        List<Integer> bogeyOffsets = new ArrayList<>();
        for (int i = 0; i < run.rails.size(); i++) {
            BlockPos above = run.rails.get(i).up();
            if (!world.isBlockLoaded(above))
                continue;
            if (isBogey(world.getBlockState(above))) {
                bogeyAnchors.add(above.toImmutable());
                bogeyOffsets.add(i);
            }
        }
        if (bogeyAnchors.isEmpty())
            return Result.fail("No bogeys above the track");
        if (bogeyAnchors.size() > MAX_BOGEYS)
            return Result.fail("Too many bogeys (" + bogeyAnchors.size() + ", max " + MAX_BOGEYS + ")");
        if (bogeyOffsets.get(0) != 0)
            return Result.fail("Front bogey must sit on the first rail past the station");
        for (int i = 1; i < bogeyOffsets.size(); i++) {
            if (bogeyOffsets.get(i) - bogeyOffsets.get(i - 1) < MIN_BOGEY_GAP)
                return Result.fail("Bogeys " + i + " and " + (i + 1) + " too close (min gap " + MIN_BOGEY_GAP + ")");
        }

        // Flood one carriage per anchor; a flood may swallow the next anchor
        // (two-bogey carriage) but never more than two bogeys.
        ContraptionAssembly counter = new ContraptionAssembly(new Object2IntArrayMap<>());
        Set<BlockPos> claimed = new HashSet<>();
        List<TrainCarriageData> carriages = new ArrayList<>();
        Set<BlockPos> consumedBogey = new HashSet<>();

        for (int index = 0; index < bogeyAnchors.size(); index++) {
            BlockPos anchor = bogeyAnchors.get(index);
            if (consumedBogey.contains(anchor))
                continue;
            if (carriages.size() >= MAX_CARRIAGES)
                return Result.fail("Too many carriages (max " + MAX_CARRIAGES + ")");

            TrainCarriageData carriage = new TrainCarriageData();
            String error = floodCarriage(world, anchor, claimed, counter, carriage);
            if (error != null)
                return Result.fail(error);
            if (carriage.blocks.size() <= 1)
                return Result.fail("Carriage " + (carriages.size() + 1) + ": nothing attached");

            int bogeysInCarriage = 0;
            for (BlockPos b : bogeyAnchors) {
                if (containsBlock(carriage, b)) {
                    bogeysInCarriage++;
                    consumedBogey.add(b);
                }
            }
            if (bogeysInCarriage > 2)
                return Result.fail("Carriage " + (carriages.size() + 1) + ": too many bogeys");
            carriages.add(carriage);
        }

        if (carriages.isEmpty())
            return Result.fail("No bogeys above the track");

        // All valid: remove captured blocks, then spawn one cart per carriage.
        for (TrainCarriageData carriage : carriages) {
            for (TrainCarriageData.Entry e : carriage.blocks) {
                TileEntity te = world.getTileEntity(e.pos);
                if (te != null) {
                    te.invalidate();
                    world.removeTileEntity(e.pos);
                }
                world.setBlockToAir(e.pos);
            }
        }

        EntityTrain lead = null;
        for (int i = 0; i < carriages.size(); i++) {
            TrainCarriageData carriage = carriages.get(i);
            BlockPos at = carriage.bogeys.get(0);
            EntityTrain cart = new EntityTrain(world);
            cart.setPosition(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
            cart.addStop(stationPos);
            if (lead == null) {
                lead = cart;
            } else {
                cart.trainId = lead.trainId;
            }
            cart.getEntityData().setLong(TAG_STATION, stationPos.toLong());
            cart.getEntityData().setTag(TAG_CARRIAGE, carriage.writeToNBT(new NBTTagCompound()));
            cart.getEntityData().setInteger(TAG_CARRIAGE_INDEX, i);
            world.spawnEntity(cart);
        }
        if (lead != null)
            lead.start();

        int blocks = 0;
        for (TrainCarriageData c : carriages)
            blocks += c.blockCount();
        return Result.ok("Assembled " + carriages.size() + " carriage(s), "
                + bogeyAnchors.size() + " bogey(s), " + blocks + " blocks", carriages);
    }

    private static boolean containsBlock(TrainCarriageData carriage, BlockPos pos) {
        for (TrainCarriageData.Entry e : carriage.blocks) {
            if (e.pos.equals(pos))
                return true;
        }
        return false;
    }

    /**
     * 6-neighbour flood from the bogey anchor over joinable blocks. Tracks,
     * stations, air and liquids are left standing and not traversed; anything
     * immovable aborts with a reason.
     */
    private static String floodCarriage(World world, BlockPos anchor, Set<BlockPos> claimed,
            ContraptionAssembly counter, TrainCarriageData out) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(anchor);

        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.poll();
            if (!visited.add(pos))
                continue;
            if (!world.isBlockLoaded(pos))
                return "Unloaded chunk at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            if (claimed.contains(pos))
                return "Carriages overlap at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();

            IBlockState state = world.getBlockState(pos);
            if (isSkipped(state, world, pos))
                continue;
            String blocked = immovableReason(world, pos, state);
            if (blocked != null)
                return blocked;

            counter.incrementCounter(state);
            if (out.blocks.size() >= MAX_BLOCKS_PER_CARRIAGE)
                return "Carriage too big (max " + MAX_BLOCKS_PER_CARRIAGE + " blocks)";

            TileEntity te = world.getTileEntity(pos);
            NBTTagCompound teCopy = null;
            if (te != null) {
                teCopy = new NBTTagCompound();
                te.writeToNBT(teCopy);
            }
            ResourceLocation name = state.getBlock().getRegistryName();
            String id = name == null ? "minecraft:air" : name.toString();
            @SuppressWarnings("deprecation")
            int meta = state.getBlock().getMetaFromState(state);
            out.blocks.add(new TrainCarriageData.Entry(pos.toImmutable(), id, meta, teCopy));
            if (isBogey(state))
                out.bogeys.add(pos.toImmutable());

            for (EnumFacing side : EnumFacing.VALUES) {
                BlockPos next = pos.offset(side);
                if (!visited.contains(next))
                    frontier.add(next);
            }
        }

        for (TrainCarriageData.Entry e : out.blocks)
            claimed.add(e.pos);
        return null;
    }

    // ------------------------------------------------------------------
    // Disassembly
    // ------------------------------------------------------------------

    /** Trains spawned by this station that are still nearby. */
    public static List<EntityTrain> linkedTrains(World world, BlockPos stationPos) {
        long key = stationPos.toLong();
        List<EntityTrain> out = new ArrayList<>();
        for (EntityTrain t : world.getEntitiesWithinAABB(EntityTrain.class,
                new AxisAlignedBB(stationPos).grow(DISASSEMBLE_RANGE),
                e -> e != null && e.isEntityAlive())) {
            if (t.getEntityData().hasKey(TAG_STATION)
                    && t.getEntityData().getLong(TAG_STATION) == key)
                out.add(t);
        }
        return out;
    }

    public static String disassemble(World world, BlockPos stationPos, List<TrainCarriageData> stored) {
        List<EntityTrain> trains = linkedTrains(world, stationPos);
        if (trains.isEmpty())
            return "No assembled train nearby";

        for (EntityTrain t : trains) {
            if (Math.abs(t.motionX) > 0.05 || Math.abs(t.motionZ) > 0.05)
                return "Train still moving";
        }

        // Prefer each cart's own carriage copy (survives chunk reloads via
        // ForgeData); fall back to the station's stored list by index.
        for (EntityTrain t : trains) {
            TrainCarriageData carriage = null;
            if (t.getEntityData().hasKey(TAG_CARRIAGE, 10))
                carriage = TrainCarriageData.readFromNBT(t.getEntityData().getCompoundTag(TAG_CARRIAGE));
            if (carriage == null) {
                int index = t.getEntityData().getInteger(TAG_CARRIAGE_INDEX);
                if (index >= 0 && index < stored.size())
                    carriage = stored.get(index);
            }
            if (carriage != null)
                restoreCarriage(world, carriage);
            StackUtil.spawnItemNoVelocity(world, t.posX, t.posY, t.posZ, new ItemStack(Items.MINECART));
            t.setDead();
        }
        return "Disassembled " + trains.size() + " carriage(s)";
    }

    private static void restoreCarriage(World world, TrainCarriageData carriage) {
        for (TrainCarriageData.Entry e : carriage.blocks) {
            if (!world.isBlockLoaded(e.pos))
                continue;
            Block block = Block.getBlockFromName(e.block);
            if (block == null || block == Blocks.AIR)
                continue;
            @SuppressWarnings("deprecation")
            IBlockState state = block.getStateFromMeta(e.meta);
            IBlockState current = world.getBlockState(e.pos);
            boolean free = current.getBlock().isAir(current, world, e.pos)
                    || current.getMaterial().isReplaceable();
            if (!free) {
                // Something grew into the parking spot: drop the block instead
                // of deleting what is there.
                StackUtil.spawnItemNoVelocity(world, e.pos.getX() + 0.5, e.pos.getY() + 0.5,
                        e.pos.getZ() + 0.5, new ItemStack(block, 1, e.meta));
                continue;
            }
            world.setBlockState(e.pos, state, 2);
            if (e.te != null) {
                TileEntity te = world.getTileEntity(e.pos);
                if (te != null) {
                    try {
                        // Re-point the saved data at the restored position.
                        NBTTagCompound copy = e.te.copy();
                        copy.setInteger("x", e.pos.getX());
                        copy.setInteger("y", e.pos.getY());
                        copy.setInteger("z", e.pos.getZ());
                        te.readFromNBT(copy);
                    } catch (Exception ignored) {
                        // A stale tile copy must not break the whole restore.
                    }
                }
            }
        }
    }
}
