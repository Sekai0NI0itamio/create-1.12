package nl.melonstudios.create.tileentity.logistics;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nl.melonstudios.create.block.actor.BlockBeltBase;
import nl.melonstudios.create.block.logistics.BlockBrassTunnel;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.tileentity.actor.TileEntityBeltBase;
import nl.melonstudios.create.tileentity.marker.ITopOpenInventory;
import nl.melonstudios.create.util.filter.IItemFilter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Brass tunnel: a belt-cover splitter/sorter sitting on top of a belt segment.
 * Pulls items off the belt below (only while the belt moves, mirroring the
 * reference requirement that the segment below has speed) and pushes them out
 * across the horizontal side outputs, preferring sides that carry a filter.
 *
 * Ports BrassTunnelBlockEntity's observable behaviour: per-side exact filters,
 * filtered-first distribution, the seven selection modes (split, forced split,
 * round robin, forced round robin, prefer nearest, randomize, synchronize),
 * connected-tunnel rows along the tunnel axis sharing one output group, a
 * short distribution delay, flap animation + sound on transfer, and
 * take-back of held stacks by hand.
 *
 * Backport notes: behaviours replace capabilities where 1.12 lacks them;
 * intake additionally accepts hopper/pipe insertion via the exposed
 * insert-only item handler and ITopOpenInventory, since belt code cannot push
 * upwards into a cover. Pulling is gated on at least one live output so a
 * dead-end tunnel never starves the belt underneath it.
 */
public class TileEntityBrassTunnel extends TileEntityOptimizedBase implements ITopOpenInventory {
    /** Ticks between distribution attempts (reference funnel defaultExtractionTimer). */
    public static final int DISTRIBUTE_DELAY = 8;
    /** Cap on row walks so a degenerate tunnel loop cannot hang the tick. */
    private static final int MAX_ROW = 16;

    public enum SelectionMode {
        SPLIT("Split"),
        FORCED_SPLIT("Forced split"),
        ROUND_ROBIN("Round robin"),
        FORCED_ROUND_ROBIN("Forced round robin"),
        PREFER_NEAREST("Prefer nearest"),
        RANDOMIZE("Randomize"),
        SYNCHRONIZE("Synchronize");

        public final String label;

        SelectionMode(String label) {
            this.label = label;
        }

        public SelectionMode next() {
            SelectionMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    /** One candidate output: a side of one tunnel in the connected row. */
    private static class Target {
        final TileEntityBrassTunnel owner;
        final EnumFacing side;
        final boolean filtered;

        Target(TileEntityBrassTunnel owner, EnumFacing side, boolean filtered) {
            this.owner = owner;
            this.side = side;
            this.filtered = filtered;
        }
    }

    public ItemStack held = ItemStack.EMPTY;
    private EnumFacing enteredFrom = null;
    public SelectionMode mode = SelectionMode.SPLIT;
    private int prevIndex = 0;
    private int timer = 0;
    /** Flap animation clock driving renderers; counts down from FLAP_TIME. */
    public static final int FLAP_TIME = 8;
    public int flapTicks = 0;
    public int lastFlapTicks = 0;
    private final Map<EnumFacing, IItemFilter> filters = new EnumMap<>(EnumFacing.class);
    private boolean connectedLeft = false;
    private boolean connectedRight = false;
    private boolean syncedOutput = false;
    private boolean connectionsDirty = true;

    private final IItemHandler inputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? TileEntityBrassTunnel.this.held.copy() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty()) return stack;
            if (!TileEntityBrassTunnel.this.held.isEmpty()) return stack;
            int cap = Math.min(stack.getMaxStackSize(), 64);
            ItemStack accepted = stack.copy();
            accepted.setCount(Math.min(cap, stack.getCount()));
            ItemStack remainder = stack.copy();
            remainder.shrink(accepted.getCount());
            if (!simulate) TileEntityBrassTunnel.this.setHeld(accepted, null);
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };

    //region helpers

    @Nullable
    private TileEntityBeltBase beltBelow() {
        return this.beltBelowAt(this.pos.down());
    }

