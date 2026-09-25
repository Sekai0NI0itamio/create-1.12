package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.tileentity.TileEntityKinetic;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TileEntityMechanicalArm extends TileEntityKinetic {
    public static final int RANGE = 5;
    public static final int MODE_PREFER_FIRST = 0;
    public static final int MODE_ROUND_ROBIN = 1;
    public static final int MODE_FORCED_ROUND_ROBIN = 2;

    public enum Phase {
        SEARCH_INPUTS,
        MOVE_TO_INPUT,
        SEARCH_OUTPUTS,
        MOVE_TO_OUTPUT
    }

    public Phase phase = Phase.SEARCH_INPUTS;
    public ItemStack heldItem = ItemStack.EMPTY;
    public ItemStack filterGhost = ItemStack.EMPTY;
    public int mode = MODE_PREFER_FIRST;
    public boolean redstoneLocked = false;

    public float progress = 1.0F;
    public float lastProgress = 1.0F;
    public float prevYaw = 0.0F;
    public float targetYaw = 0.0F;
    public float lastYaw = 0.0F;
    public float yaw = 0.0F;

    private BlockPos targetInput = null;
    private BlockPos targetOutput = null;
    private int lastInputIndex = -1;
    private int lastOutputIndex = -1;
    private int scanCooldown = 0;

    private final List<BlockPos> inputs = new ArrayList<>();
    private final List<BlockPos> outputs = new ArrayList<>();

    @Override
    public void tick() {
        super.tick();
        if (this.world == null) return;
        this.lastProgress = this.progress;
        this.lastYaw = this.yaw;

        if (this.world.isRemote) {
            float eased = easeInOut(this.progress);
            this.yaw = this.prevYaw + (this.targetYaw - this.prevYaw) * eased;
            return;
        }

        if (this.getSpeed() == 0.0F || this.redstoneLocked) return;

        this.progress += Math.min(256.0F, Math.abs(this.getSpeed())) / 1024.0F;
        if (this.progress > 1.0F) this.progress = 1.0F;
        float eased = easeInOut(this.progress);
        this.yaw = this.prevYaw + (this.targetYaw - this.prevYaw) * eased;
        if (this.progress < 1.0F) {
            this.markDirty();
            return;
        }

        switch (this.phase) {
            case MOVE_TO_INPUT:
                this.collectItem();
                break;
            case MOVE_TO_OUTPUT:
                this.depositItem();
                break;
            case SEARCH_INPUTS:
                this.searchForItem();
                break;
            case SEARCH_OUTPUTS:
                this.searchForDestination();
                break;
        }
        this.markDirty();
    }

    private static float easeInOut(float t) {
        if (t <= 0.0F) return 0.0F;
        if (t >= 1.0F) return 1.0F;
        return (float) ((1.0 - Math.cos(t * Math.PI)) * 0.5);
    }

    public void resetToSearch() {
        this.phase = Phase.SEARCH_INPUTS;
        this.progress = 0.0F;
        this.prevYaw = this.yaw;
        this.targetYaw = this.prevYaw;
        this.targetInput = null;
        this.targetOutput = null;
        this.sync();
    }

    public void setFilter(ItemStack stack) {
        ItemStack ghost = stack.copy();
        ghost.setCount(1);
        this.filterGhost = ghost;
        this.lastInputIndex = -1;
        this.lastOutputIndex = -1;
        this.sync();
    }

    public void cycleMode() {
        this.mode = (this.mode + 1) % 3;
        this.lastInputIndex = -1;
        this.lastOutputIndex = -1;
        this.sync();
    }

    public void redstoneUpdate() {
        if (this.world == null || this.world.isRemote) return;
        boolean powered = BlockKineticBase.isPosPowered(this.world, this.pos);
        if (powered == this.redstoneLocked) return;
        this.redstoneLocked = powered;
        if (!this.redstoneLocked && this.phase == Phase.SEARCH_INPUTS) this.searchForItem();
        this.sync();
    }

    private void refreshPoints() {
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return;
        }
        this.scanCooldown = 40;
        this.inputs.clear();
        this.outputs.clear();
        if (this.world == null) return;
        for (BlockPos p : BlockPos.getAllInBox(this.pos.add(-RANGE, -RANGE, -RANGE), this.pos.add(RANGE, RANGE, RANGE))) {
            if (p.equals(this.pos)) continue;
            if (!this.world.isBlockLoaded(p)) continue;
            TileEntity te = this.world.getTileEntity(p);
            if (te == null || te instanceof TileEntityMechanicalArm) continue;
            if (!te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
                    && !te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP)) continue;
            this.inputs.add(p.toImmutable());
            this.outputs.add(p.toImmutable());
        }
    }

    private IItemHandler handlerAt(BlockPos p) {
        TileEntity te = this.world.getTileEntity(p);
        if (te == null) return null;
        for (EnumFacing side : EnumFacing.VALUES) {
            if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) {
                IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
                if (handler != null) return handler;
            }
        }
        if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        }
        return null;
    }

    private boolean matchesFilter(ItemStack stack) {
        if (this.filterGhost.isEmpty()) return true;
        return ItemStack.areItemsEqual(this.filterGhost, stack)
                && ItemStack.areItemStackTagsEqual(this.filterGhost, stack);
    }

    private int distributableAt(BlockPos p) {
        IItemHandler handler = this.handlerAt(p);
        if (handler == null) return 0;
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack peek = handler.extractItem(slot, 64, true);
            if (peek.isEmpty() || !this.matchesFilter(peek)) continue;
            if (!this.canPlaceAnywhere(peek)) continue;
            total += peek.getCount();
        }
        return total;
    }

    private boolean canPlaceAnywhere(ItemStack stack) {
        ItemStack probe = stack.copy();
        for (BlockPos p : this.outputs) {
            IItemHandler handler = this.handlerAt(p);
            if (handler == null) continue;
            probe = this.tryInsert(handler, probe, true);
            if (probe.isEmpty()) return true;
        }
        return false;
    }

    private static ItemStack tryInsert(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (remainder.isEmpty()) break;
            if (!handler.isItemValid(slot, remainder)) continue;
            remainder = handler.insertItem(slot, remainder, simulate);
        }
        return remainder;
    }

    protected void searchForItem() {
        this.refreshPoints();
        if (this.redstoneLocked) return;
        int start = this.mode == MODE_PREFER_FIRST ? 0 : this.lastInputIndex + 1;
        int scanRange = this.mode == MODE_FORCED_ROUND_ROBIN
                ? Math.min(this.lastInputIndex + 2, this.inputs.size())
                : this.inputs.size();
        for (int i = start; i < scanRange; i++) {
            if (i < 0 || i >= this.inputs.size()) continue;
            BlockPos p = this.inputs.get(i);
            if (this.handlerAt(p) == null) continue;
            if (this.distributableAt(p) == 0) continue;
            this.selectInput(p, i);
            return;
        }
        if (this.mode == MODE_ROUND_ROBIN) this.lastInputIndex = -1;
        if (!this.inputs.isEmpty() && this.lastInputIndex >= this.inputs.size() - 1) this.lastInputIndex = -1;
    }

    protected void searchForDestination() {
        this.refreshPoints();
        ItemStack held = this.heldItem.copy();
        if (held.isEmpty()) {
            this.phase = Phase.SEARCH_INPUTS;
            return;
        }
        int start = this.mode == MODE_PREFER_FIRST ? 0 : this.lastOutputIndex + 1;
        int scanRange = this.mode == MODE_FORCED_ROUND_ROBIN
                ? Math.min(this.lastOutputIndex + 2, this.outputs.size())
                : this.outputs.size();
        for (int i = start; i < scanRange; i++) {
            if (i < 0 || i >= this.outputs.size()) continue;
            BlockPos p = this.outputs.get(i);
            IItemHandler handler = this.handlerAt(p);
            if (handler == null) continue;
            ItemStack remainder = tryInsert(handler, held, true);
            if (remainder.getCount() >= held.getCount()
                    && ItemHandlerHelper.canItemStacksStack(remainder, held)) continue;
            this.selectOutput(p, i);
            return;
        }
        if (this.mode == MODE_ROUND_ROBIN) this.lastOutputIndex = -1;
        if (!this.outputs.isEmpty() && this.lastOutputIndex >= this.outputs.size() - 1) this.lastOutputIndex = -1;
        if (this.phase == Phase.SEARCH_OUTPUTS) {
            this.phase = Phase.SEARCH_INPUTS;
            this.progress = 0.0F;
            this.sync();
        }
    }

    private float yawTowards(BlockPos p) {
        double dx = (p.getX() + 0.5) - (this.pos.getX() + 0.5);
        double dz = (p.getZ() + 0.5) - (this.pos.getZ() + 0.5);
        if (dx * dx + dz * dz < 1.0e-4) return this.yaw;
        return (float) Math.toDegrees(Math.atan2(dx, dz));
    }

    private void selectInput(BlockPos p, int index) {
        this.phase = Phase.MOVE_TO_INPUT;
        this.targetInput = p;
        this.lastInputIndex = index;
        this.prevYaw = this.yaw;
        this.targetYaw = this.yawTowards(p);
        this.progress = 0.0F;
        this.sync();
        this.markDirty();
    }

    private void selectOutput(BlockPos p, int index) {
        this.phase = Phase.MOVE_TO_OUTPUT;
        this.targetOutput = p;
        this.lastOutputIndex = index;
        this.prevYaw = this.yaw;
        this.targetYaw = this.yawTowards(p);
        this.progress = 0.0F;
        this.sync();
        this.markDirty();
    }

    protected void collectItem() {
        if (this.targetInput != null) {
            IItemHandler handler = this.handlerAt(this.targetInput);
            if (handler != null) {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack peek = handler.extractItem(slot, 64, true);
                    if (peek.isEmpty() || !this.matchesFilter(peek)) continue;
                    if (!this.canPlaceAnywhere(peek)) continue;
                    this.heldItem = handler.extractItem(slot, peek.getCount(), false);
                    this.phase = Phase.SEARCH_OUTPUTS;
                    this.progress = 0.0F;
                    this.prevYaw = this.yaw;
                    this.targetInput = null;
                    this.sync();
                    this.markDirty();
                    return;
                }
            }
        }
        this.phase = Phase.SEARCH_INPUTS;
        this.progress = 0.0F;
        this.prevYaw = this.yaw;
        this.targetInput = null;
        this.sync();
        this.markDirty();
    }

    protected void depositItem() {
        if (this.targetOutput != null && !this.heldItem.isEmpty()) {
            IItemHandler handler = this.handlerAt(this.targetOutput);
            if (handler != null) {
                this.heldItem = tryInsert(handler, this.heldItem, false);
            }
        }
        this.phase = this.heldItem.isEmpty() ? Phase.SEARCH_INPUTS : Phase.SEARCH_OUTPUTS;
        this.progress = 0.0F;
        this.prevYaw = this.yaw;
        this.targetOutput = null;
        this.sync();
        this.markDirty();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (this.world != null && !this.world.isRemote && !this.heldItem.isEmpty()) {
            StackUtil.dropItemsAt(this.world, this.pos, this.heldItem);
            this.heldItem = ItemStack.EMPTY;
        }
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, RANGE + 1);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("Phase", this.phase.ordinal());
        nbt.setFloat("Progress", this.progress);
        nbt.setFloat("Yaw", this.yaw);
        nbt.setFloat("PrevYaw", this.prevYaw);
        nbt.setFloat("TargetYaw", this.targetYaw);
        nbt.setInteger("Mode", this.mode);
        nbt.setBoolean("Locked", this.redstoneLocked);
        nbt.setInteger("LastIn", this.lastInputIndex);
        nbt.setInteger("LastOut", this.lastOutputIndex);
        if (!this.heldItem.isEmpty()) nbt.setTag("Held", this.heldItem.writeToNBT(new NBTTagCompound()));
        if (!this.filterGhost.isEmpty()) nbt.setTag("Filter", this.filterGhost.writeToNBT(new NBTTagCompound()));
        if (this.targetInput != null) nbt.setLong("TargetIn", this.targetInput.toLong());
        if (this.targetOutput != null) nbt.setLong("TargetOut", this.targetOutput.toLong());
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        int phaseId = nbt.getInteger("Phase");
        Phase[] phases = Phase.values();
        this.phase = phases[Math.max(0, Math.min(phaseId, phases.length - 1))];
        this.progress = nbt.getFloat("Progress");
        this.lastProgress = this.progress;
        this.yaw = nbt.getFloat("Yaw");
        this.lastYaw = this.yaw;
        this.prevYaw = nbt.getFloat("PrevYaw");
        this.targetYaw = nbt.getFloat("TargetYaw");
        this.mode = nbt.getInteger("Mode") % 3;
        this.redstoneLocked = nbt.getBoolean("Locked");
        this.lastInputIndex = nbt.getInteger("LastIn");
        this.lastOutputIndex = nbt.getInteger("LastOut");
        if (nbt.hasKey("Held", 10)) {
            this.heldItem = new ItemStack(nbt.getCompoundTag("Held"));
        } else {
            this.heldItem = ItemStack.EMPTY;
        }
        if (nbt.hasKey("Filter", 10)) {
            this.filterGhost = new ItemStack(nbt.getCompoundTag("Filter"));
        } else {
            this.filterGhost = ItemStack.EMPTY;
        }
        if (nbt.hasKey("TargetIn")) {
            this.targetInput = BlockPos.fromLong(nbt.getLong("TargetIn"));
        } else {
            this.targetInput = null;
        }
        if (nbt.hasKey("TargetOut")) {
            this.targetOutput = BlockPos.fromLong(nbt.getLong("TargetOut"));
        } else {
            this.targetOutput = null;
        }
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeInt(this.phase.ordinal());
        buf.writeFloat(this.progress);
        buf.writeFloat(this.yaw);
        buf.writeFloat(this.prevYaw);
        buf.writeFloat(this.targetYaw);
        buf.writeInt(this.mode);
        buf.writeBoolean(this.redstoneLocked);
        if (!this.heldItem.isEmpty()) {
            buf.writeBoolean(true);
            StackUtil.writeItemStack(this.heldItem, buf, true, true);
        } else {
            buf.writeBoolean(false);
        }
        if (!this.filterGhost.isEmpty()) {
            buf.writeBoolean(true);
            StackUtil.writeItemStack(this.filterGhost, buf, true, true);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        int phaseId = buf.readInt();
        Phase[] phases = Phase.values();
        this.phase = phases[Math.max(0, Math.min(phaseId, phases.length - 1))];
        this.progress = buf.readFloat();
        this.yaw = buf.readFloat();
        this.prevYaw = buf.readFloat();
        this.targetYaw = buf.readFloat();
        this.mode = buf.readInt() % 3;
        this.redstoneLocked = buf.readBoolean();
        if (buf.readBoolean()) {
            this.heldItem = StackUtil.readItemStack(buf, true, true);
        } else {
            this.heldItem = ItemStack.EMPTY;
        }
        if (buf.readBoolean()) {
            this.filterGhost = StackUtil.readItemStack(buf, true, true);
        } else {
            this.filterGhost = ItemStack.EMPTY;
        }
    }
}
