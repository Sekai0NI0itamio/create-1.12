package nl.melonstudios.create.tileentity.funnel;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.block.actor.BlockBeltBase;
import nl.melonstudios.create.block.state.EnumFunnelState;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.tileentity.actor.TileEntityBeltBase;
import nl.melonstudios.create.tileentity.marker.IDepot;
import nl.melonstudios.create.tileentity.marker.ITopOpenInventory;
import nl.melonstudios.create.util.filter.IItemFilter;

import javax.annotation.Nullable;
import java.util.List;

public class TileEntityFunnelWall extends TileEntityFunnelBase implements ITickable {
    public int cooldown = 0;
    /** Flap animation state (reference: LerpedFloat flap). Counts down from 8 after a transfer; renderers read it. */
    public int flap = 0;
    public TileEntityFunnelWall() {
        super();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("cooldown", this.cooldown);
        nbt.setInteger("Flap", this.flap);
        return super.writeToNBT(nbt);
    }
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.cooldown = nbt.getInteger("cooldown");
        this.flap = nbt.getInteger("Flap");
        super.readFromNBT(nbt);
    }

    @Override
    public boolean isPowered(int meta) {
        return (meta & 0b1000) != 0;
    }
    public EnumFunnelState getFunnelState(int meta) {
        return EnumFunnelState.VALUES[(meta >> 2) & 0b0001];
    }
    public EnumFacing getFacing(int meta) {
        return EnumFacing.getHorizontal(meta & 0b0011);
    }

    protected TileEntity depot = null;
    protected void getDepot() {
        if (this.depot == null || this.depot.isInvalid()) {
            this.depot = this.world.getTileEntity(this.pos.down());
        }
    }

    @Nullable
    protected IItemFilter getFilter() {
        return null;
    }
    protected int getExtractionAmount() {
        return 1;
    }
    protected boolean isExtractionAmountExact() {
        return false;
    }

    /**
     * Reference FunnelBlockEntity.onTransfer + flap: runs on every successful
     * transfer, snaps the flap open and plays the flap sound.
     */
    protected void onTransfer() {
        this.flap = 8;
        if (this.world != null && !this.world.isRemote) {
            this.world.playSound(null, this.pos, SoundInit.funnel_flap, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        this.markDirty();
    }

    @Override
    public void update() {
        if (!this.world.isRemote) {
            if (this.flap > 0) {
                this.flap--;
                this.markDirty();
            }
            if (this.isPowered()) {
                if (this.cooldown != 0) {
                    this.cooldown = 0;
                    this.markDirty();
                }
            } else {
                if (this.cooldown > 0) {
                    this.cooldown--;
                    this.markDirty();
                } else {
                    IItemHandler inventory = this.getInventory(this.getFacing(this.getBlockMetadata()).getOpposite());
                    if (inventory != null && inventory.getSlots() > 0) {
                        EnumFunnelState state = this.getFunnelState(this.getBlockMetadata());
                        this.tick(inventory, state);
                    }
                }
            }
        }
    }

    private BlockPos lastAABBPos = null;
    private AxisAlignedBB lastAABB = null;
    private AxisAlignedBB getEntityAABB() {
        if (this.lastAABB == null || (!this.pos.equals(this.lastAABBPos))) {
            this.lastAABB = new AxisAlignedBB(this.pos);
            this.lastAABBPos = this.pos.toImmutable();
        }
        return this.lastAABB;
    }

    /**
     * Belt pull (reference BeltFunnelInteractionHandler.checkForFunnels, TAKING_FROM_BELT).
     * A funnel sitting above a belt segment pulls riders off the belt into the
     * attached inventory (the neighbour on the funnel's source side). Belt items
     * ride at belt-top height (+0.75, see TESRBeltBase / IDepot.getItemHeight),
     * so this reads the belt segment TE directly below instead of scanning the
     * funnel's own block space, which is a full block higher and never overlaps
     * the riders.
     *
     * Ported reference rules:
     * - Powered funnels never pull (gated by update(), which also resets cooldown).
     * - No pull when the funnel faces along the belt movement (items pass under).
     *   Perpendicular and blocking (facing against movement) funnels pull.
     * - Pickup at the segment center: funnelEntry = segment + .5, i.e. the mouth
     *   slot where the entry half reaches 1.0 (positive flow) / 0.0 (negative
     *   flow). Both the approaching window (funnel ticks before the belt) and
     *   the just-crossed window (funnel ticks after the belt moved the stack
     *   into the exit half) are accepted, so the pull does not depend on TE
     *   tick order; either way the stack renders at the segment center.
     * - Filter mismatch pulls nothing; brass exact-amount shortfall pulls
     *   nothing; target-full pulls nothing and sets no cooldown.
     * - Reads the belt halves directly, so diagonal segments (which expose no
     *   IDepot presented item) work too. Remainder is restored to the half it
     *   came from, so leftovers keep riding instead of teleporting.
     * - Flap + sound on transfer, 8-tick cooldown (reference defaultExtractionTimer = 8).
     *
     * @return true if items moved (cooldown set, caller must return).
     */
    private boolean tryPullFromBelt(TileEntityBeltBase belt, IItemHandler inventory) {
        if (inventory == null || inventory.getSlots() <= 0) return false;
        float speed = belt.getSpeed();
        if (speed == 0.0F) return false;
        IBlockState beltState = this.world.getBlockState(belt.getPos());
        if (!(beltState.getBlock() instanceof BlockBeltBase)) return false;
        if (!((BlockBeltBase) beltState.getBlock()).isFunctional(beltState)) return false;

        EnumFacing movement = this.getBeltMovementFacing(belt);
        if (movement != null && this.getFacing(this.getBlockMetadata()) == movement) return false;

        double step = Math.abs((double) speed) / 240.0;
        double eps = 1.0E-4;
        boolean positive = belt.getFlag();
        boolean fromLeft;
        if (positive) {
            if (!belt.left.isEmpty() && belt.leftPos >= 1.0 - step - eps) {
                fromLeft = true;
            } else if (!belt.right.isEmpty() && belt.rightPos >= -eps && belt.rightPos <= step + eps) {
                fromLeft = false;
            } else return false;
        } else {
            if (!belt.right.isEmpty() && belt.rightPos <= 0.0 + step + eps) {
                fromLeft = false;
            } else if (!belt.left.isEmpty() && belt.leftPos >= 1.0 - step - eps
                    && belt.leftPos <= 1.0 + step + eps) {
                fromLeft = true;
            } else return false;
        }

        ItemStack peek = (fromLeft ? belt.left : belt.right).copy();
        if (peek.isEmpty()) return false;
        if (this.getFilter() != null && !this.getFilter().matches(peek)) return false;

        int amount = Math.max(this.getExtractionAmount(), 1);
        if (this.isExtractionAmountExact() && peek.getCount() < amount) return false;
        int take = this.isExtractionAmountExact() ? Math.min(amount, peek.getCount()) : peek.getCount();

        ItemStack sim = peek.copy();
        sim.setCount(take);
        for (int i = 0; i < inventory.getSlots(); i++) {
            sim = inventory.insertItem(i, sim, true);
            if (sim.isEmpty()) break;
        }
        if (sim.getCount() >= take) return false;
        int moved = take - sim.getCount();

        ItemStack extracted;
        if (fromLeft) {
            extracted = belt.left.splitStack(moved);
            if (belt.left.isEmpty()) {
                belt.left = ItemStack.EMPTY;
                belt.leftPosOld = belt.leftPos = 0.5;
            }
        } else {
            extracted = belt.right.splitStack(moved);
            if (belt.right.isEmpty()) {
                belt.right = ItemStack.EMPTY;
                belt.rightPosOld = belt.rightPos = 0.5;
            }
        }
        belt.sync();
        if (extracted.isEmpty()) return false;
        ItemStack leftover = extracted.copy();
        for (int i = 0; i < inventory.getSlots() && !leftover.isEmpty(); i++) {
            leftover = inventory.insertItem(i, leftover, false);
        }
        if (!leftover.isEmpty()) {
            if (fromLeft) {
                if (belt.left.isEmpty()) {
                    belt.left = leftover.copy();
                } else if (ItemStack.areItemsEqual(belt.left, leftover)
                        && ItemStack.areItemStackTagsEqual(belt.left, leftover)) {
                    belt.left.grow(leftover.getCount());
                } else {
                    belt.left = leftover.copy();
                }
            } else {
                if (belt.right.isEmpty()) {
                    belt.right = leftover.copy();
                } else if (ItemStack.areItemsEqual(belt.right, leftover)
                        && ItemStack.areItemStackTagsEqual(belt.right, leftover)) {
                    belt.right.grow(leftover.getCount());
                } else {
                    belt.right = leftover.copy();
                }
            }
            belt.sync();
        }
        this.onTransfer();
        this.cooldown = 8;
        this.markDirty();
        return true;
    }

    @Nullable
    private EnumFacing getBeltMovementFacing(TileEntityBeltBase belt) {
        IBlockState beltState = this.world.getBlockState(belt.getPos());
        if (!(beltState.getBlock() instanceof BlockBeltBase)) return null;
        EnumFacing.Axis axis = ((BlockBeltBase) beltState.getBlock()).getTransportAxis(beltState);
        if (axis == null || axis == EnumFacing.Axis.Y) return null;
        // Mirrors TileEntityBeltBase.tick: X-axis belts move negated (speed *= -1).
        boolean positive = (belt.getSpeed() > 0.0F) != (axis == EnumFacing.Axis.X);
        return EnumFacing.getFacingFromAxis(positive
                ? EnumFacing.AxisDirection.POSITIVE : EnumFacing.AxisDirection.NEGATIVE, axis);
    }

    @SuppressWarnings("unchecked")
    private <DEPOT extends TileEntity & IDepot, TOP_OPEN extends TileEntity & ITopOpenInventory> void tick(IItemHandler inventory, EnumFunnelState state) {
        this.getDepot();
        if (state == EnumFunnelState.INSERTING) {
            if (this.depot instanceof IDepot) {
                if (this.depot instanceof TileEntityBeltBase) {
                    if (this.tryPullFromBelt((TileEntityBeltBase) this.depot, inventory)) return;
                }
                DEPOT depot = (DEPOT) this.depot;
                ItemStack presented = depot.getPresentedItem();
                if (presented.isEmpty() || (this.getFilter() != null && !this.getFilter().matches(presented))) return;
                ItemStack copy = presented.copy();
                for (int i = 0; i < inventory.getSlots(); i++) {
                    copy = inventory.insertItem(i, copy, false);
                    if (copy.isEmpty()) break;
                }
                if (copy.getCount() >= presented.getCount()) return;
                depot.setPresentedItem(copy.isEmpty() ? ItemStack.EMPTY : copy);
                this.onTransfer();
                this.cooldown = 8;
                this.markDirty();
            } else {
                List<EntityItem> candidates = this.world.getEntitiesWithinAABB(EntityItem.class, this.getEntityAABB());
                for (EntityItem entity : candidates) {
                    if (entity.isDead) continue;
                    ItemStack stack = entity.getItem();
                    if (stack.isEmpty()) {
                        this.world.removeEntity(entity);
                        continue;
                    }
                    if (this.getFilter() != null && !this.getFilter().matches(stack)) continue;
                    ItemStack copy = stack.copy();
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        copy = inventory.insertItem(i, copy, false);
                        if (copy.isEmpty()) break;
                    }
                    if (!ItemStack.areItemStacksEqual(stack, copy)) {
                        if (copy.isEmpty()) {
                            this.world.removeEntity(entity);
                            entity.setItem(ItemStack.EMPTY);
                        } else entity.setItem(copy);
                        this.onTransfer();
                        this.cooldown = 8;
                        this.markDirty();
                        break;
                    }
                }
            }
        } else {
            if (this.depot instanceof ITopOpenInventory) {
                TOP_OPEN topOpen = (TOP_OPEN) this.depot;
                if (topOpen.isInsertionSlotEmpty(ItemStack.EMPTY)) {
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        ItemStack stack = inventory.extractItem(i, this.getExtractionAmount(), true);
                        if (stack.isEmpty() || (this.isExtractionAmountExact() && stack.getCount() != this.getExtractionAmount())) continue;
                        if (this.getFilter() != null && !this.getFilter().matches(stack)) continue;
                        if (!topOpen.isInsertionSlotEmpty(stack)) continue;
                        stack = inventory.extractItem(i, this.getExtractionAmount(), false);
                        stack = topOpen.tryInsertItem(stack, this.getFacing(this.getBlockMetadata()).getOpposite());
                        if (!stack.isEmpty()) {

                        }
                        this.onTransfer();
                        this.cooldown = 8;
                        this.markDirty();
                        break;
                    }
                }
            } else {
                List<EntityItem> candidates = this.world.getEntitiesWithinAABB(EntityItem.class, this.getEntityAABB());
                if (candidates.isEmpty()) {
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        ItemStack stack = inventory.extractItem(i, this.getExtractionAmount(), true);
                        if (stack.isEmpty() || (this.isExtractionAmountExact() && stack.getCount() != this.getExtractionAmount())) continue;
                        if (this.getFilter() != null && !this.getFilter().matches(stack)) continue;
                        stack = inventory.extractItem(i, this.getExtractionAmount(), false);
                        EntityItem entity = new EntityItem(this.world,
                                this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5,
                                stack.copy()
                        );
                        entity.motionX = entity.motionY = entity.motionZ = 0.0;
                        this.world.spawnEntity(entity);
                        this.onTransfer();
                        this.cooldown = 8;
                        this.markDirty();
                        break;
                    }
                }
            }
        }
    }
}
