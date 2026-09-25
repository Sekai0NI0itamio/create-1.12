package nl.melonstudios.create.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import nl.melonstudios.create.block.BlockChainConveyor;
import nl.melonstudios.create.block.state.EnumBeltPart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Chain conveyor lift logic (core loop only).
 *
 * Translated from the reference ChainConveyorBlockEntity tick: carried
 * items ride the chain at an RPM-scaled speed and load/unload at the
 * endpoints. Reference values kept:
 * - speed: |rpm| / 360 blocks per tick (reference: getSpeed() / 360, with
 *   belt comparison speed / 480 in TileEntityBeltBase).
 * - capacity: 20 carriers per line (reference
 *   CLogistics.chainConveyorCapacity default).
 * - max line length 32, min endpoint gap 3 (reference
 *   maxChainConveyorLength 32, minimum link distance 2.5).
 *
 * Backport simplifications: one vertical line per endpoint pair (no
 * routing table, no multi-connection graph), cargo is plain ItemStacks
 * (no package/port system), loading via EntityItem pickup at the intake
 * end plus hand insertion, unloading as dropped EntityItems.
 * Direction follows RPM sign: positive RPM carries upward, negative
 * downward (reference uses a separate reversed flag on speed flip).
 */
public class TileEntityChainConveyor extends TileEntityKinetic {
    /** Reference chainConveyorCapacity default. */
    public static final int CAPACITY = 20;
    /** Reference speed mapping: blocks travelled per tick per RPM. */
    public static final float SPEED_DIVISOR = 360.0F;

    public static class Carried {
        public ItemStack stack;
        /** Blocks above the bottom endpoint block (0 .. lineLength). */
        public float offset;

        public Carried(ItemStack stack, float offset) {
            this.stack = stack;
            this.offset = offset;
        }
    }

    private final List<Carried> cargo = new ArrayList<>();
    private boolean hasLine;
    private int bottomY;
    private int topY;
    private boolean refreshRequested = true;
    private int tickCounter;

    public void requestLineRefresh() {
        this.refreshRequested = true;
    }

    public boolean hasLine() {
        return this.hasLine;
    }

    public List<Carried> getCargo() {
        return this.cargo;
    }

    public int getBottomY() {
        return this.bottomY;
    }

    public int getTopY() {
        return this.topY;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.world == null) return;
        if (this.world.isRemote) return;

        this.tickCounter++;
        if (this.refreshRequested || this.tickCounter % 40 == 0) {
            this.refreshRequested = false;
            this.refreshLine();
        }
        if (!this.hasLine) return;

        float speed = this.getSpeed();
        if (speed == 0) return;

        // Reference: distancePerTick = |speed| / 360. Sign gives direction.
        float step = speed / SPEED_DIVISOR;
        int lineLength = this.topY - this.bottomY;

        // Advance cargo along the chain.
        boolean changed = false;
        Iterator<Carried> it = this.cargo.iterator();
        while (it.hasNext()) {
            Carried c = it.next();
            c.offset += step;
            if (c.offset > lineLength) {
                this.ejectTop(c);
                it.remove();
                changed = true;
            } else if (c.offset < 0) {
                this.ejectBottom(c);
                it.remove();
                changed = true;
            } else {
                changed = true;
            }
        }

        // Carry entities standing in the column (elevator-pulley pattern).
        AxisAlignedBB column = new AxisAlignedBB(
                this.pos.getX(), this.bottomY + 0.2, this.pos.getZ(),
                this.pos.getX() + 1, this.topY + 1.0, this.pos.getZ() + 1);
        List<Entity> riders = this.world.getEntitiesWithinAABB(Entity.class, column,
                e -> e != null && e.isEntityAlive() && !(e instanceof EntityItem));
        for (Entity e : riders) {
            e.move(net.minecraft.entity.MoverType.SELF, 0, step, 0);
            e.fallDistance = 0;
        }

        // Intake pickup every few ticks.
        if (this.tickCounter % 8 == 0) {
            if (this.pickupAtIntake(speed > 0)) changed = true;
        }

