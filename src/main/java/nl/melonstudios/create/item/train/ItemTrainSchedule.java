package nl.melonstudios.create.item.train;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.entity.train.EntityTrain;
import nl.melonstudios.create.entity.train.TrainScheduleData;
import nl.melonstudios.create.tileentity.train.TileEntityStation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Train schedule (foundation): stores an ordered stop list in item NBT.
 *
 * Each stop is a station position plus a dwell time plus a wait condition
 * (timed hold, or none for a pass-through pause). This mirrors the reference
 * schedule concept (ordered destination entries each carrying wait
 * conditions) without its UI: stops are recorded by sneak-using the schedule
 * on stations in travel order, and the schedule is assigned by sneak-using it
 * in the air near the consist, which loads it into the nearest train.
 *
 * No UI, no graph routing; the train keeps looping stops with per-stop dwell.
 */
public class ItemTrainSchedule extends Item {
    public ItemTrainSchedule() {
        super();
        this.setMaxStackSize(1);
        this.setRegistryName("schedule");
        this.setUnlocalizedName("create.schedule");
    }

    private static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }

    public static int stopCount(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey("Stops", 9)) return 0;
        return tag.getTagList("Stops", 10).tagCount();
    }

    /**
     * Sneak-use on a station: append the station as the next stop. Sneak-using
     * the station that is already the last stop toggles that stop's wait
     * condition between timed and none. Returns SUCCESS so the station does
     * not assemble on the same click.
     */
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote || !player.isSneaking()) return EnumActionResult.PASS;
        if (!(world.getTileEntity(pos) instanceof TileEntityStation)) return EnumActionResult.PASS;

        NBTTagCompound tag = tag(stack);
        List<BlockPos> stops = new ArrayList<>();
        List<Integer> dwells = new ArrayList<>();
        List<Integer> conds = new ArrayList<>();
        TrainScheduleData.readStops(tag, stops, dwells, conds);

        if (!stops.isEmpty() && stops.get(stops.size() - 1).equals(pos)) {
            int last = conds.size() - 1;
            int next = conds.get(last) == TrainScheduleData.COND_TIMED
                    ? TrainScheduleData.COND_NONE : TrainScheduleData.COND_TIMED;
            conds.set(last, next);
            tag.setTag("Stops", TrainScheduleData.writeStops(stops, dwells, conds));
            player.sendStatusMessage(new TextComponentString("Schedule stop " + stops.size()
                    + ": " + (next == TrainScheduleData.COND_TIMED ? "timed wait" : "no wait") + "."), true);
            return EnumActionResult.SUCCESS;
        }

        if (stops.size() >= TrainScheduleData.MAX_STOPS) {
            player.sendStatusMessage(new TextComponentString("Schedule is full (" + TrainScheduleData.MAX_STOPS + " stops)."), true);
            return EnumActionResult.SUCCESS;
        }
        stops.add(pos.toImmutable());
        dwells.add(TrainScheduleData.DWELL_DEFAULT);
        conds.add(TrainScheduleData.COND_TIMED);
        tag.setTag("Stops", TrainScheduleData.writeStops(stops, dwells, conds));
        player.sendStatusMessage(new TextComponentString("Schedule stop " + stops.size()
                + " added. Sneak-click air near the train to assign."), true);
        return EnumActionResult.SUCCESS;
    }

    /**
     * Sneak-use in the air: load this schedule into the nearest train within
     * 8 blocks and start it. Plain right-click only reports the stop count.
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote || !player.isSneaking()) return new ActionResult<>(EnumActionResult.PASS, stack);

        NBTTagCompound tag = stack.getTagCompound();
        List<BlockPos> stops = new ArrayList<>();
        List<Integer> dwells = new ArrayList<>();
        List<Integer> conds = new ArrayList<>();
        TrainScheduleData.readStops(tag, stops, dwells, conds);
        if (stops.isEmpty()) {
            player.sendStatusMessage(new TextComponentString("Schedule is empty. Sneak-click stations in order first."), true);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        List<EntityTrain> trains = world.getEntitiesWithinAABB(EntityTrain.class,
                new AxisAlignedBB(player.posX - 8, player.posY - 4, player.posZ - 8,
                        player.posX + 8, player.posY + 4, player.posZ + 8),
                e -> e != null && e.isEntityAlive());
        if (trains.isEmpty()) {
            player.sendStatusMessage(new TextComponentString("No train nearby. Stand by the consist and sneak-click again."), true);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        EntityTrain nearest = trains.get(0);
        for (EntityTrain t : trains) {
            if (t.getDistance(player) < nearest.getDistance(player)) nearest = t;
        }
        nearest.applyScheduleData(stops, dwells, conds);
        nearest.start();
        player.sendStatusMessage(new TextComponentString("Schedule assigned (" + stops.size() + " stops). Train departing."), true);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /** Tooltip: stop count plus the first stops with their wait conditions. */
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        int n = stopCount(stack);
        if (n == 0) {
            tooltip.add(TextFormatting.GRAY + "Empty. Sneak-click stations in travel order.");
            return;
        }
        tooltip.add(TextFormatting.GOLD + "" + n + (n == 1 ? " stop" : " stops"));
        NBTTagList list = stack.getTagCompound().getTagList("Stops", 10);
        for (int i = 0; i < Math.min(n, 6); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            int cond = e.hasKey("Cond") ? e.getInteger("Cond") : TrainScheduleData.COND_TIMED;
            tooltip.add(TextFormatting.GRAY + "" + (i + 1) + ". "
                    + e.getInteger("X") + ", " + e.getInteger("Y") + ", " + e.getInteger("Z")
                    + (cond == TrainScheduleData.COND_TIMED ? " (timed)" : " (pass)"));
        }
        if (n > 6) tooltip.add(TextFormatting.GRAY + "...");
        tooltip.add(TextFormatting.DARK_GRAY + "Sneak-click air near a train to assign.");
    }
}
