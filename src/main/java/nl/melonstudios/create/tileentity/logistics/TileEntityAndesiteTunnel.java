package nl.melonstudios.create.tileentity.logistics;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nl.melonstudios.create.block.actor.BlockBeltBase;
import nl.melonstudios.create.block.logistics.BlockAndesiteTunnel;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.tileentity.actor.TileEntityBeltBase;
import nl.melonstudios.create.tileentity.marker.ITopOpenInventory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Andesite tunnel TE: unfiltered version of the brass tunnel distributor.
 * Ports the reference BeltTunnelBlockEntity behaviour: pulls items off the
 * belt below (only while the belt moves) and pushes them out across the
 * horizontal side outputs of the whole connected row, split evenly.
 * No filters, no selection modes. Distribution delay 8 ticks, flap 8 ticks
 * with flap sound on transfer, take-back of held stacks by hand.
 */
public class TileEntityAndesiteTunnel extends TileEntityOptimizedBase implements ITopOpenInventory {
    /** Ticks between distribution attempts (mirrors the brass tunnel delay). */
    public static final int DISTRIBUTE_DELAY = 8;
    /** Cap on row walks so a degenerate tunnel loop cannot hang the tick. */
    private static final int MAX_ROW = 16;
    /** Flap animation clock driving renderers; counts down from FLAP_TIME. */
    public static final int FLAP_TIME = 8;

    public ItemStack held = ItemStack.EMPTY;
    private int timer = 0;
    public int flapTicks = 0;
    public int lastFlapTicks = 0;
    private boolean connectedLeft = false;
    private boolean connectedRight = false;
    private boolean connectionsDirty = true;