        if (changed) {
            this.markDirty();
            if (this.tickCounter % 20 == 0) this.sync();
        }
    }

    /**
     * Hand-insert one stack onto the line. Returns EMPTY when fully
     * accepted, otherwise the original stack.
     */
    public ItemStack tryInsert(ItemStack stack) {
        if (this.world != null && !this.world.isRemote) {
            this.refreshLine();
        }
        if (!this.hasLine || stack.isEmpty()) return stack;
        if (this.cargo.size() >= CAPACITY) return stack;
        float at = this.getSpeed() >= 0 ? 0 : (this.topY - this.bottomY);
        this.cargo.add(new Carried(stack.copy(), at));
        this.markDirty();
        this.sync();
        return ItemStack.EMPTY;
    }

    private boolean pickupAtIntake(boolean movingUp) {
        if (this.cargo.size() >= CAPACITY) return false;
        int y = movingUp ? this.bottomY : this.topY;
        AxisAlignedBB box = new AxisAlignedBB(
                this.pos.getX() + 0.1, y + 0.1, this.pos.getZ() + 0.1,
                this.pos.getX() + 0.9, y + 1.1, this.pos.getZ() + 0.9);
        List<EntityItem> items = this.world.getEntitiesWithinAABB(EntityItem.class, box);
        if (items.isEmpty()) return false;
        EntityItem entity = items.get(0);
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) return false;
        float at = movingUp ? 0 : (this.topY - this.bottomY);
        this.cargo.add(new Carried(stack.copy(), at));
        entity.setDead();
        return true;
    }

    private void ejectTop(Carried c) {
        this.spawnDrop(this.pos.getX() + 0.5, this.topY + 1.0, this.pos.getZ() + 0.5, c.stack);
    }

    private void ejectBottom(Carried c) {
        this.spawnDrop(this.pos.getX() + 0.5, this.bottomY + 0.5, this.pos.getZ() + 0.5, c.stack);
    }

    private void spawnDrop(double x, double y, double z, ItemStack stack) {
        if (stack.isEmpty()) return;
        EntityItem entity = new EntityItem(this.world, x, y, z, stack);
        entity.motionX = 0;
        entity.motionY = 0.05;
        entity.motionZ = 0;
        entity.setDefaultPickupDelay();
        this.world.spawnEntity(entity);
    }

    /**
     * Recompute the line this endpoint belongs to. START scans up for its
     * END partner, END scans down for its START partner; every block
     * between must be a MIDDLE segment of this block.
     */
    private void refreshLine() {
        this.hasLine = false;
        if (this.world == null) return;
        IBlockState st = this.world.getBlockState(this.pos);
        if (!(st.getBlock() instanceof BlockChainConveyor)) return;
        EnumBeltPart part = st.getValue(BlockChainConveyor.PART);
        if (part == EnumBeltPart.MIDDLE) {
            if (!this.cargo.isEmpty()) this.cargo.clear();
            return;
        }
        if (part == EnumBeltPart.START) {
            BlockPos top = this.scanForPartner(1);
            if (top != null) {
                this.bottomY = this.pos.getY();
                this.topY = top.getY();
                this.hasLine = true;
            }
        } else if (part == EnumBeltPart.END) {
            BlockPos bottom = this.scanForPartner(-1);
            if (bottom != null) {
                this.bottomY = bottom.getY();
                this.topY = this.pos.getY();
                this.hasLine = true;
            }
        }
        if (!this.hasLine && !this.cargo.isEmpty()) {
            // Line dissolved: drop cargo in place instead of voiding it.
            for (Carried c : this.cargo) {
                this.spawnDrop(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, c.stack);
            }
            this.cargo.clear();
        }
    }

    private BlockPos scanForPartner(int dir) {
        World world = this.world;
        for (int i = BlockChainConveyor.MIN_LENGTH; i <= BlockChainConveyor.MAX_LENGTH; i++) {
            BlockPos p = this.pos.up(dir * i);
            if (!world.isBlockLoaded(p)) return null;
            IBlockState st = world.getBlockState(p);
            if (!(st.getBlock() instanceof BlockChainConveyor)) return null;
            EnumBeltPart q = st.getValue(BlockChainConveyor.PART);
            if (i < BlockChainConveyor.MAX_LENGTH && q == EnumBeltPart.MIDDLE) continue;
            if (q == EnumBeltPart.START || q == EnumBeltPart.END) {
                return p;
            }
            return null;
        }
        return null;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (this.hasLine) {
            return new AxisAlignedBB(this.pos.getX(), this.bottomY, this.pos.getZ(),
                    this.pos.getX() + 1, this.topY + 1, this.pos.getZ() + 1);
        }
        return super.getRenderBoundingBox();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("hasLine", this.hasLine);
        nbt.setInteger("bottomY", this.bottomY);
        nbt.setInteger("topY", this.topY);
        NBTTagList list = new NBTTagList();
        for (Carried c : this.cargo) {
            NBTTagCompound tag = c.stack.writeToNBT(new NBTTagCompound());
            tag.setFloat("off", c.offset);
            list.appendTag(tag);
        }
        nbt.setTag("cargo", list);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.hasLine = nbt.getBoolean("hasLine");
        this.bottomY = nbt.getInteger("bottomY");
        this.topY = nbt.getInteger("topY");
        this.cargo.clear();
        NBTTagList list = nbt.getTagList("cargo", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            ItemStack stack = new ItemStack(tag);
            if (!stack.isEmpty()) {
                this.cargo.add(new Carried(stack, tag.getFloat("off")));
            }
        }
        this.refreshRequested = true;
    }
}