    @Nullable
    private TileEntityBeltBase beltBelowAt(BlockPos beltPos) {
        if (this.world == null) return null;
        TileEntity te = this.world.getTileEntity(beltPos);
        if (!(te instanceof TileEntityBeltBase)) return null;
        if (!te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP)) return null;
        return (TileEntityBeltBase) te;
    }

    private EnumFacing.Axis tunnelAxis() {
        if (this.world == null) return EnumFacing.Axis.X;
        net.minecraft.block.state.IBlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() instanceof BlockBrassTunnel) {
            return state.getValue(BlockBrassTunnel.AXIS);
        }
        return EnumFacing.Axis.X;
    }

    private static EnumFacing negativeOf(EnumFacing.Axis axis) {
        return axis == EnumFacing.Axis.X ? EnumFacing.WEST : EnumFacing.NORTH;
    }

    private static EnumFacing positiveOf(EnumFacing.Axis axis) {
        return axis == EnumFacing.Axis.X ? EnumFacing.EAST : EnumFacing.SOUTH;
    }

    private boolean isRowMember(BlockPos p, EnumFacing.Axis axis) {
        if (this.world == null) return false;
        if (!this.world.getBlockState(p).getBlock().equals(this.getBlockType())) return false;
        TileEntity te = this.world.getTileEntity(p);
        if (!(te instanceof TileEntityBrassTunnel)) return false;
        return ((TileEntityBrassTunnel) te).tunnelAxis() == axis;
    }

    private List<TileEntityBrassTunnel> rowMembers() {
        List<TileEntityBrassTunnel> members = new ArrayList<>();
        members.add(this);
        if (this.world == null) return members;
        EnumFacing.Axis axis = this.tunnelAxis();
        for (EnumFacing dir : new EnumFacing[]{negativeOf(axis), positiveOf(axis)}) {
            BlockPos p = this.pos.offset(dir);
            for (int i = 0; i < MAX_ROW && this.isRowMember(p, axis); i++) {
                members.add((TileEntityBrassTunnel) this.world.getTileEntity(p));
                p = p.offset(dir);
            }
        }
        return members;
    }

    /** Sides with a physical opening (row-internal faces carry no flap). */
    private boolean hasFlap(EnumFacing side) {
        if (side.getAxis() == EnumFacing.Axis.Y) return false;
        if (this.world == null) return true;
        EnumFacing.Axis axis = this.tunnelAxis();
        if (side.getAxis() == axis) {
            return !this.isRowMember(this.pos.offset(side), axis);
        }
        return true;
    }

    public boolean hasDistributionBehaviour() {
        boolean anyFlap = false;
        boolean offAxisFlap = false;
        EnumFacing.Axis axis = this.tunnelAxis();
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            if (!this.hasFlap(side)) continue;
            anyFlap = true;
            if (side.getAxis() != axis) offAxisFlap = true;
        }
        if (!anyFlap) return false;
        if (this.connectedLeft || this.connectedRight) return true;
        return offAxisFlap;
    }

    public boolean testFilter(EnumFacing side, ItemStack stack) {
        IItemFilter filter = this.filters.get(side);
        return filter == null || filter.matches(stack);
    }

    public void setFilter(EnumFacing side, @Nullable IItemFilter filter) {
        if (side.getAxis() == EnumFacing.Axis.Y) return;
        if (filter == null) this.filters.remove(side);
        else this.filters.put(side, filter);
        this.markDirty();
        this.sync();
    }

    public int filterCount() {
        return this.filters.size();
    }

    public void cycleMode() {
        this.mode = this.mode.next();
        this.markDirty();
        this.sync();
    }

    private void setHeld(ItemStack stack, @Nullable EnumFacing from) {
        this.held = stack;
        this.enteredFrom = from;
        this.timer = DISTRIBUTE_DELAY;
        this.markDirty();
        this.sync();
    }

    public void clearHeld() {
        this.held = ItemStack.EMPTY;
        this.enteredFrom = null;
        this.timer = 0;
        this.markDirty();
        this.sync();
    }

    private void flap() {
        this.flapTicks = FLAP_TIME;
        if (this.world != null && !this.world.isRemote) {
            this.world.playSound(null, this.pos, SoundInit.funnel_flap, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        this.markDirty();
        this.sync();
    }

    /** Belt flow direction under the given owner (reference getMovementFacing). */
    private static EnumFacing flowFacing(TileEntityBeltBase belt) {
        EnumFacing.Axis axis = ((BlockBeltBase) belt.getBlockType()).getTransportAxis(belt.getState());
        return belt.getFlag() ? positiveOf(axis) : negativeOf(axis);
    }

    //endregion

    //region connections

    @Override
    public void onLoad() {
        super.onLoad();
        this.connectionsDirty = true;
    }

    public void refreshConnections() {
        if (this.world == null || this.pos == null) return;
        if (!(this.world.getBlockState(this.pos).getBlock() instanceof BlockBrassTunnel)) return;
        EnumFacing.Axis axis = this.tunnelAxis();
        boolean left = this.isRowMember(this.pos.offset(negativeOf(axis)), axis);
        boolean right = this.isRowMember(this.pos.offset(positiveOf(axis)), axis);
        this.connectionsDirty = false;
        if (left == this.connectedLeft && right == this.connectedRight) return;
        this.connectedLeft = left;
        this.connectedRight = right;
        if (!this.world.isRemote) {
            for (EnumFacing dir : new EnumFacing[]{negativeOf(axis), positiveOf(axis)}) {
                TileEntity te = this.world.getTileEntity(this.pos.offset(dir));
                if (te instanceof TileEntityBrassTunnel) {
                    TileEntityBrassTunnel other = (TileEntityBrassTunnel) te;
                    if (other.tunnelAxis() == axis && other.mode != this.mode) {
                        other.mode = this.mode;
                        other.markDirty();
                        other.sync();
                    }
                }
            }
            this.markDirty();
            this.sync();
        }
    }

    //endregion

    //region tick

    @Override
    public void tick() {
        this.lastFlapTicks = this.flapTicks;
        if (this.flapTicks > 0) this.flapTicks--;
        if (this.world.isRemote) return;
        if (this.connectionsDirty) this.refreshConnections();
        TileEntityBeltBase belt = this.beltBelow();
        if (belt == null || belt.getSpeed() == 0.0F) return;
        if (this.held.isEmpty()) this.pullFromBelt(belt);
        if (this.held.isEmpty()) return;
        if (--this.timer > 0) return;
        this.timer = DISTRIBUTE_DELAY;
        this.distributeGroup();
    }

    @Override
    public void tickLazy() {
    }

    private void pullFromBelt(TileEntityBeltBase belt) {
        if (!this.hasDistributionBehaviour()) return;
        IItemHandler handler =
                belt.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        if (handler == null) return;
        // Only capture when at least one output is live, so a dead-end tunnel
        // never starves the belt running underneath it.
        List<Target> filtered = new ArrayList<>();
        List<Target> plain = new ArrayList<>();
        this.gatherTargets(filtered, plain);
        if (filtered.isEmpty() && plain.isEmpty()) return;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack simulated = handler.extractItem(i, 64, true);
            if (simulated.isEmpty()) continue;
            ItemStack taken = handler.extractItem(i, 64, false);
            if (taken.isEmpty()) continue;
            this.setHeld(taken, null);
            this.flap();
            return;
        }
    }

    //endregion

    //region distribution

    private void gatherTargets(List<Target> filtered, List<Target> plain) {
        for (TileEntityBrassTunnel member : this.rowMembers()) {
            TileEntityBeltBase belt = member.beltBelowAt(member.pos.down());
            EnumFacing flow = belt == null ? null : flowFacing(belt);
            for (EnumFacing side : EnumFacing.HORIZONTALS) {
                if (member == this && side == this.enteredFrom) continue;
                if (!member.hasFlap(side)) continue;
                BlockPos out = member.pos.offset(side);
                TileEntity outTe = member.world.getTileEntity(out);
                if (outTe instanceof TileEntityBrassTunnel) continue;
                boolean live = false;
                if (outTe != null && outTe.hasCapability(
                        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) {
                    live = true;
                } else if (outTe instanceof ITopOpenInventory) {
                    live = true;
                } else if (outTe == null && flow != null && side == flow
                        && member.world.getBlockState(out).getMaterial().isReplaceable()) {
                    live = true;
                }
                if (!live) continue;
                Target target = new Target(member, side, member.filters.get(side) != null);
                if (target.filtered) filtered.add(target);
                else plain.add(target);
            }
        }
    }

    /**
     * Push one stack towards a target. Returns null when the target rejects
     * the stack (filter mismatch or incompatible neighbour), otherwise the
     * remainder (possibly empty).
     */
    @Nullable
    private ItemStack pushInto(Target target, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!target.owner.testFilter(target.side, stack)) return null;
        TileEntityBeltBase belt = target.owner.beltBelowAt(target.owner.pos.down());
        BlockPos out = target.owner.pos.offset(target.side);
        TileEntity outTe = target.owner.world.getTileEntity(out);
        if (outTe != null && !(outTe instanceof TileEntityBrassTunnel) && outTe.hasCapability(
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, target.side.getOpposite())) {
            IItemHandler handler = outTe.getCapability(
                    CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, target.side.getOpposite());
            ItemStack remainder = ItemHandlerHelper.insertItem(handler, stack.copy(), simulate);
            if (!simulate && remainder.getCount() != stack.getCount()) target.owner.flap();
            return remainder;
        }
        if (outTe instanceof ITopOpenInventory) {
            ITopOpenInventory inv = (ITopOpenInventory) outTe;
            if (simulate) {
                return inv.isInsertionSlotEmpty(stack) ? ItemStack.EMPTY : stack;
            }
            ItemStack remainder = inv.tryInsertItem(stack, target.side.getOpposite());
            if (remainder.getCount() != stack.getCount()) target.owner.flap();
            return remainder;
        }
        if (belt != null && target.side == flowFacing(belt)
                && target.owner.world.getBlockState(out).getMaterial().isReplaceable()) {
            if (!simulate) {
                target.owner.flap();
                StackUtil.spawnItemWithVelocity(target.owner.world,
                        out.getX() + 0.5, out.getY() + 0.85, out.getZ() + 0.5, stack.copy(),
                        target.side.getFrontOffsetX() * 0.15, 0.2,
                        target.side.getFrontOffsetZ() * 0.15);
            }
            return ItemStack.EMPTY;
        }
        return null;
    }

    private static boolean accepts(@Nullable ItemStack remainder, ItemStack offered) {
        return remainder != null && remainder.getCount() < offered.getCount();
    }

    private static boolean fullyAccepts(@Nullable ItemStack remainder) {
        return remainder != null && remainder.isEmpty();
    }

    private void distributeGroup() {
        List<TileEntityBrassTunnel> members = this.rowMembers();
        if (this.mode == SelectionMode.SYNCHRONIZE) {
            boolean allHeld = true;
            for (TileEntityBrassTunnel member : members) {
                if (member.held.isEmpty()) {
                    allHeld = false;
                    break;
                }
            }
            boolean changed = allHeld != this.syncedOutput;
            this.syncedOutput = allHeld;
            if (changed) {
                this.markDirty();
                this.sync();
            }
            if (!allHeld) return;
            for (TileEntityBrassTunnel member : members) member.pushNearest();
            return;
        }
        List<Target> filtered = new ArrayList<>();
        List<Target> plain = new ArrayList<>();
        this.gatherTargets(filtered, plain);
        // Filtered outputs pick first; the remainder cascades to the rest,
        // mirroring the reference filtered/non-filtered target lists.
        this.distributeInto(filtered);
        this.distributeInto(plain);
    }

    private void distributeInto(List<Target> targets) {
        if (targets.isEmpty() || this.held.isEmpty()) return;
        switch (this.mode) {
            case SPLIT:
                this.splitCycle(targets, false);
                break;
            case FORCED_SPLIT:
                this.splitCycle(targets, true);
                break;
            case ROUND_ROBIN:
                this.roundRobin(targets, false);
                break;
            case FORCED_ROUND_ROBIN:
                this.roundRobin(targets, true);
                break;
            case RANDOMIZE: {
                java.util.Random rand = this.world.rand;
                int start = rand.nextInt(targets.size());
                for (int k = 0; k < targets.size(); k++) {
                    Target target = targets.get((start + k) % targets.size());
                    ItemStack probe = this.pushInto(target, this.held.copy(), true);
                    if (!accepts(probe, this.held)) continue;
                    ItemStack remainder = this.pushInto(target, this.held.copy(), false);
                    if (remainder != null) this.settleRemainder(remainder);
                    return;
                }
                break;
            }
            case PREFER_NEAREST:
            case SYNCHRONIZE:
                this.pushNearest(targets);
                break;
            default:
                break;
        }
    }

    private void pushNearest() {
        List<Target> filtered = new ArrayList<>();
        List<Target> plain = new ArrayList<>();
        this.gatherTargets(filtered, plain);
        this.pushNearest(filtered);
        if (!this.held.isEmpty()) this.pushNearest(plain);
    }

    private void pushNearest(List<Target> targets) {
        if (targets.isEmpty() || this.held.isEmpty()) return;
        for (Target target : targets) {
            if (!accepts(this.pushInto(target, this.held.copy(), true), this.held)) continue;
            ItemStack remainder = this.pushInto(target, this.held.copy(), false);
            if (remainder != null) this.settleRemainder(remainder);
            return;
        }
    }

    private void splitCycle(List<Target> targets, boolean forced) {
        for (int cycle = 0; cycle < 2 && !this.held.isEmpty(); cycle++) {
            List<Target> live = new ArrayList<>();
            ItemStack single = this.held.copy();
            single.setCount(1);
            for (Target target : targets) {
                ItemStack probe = this.pushInto(target, single, true);
                if (forced ? fullyAccepts(probe) : accepts(probe, single)) live.add(target);
            }
            if (live.isEmpty()) return;
            int remaining = this.held.getCount();
            ItemStack kind = this.held.copy();
            int share = remaining / live.size();
            int extra = remaining % live.size();
            for (Target target : live) {
                int give = share + (extra > 0 ? 1 : 0);
                if (extra > 0) extra--;
                if (give <= 0) continue;
                ItemStack offer = kind.copy();
                offer.setCount(Math.min(give, remaining));
                ItemStack remainder = this.pushInto(target, offer, false);
                if (remainder == null) continue;
                remaining -= offer.getCount() - remainder.getCount();
                if (remaining <= 0) break;
            }
            if (remaining == this.held.getCount()) return;
            ItemStack rest = this.held.copy();
            rest.setCount(remaining);
            this.settleRemainder(rest);
        }
    }

    private void roundRobin(List<Target> targets, boolean forced) {
        int n = targets.size();
        for (int k = 0; k < n; k++) {
            int idx = (this.prevIndex + k) % n;
            Target target = targets.get(idx);
            ItemStack probe = this.pushInto(target, this.held.copy(), true);
            if (!(forced ? fullyAccepts(probe) : accepts(probe, this.held))) continue;
            ItemStack remainder = this.pushInto(target, this.held.copy(), false);
            if (remainder != null) {
                this.prevIndex = (idx + 1) % n;
                this.settleRemainder(remainder);
            }
            return;
        }
        this.prevIndex = (this.prevIndex + 1) % n;
        this.markDirty();
    }

    private void settleRemainder(ItemStack remainder) {
        if (remainder.isEmpty()) {
            this.clearHeld();
        } else {
            this.held = remainder;
            this.markDirty();
            this.sync();
        }
    }

    //endregion

    //region intake API

    public boolean canInput(ItemStack stack, @Nullable EnumFacing side) {
        if (stack.isEmpty() || !this.held.isEmpty()) return false;
        return side == null || this.testFilter(side, stack);
    }

    @Override
    public ItemStack tryInsertItem(ItemStack stack) {
        return this.tryInsertItem(stack, null);
    }

    @Override
    public boolean isInsertionSlotEmpty(ItemStack stack) {
        return this.held.isEmpty();
    }

    @Override
    public ItemStack tryInsertItem(ItemStack stack, @Nullable EnumFacing side) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!this.canInput(stack, side)) return stack;
        int cap = Math.min(stack.getMaxStackSize(), 64);
        ItemStack accepted = stack.copy();
        accepted.setCount(Math.min(cap, stack.getCount()));
        ItemStack remainder = stack.copy();
        remainder.shrink(accepted.getCount());
        this.setHeld(accepted, side);
        return remainder;
    }

    /** Take-back used by hand retrieval (own plus connected row members). */
    public List<ItemStack> grabRow(boolean simulate) {
        List<ItemStack> taken = new ArrayList<>();
        for (TileEntityBrassTunnel member : this.rowMembers()) {
            if (member.held.isEmpty()) continue;
            taken.add(member.held.copy());
            if (!simulate) member.clearHeld();
        }
        return taken;
    }

    //endregion

    //region persistence

    private static String filterKey(EnumFacing side) {
        switch (side) {
            case NORTH:
                return "FilterN";
            case SOUTH:
                return "FilterS";
            case WEST:
                return "FilterW";
            case EAST:
                return "FilterE";
            default:
                return "FilterN";
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (!this.held.isEmpty()) nbt.setTag("Held", this.held.writeToNBT(new NBTTagCompound()));
        nbt.setByte("EnteredFrom", (byte) (this.enteredFrom == null ? -1 : this.enteredFrom.getIndex()));
        nbt.setByte("Mode", (byte) this.mode.ordinal());
        nbt.setInteger("PrevIndex", this.prevIndex);
        nbt.setInteger("Timer", this.timer);
        nbt.setInteger("Flap", this.flapTicks);
        nbt.setBoolean("ConnL", this.connectedLeft);
        nbt.setBoolean("ConnR", this.connectedRight);
        nbt.setBoolean("Synced", this.syncedOutput);
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            IItemFilter filter = this.filters.get(side);
            if (filter != null) nbt.setTag(filterKey(side), filter.serialize(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.held = nbt.hasKey("Held", 10) ? new ItemStack(nbt.getCompoundTag("Held")) : ItemStack.EMPTY;
        byte from = nbt.getByte("EnteredFrom");
        this.enteredFrom = from < 0 ? null : EnumFacing.getFront(from);
        SelectionMode[] modes = SelectionMode.values();
        this.mode = modes[nbt.getByte("Mode") % modes.length];
        this.prevIndex = nbt.getInteger("PrevIndex");
        this.timer = nbt.getInteger("Timer");
        this.flapTicks = nbt.getInteger("Flap");
        this.lastFlapTicks = this.flapTicks;
        this.connectedLeft = nbt.getBoolean("ConnL");
        this.connectedRight = nbt.getBoolean("ConnR");
        this.syncedOutput = nbt.getBoolean("Synced");
        this.filters.clear();
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            String key = filterKey(side);
            if (nbt.hasKey(key, 10)) {
                IItemFilter filter = IItemFilter.deserialize(nbt.getCompoundTag(key));
                if (filter != null) this.filters.put(side, filter);
            }
        }
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        if (!this.held.isEmpty()) nbt.setTag("Held", this.held.writeToNBT(new NBTTagCompound()));
        nbt.setByte("Mode", (byte) this.mode.ordinal());
        nbt.setInteger("Flap", this.flapTicks);
        nbt.setBoolean("ConnL", this.connectedLeft);
        nbt.setBoolean("ConnR", this.connectedRight);
        nbt.setBoolean("Synced", this.syncedOutput);
        int mask = 0;
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            if (this.filters.get(side) != null) mask |= 1 << side.getHorizontalIndex();
        }
        nbt.setByte("FilterMask", (byte) mask);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.held = nbt.hasKey("Held", 10) ? new ItemStack(nbt.getCompoundTag("Held")) : ItemStack.EMPTY;
        SelectionMode[] modes = SelectionMode.values();
        this.mode = modes[nbt.getByte("Mode") % modes.length];
        this.lastFlapTicks = this.flapTicks;
        this.flapTicks = nbt.getInteger("Flap");
        this.connectedLeft = nbt.getBoolean("ConnL");
        this.connectedRight = nbt.getBoolean("ConnR");
        this.syncedOutput = nbt.getBoolean("Synced");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        ByteBuf temp = Unpooled.buffer();
        ByteBufUtils.writeTag(temp, this.writePacket());
        StackUtil.writeItemStack(this.held, temp, true, true);
        temp.writeByte(this.mode.ordinal());
        buf.writeBytes(temp);
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            IItemFilter filter = this.filters.get(side);
            if (filter != null) {
                buf.writeBoolean(true);
                filter.serialize(buf);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(ByteBufUtils.readTag(buf));
        this.held = StackUtil.readItemStack(buf, true, true);
        SelectionMode[] modes = SelectionMode.values();
        this.mode = modes[buf.readUnsignedByte() % modes.length];
        this.filters.clear();
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            if (buf.readBoolean()) {
                IItemFilter filter = IItemFilter.deserialize(buf);
                if (filter != null) this.filters.put(side, filter);
            }
        }
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }

    @Override
    public void destroy() {
        StackUtil.dropItemsAt(this.world, this.pos, this.held);
    }

    //endregion

    //region capabilities

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) this.inputHandler;
        }
        return super.getCapability(capability, facing);
    }

    //endregion
}