    private final IItemHandler inputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? TileEntityAndesiteTunnel.this.held.copy() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty()) return stack;
            if (!TileEntityAndesiteTunnel.this.held.isEmpty()) return stack;
            int cap = Math.min(stack.getMaxStackSize(), 64);
            ItemStack accepted = stack.copy();
            accepted.setCount(Math.min(cap, stack.getCount()));
            ItemStack remainder = stack.copy();
            remainder.shrink(accepted.getCount());
            if (!simulate) TileEntityAndesiteTunnel.this.setHeld(accepted);
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
        if (state.getBlock() instanceof BlockAndesiteTunnel) {
            return state.getValue(BlockAndesiteTunnel.AXIS);
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
        TileEntity te = this.world.getTileEntity(p);
        if (!(te instanceof TileEntityAndesiteTunnel)) return false;
        return ((TileEntityAndesiteTunnel) te).tunnelAxis() == axis;
    }

    private List<TileEntityAndesiteTunnel> rowMembers() {
        List<TileEntityAndesiteTunnel> members = new ArrayList<>();
        members.add(this);
        if (this.world == null) return members;
        EnumFacing.Axis axis = this.tunnelAxis();
        for (EnumFacing dir : new EnumFacing[]{negativeOf(axis), positiveOf(axis)}) {
            BlockPos p = this.pos.offset(dir);
            for (int i = 0; i < MAX_ROW && this.isRowMember(p, axis); i++) {
                members.add((TileEntityAndesiteTunnel) this.world.getTileEntity(p));
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

    private void setHeld(ItemStack stack) {
        this.held = stack;
        this.timer = DISTRIBUTE_DELAY;
        this.markDirty();
        this.sync();
    }

    public void clearHeld() {
        this.held = ItemStack.EMPTY;
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

    /** Belt flow direction under the given owner. */
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
        if (!(this.world.getBlockState(this.pos).getBlock() instanceof BlockAndesiteTunnel)) return;
        EnumFacing.Axis axis = this.tunnelAxis();
        boolean left = this.isRowMember(this.pos.offset(negativeOf(axis)), axis);
        boolean right = this.isRowMember(this.pos.offset(positiveOf(axis)), axis);
        this.connectionsDirty = false;
        if (left == this.connectedLeft && right == this.connectedRight) return;
        this.connectedLeft = left;
        this.connectedRight = right;
        this.markDirty();
        this.sync();
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
        this.distribute();
    }

    @Override
    public void tickLazy() {
    }

    private void pullFromBelt(TileEntityBeltBase belt) {
        if (!this.hasDistributionBehaviour()) return;
        IItemHandler handler =
                belt.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        if (handler == null) return;
        if (!this.hasLiveOutput()) return;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.extractItem(i, 64, true).isEmpty()) continue;
            ItemStack taken = handler.extractItem(i, 64, false);
            if (taken.isEmpty()) continue;
            this.setHeld(taken);
            this.flap();
            return;
        }
    }

    private boolean hasLiveOutput() {
        for (TileEntityAndesiteTunnel member : this.rowMembers()) {
            TileEntityBeltBase belt = member.beltBelowAt(member.pos.down());
            EnumFacing flow = belt == null ? null : flowFacing(belt);
            for (EnumFacing side : EnumFacing.HORIZONTALS) {
                if (!member.hasFlap(side)) continue;
                BlockPos out = member.pos.offset(side);
                TileEntity outTe = member.world.getTileEntity(out);
                if (outTe instanceof TileEntityAndesiteTunnel) continue;
                if (outTe != null && outTe.hasCapability(
                        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) return true;
                if (outTe instanceof ITopOpenInventory) return true;
                if (outTe == null && flow != null && side == flow
                        && member.world.getBlockState(out).getMaterial().isReplaceable()) return true;
            }
        }
        return false;
    }

    //endregion

    //region distribution

    /** One candidate output: a side of one tunnel in the connected row. */
    private static class Target {
        final TileEntityAndesiteTunnel owner;
        final EnumFacing side;

        Target(TileEntityAndesiteTunnel owner, EnumFacing side) {
            this.owner = owner;
            this.side = side;
        }
    }

    private void gatherTargets(List<Target> targets) {
        for (TileEntityAndesiteTunnel member : this.rowMembers()) {
            TileEntityBeltBase belt = member.beltBelowAt(member.pos.down());
            EnumFacing flow = belt == null ? null : flowFacing(belt);
            for (EnumFacing side : EnumFacing.HORIZONTALS) {
                if (!member.hasFlap(side)) continue;
                BlockPos out = member.pos.offset(side);
                TileEntity outTe = member.world.getTileEntity(out);
                if (outTe instanceof TileEntityAndesiteTunnel) continue;
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
                if (live) targets.add(new Target(member, side));
            }
        }
    }

    @Nullable
    private ItemStack pushInto(Target target, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        TileEntityBeltBase belt = target.owner.beltBelowAt(target.owner.pos.down());
        BlockPos out = target.owner.pos.offset(target.side);
        TileEntity outTe = target.owner.world.getTileEntity(out);
        if (outTe != null && !(outTe instanceof TileEntityAndesiteTunnel) && outTe.hasCapability(
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

    /** Even split across every live output (reference default split behaviour). */
    private void distribute() {
        List<Target> targets = new ArrayList<>();
        this.gatherTargets(targets);
        if (targets.isEmpty() || this.held.isEmpty()) return;
        for (int cycle = 0; cycle < 2 && !this.held.isEmpty(); cycle++) {
            List<Target> live = new ArrayList<>();
            ItemStack single = this.held.copy();
            single.setCount(1);
            for (Target target : targets) {
                ItemStack probe = this.pushInto(target, single, true);
                if (probe != null && probe.getCount() < single.getCount()) live.add(target);
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
            if (rest.isEmpty()) this.clearHeld();
            else {
                this.held = rest;
                this.markDirty();
                this.sync();
            }
        }
    }

    //endregion

    //region intake API

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
        if (stack.isEmpty() || !this.held.isEmpty()) return stack;
        int cap = Math.min(stack.getMaxStackSize(), 64);
        ItemStack accepted = stack.copy();
        accepted.setCount(Math.min(cap, stack.getCount()));
        ItemStack remainder = stack.copy();
        remainder.shrink(accepted.getCount());
        this.setHeld(accepted);
        return remainder;
    }

    /** Take-back used by hand retrieval (own plus connected row members). */
    public List<ItemStack> grabRow(boolean simulate) {
        List<ItemStack> taken = new ArrayList<>();
        for (TileEntityAndesiteTunnel member : this.rowMembers()) {
            if (member.held.isEmpty()) continue;
            taken.add(member.held.copy());
            if (!simulate) member.clearHeld();
        }
        return taken;
    }

    //endregion

    //region persistence

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (!this.held.isEmpty()) nbt.setTag("Held", this.held.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("Timer", this.timer);
        nbt.setInteger("Flap", this.flapTicks);
        nbt.setBoolean("ConnL", this.connectedLeft);
        nbt.setBoolean("ConnR", this.connectedRight);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.held = nbt.hasKey("Held", 10) ? new ItemStack(nbt.getCompoundTag("Held")) : ItemStack.EMPTY;
        this.timer = nbt.getInteger("Timer");
        this.flapTicks = nbt.getInteger("Flap");
        this.lastFlapTicks = this.flapTicks;
        this.connectedLeft = nbt.getBoolean("ConnL");
        this.connectedRight = nbt.getBoolean("ConnR");
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        if (!this.held.isEmpty()) nbt.setTag("Held", this.held.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("Flap", this.flapTicks);
        nbt.setBoolean("ConnL", this.connectedLeft);
        nbt.setBoolean("ConnR", this.connectedRight);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.held = nbt.hasKey("Held", 10) ? new ItemStack(nbt.getCompoundTag("Held")) : ItemStack.EMPTY;
        this.lastFlapTicks = this.flapTicks;
        this.flapTicks = nbt.getInteger("Flap");
        this.connectedLeft = nbt.getBoolean("ConnL");
        this.connectedRight = nbt.getBoolean("ConnR");
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
