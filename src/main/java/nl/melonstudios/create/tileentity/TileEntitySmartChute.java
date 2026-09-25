package nl.melonstudios.create.tileentity;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.block.BlockSmartChute;
import nl.melonstudios.create.util.filter.IItemFilter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;

/**
 * Smart chute tile: the base chute loop plus reference filter rules.
 *
 * <p>Translated from the reference SmartChuteBlockEntity (never pasted):</p>
 * <ul>
 * <li>intake only runs while unpowered ({@code canActivate} reads the
 * block's POWERED flag);</li>
 * <li>only stacks matching the ghost filter are pulled — empty filter
 * means everything, like the reference unset filter;</li>
 * <li>pull size is the dialled amount when a filter with a count is set
 * (reference {@code filtering.getAmount()}), otherwise the plain-chute
 * 16 per operation;</li>
 * <li>exact mode demands the full amount be present before anything moves
 * (reference {@code EXACTLY}), up-to mode takes what is there
 * (reference {@code UPTO}).</li>
 * </ul>
 *
 * <p>Downward output is unchanged from the parent: whatever sits in the
 * slot keeps flowing down.</p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TileEntitySmartChute extends TileEntityChute {
    /** Ghost filter; null accepts everything. */
    public IItemFilter filter = null;
    /** Dialled pull size, 1..64 (reference filter count). */
    public int extractionAmount = 64;
    /** True = take exactly the dialled amount or nothing. */
    public boolean extractionExact = false;

    /**
     * Per-tick transit flag for smart-to-smart pacing. The base chute keeps
     * an equivalent private flag; tile files outside this block are
     * read-only for this port, so smart chutes track their own flag here.
     * All moves still go through real extract/insert calls, so bypassing
     * the base flag only changes pacing, never conservation.
     */
    private boolean smartLocked = false;

    public boolean isSmartLocked() {
        return this.smartLocked;
    }

    public TileEntitySmartChute() {
        super();
    }

    /** Reference {@code canActivate}: idle while the block reads powered. */
    protected boolean canActivate() {
        if (this.world == null) return true;
        IBlockState state = this.world.getBlockState(this.pos);
        return !(state.getBlock() instanceof BlockSmartChute) || !state.getValue(BlockSmartChute.POWERED);
    }

    /** Reference {@code canAcceptItem} + filter test for the intake side. */
    protected boolean canPullStack(ItemStack stack) {
        return !stack.isEmpty() && (this.filter == null || this.filter.matches(stack));
    }

    /** Reference {@code getExtractionAmount}: dialled count, else 16.
     * Clamped to the chute slot limit (16); amounts above that behave as
     * 16, matching the plain-chute operation size. */
    protected int pullSize() {
        if (this.filter != null) return MathHelper.clamp(this.extractionAmount, 1, 16);
        return 16;
    }

    /**
     * Step the dialled amount (backport sneak-click idiom, cf. the speed
     * controller): halves 64 all the way down to 1, then wraps to 64.
     */
    public void cycleAmount() {
        if (this.extractionAmount <= 1) this.extractionAmount = 64;
        else this.extractionAmount = Math.max(1, this.extractionAmount / 2);
        this.sync();
    }

    @Override
    public void tickLazy() {
        boolean mod = false;
        // Push down first: output never needs power or a filter match.
        if (!this.stack.isEmpty()) {
            IItemHandler below = this.getSmartInv(this.pos.down(), EnumFacing.UP);
            if (below != null) {
                for (int i = 0; i < below.getSlots(); i++) {
                    ItemStack ret = below.insertItem(i, this.stack, false);
                    if (ret != this.stack) {
                        mod = true;
                        this.stack = ret;
                    }
                    if (this.stack.isEmpty()) break;
                }
            } else if (!this.world.isRemote) {
                IBlockState downState = this.world.getBlockState(this.pos.down());
                if (downState.getBlock().isAir(downState, this.world, this.pos.down())
                        || downState.getBlock().isReplaceable(this.world, this.pos.down())) {
                    EntityItem entity = new EntityItem(this.world,
                            this.pos.getX() + 0.5D, this.pos.getY() - 0.7D, this.pos.getZ() + 0.5D,
                            this.stack);
                    entity.motionX = 0;
                    entity.motionY = 0;
                    entity.motionZ = 0;
                    this.world.spawnEntity(entity);
                    this.stack = ItemStack.EMPTY;
                    mod = true;
                }
            }
        }

        this.smartLocked = false;

        // Filtered, power-gated intake from above.
        if (this.stack.isEmpty() && this.canActivate()) {
            IItemHandler above = this.getSmartInv(this.pos.up(), EnumFacing.DOWN);
            if (above != null) {
                int want = this.pullSize();
                for (int i = 0; i < above.getSlots(); i++) {
                    ItemStack probe = above.extractItem(i, want, true);
                    if (!this.canPullStack(probe)) continue;
                    if (this.extractionExact && this.filter != null && probe.getCount() < want) continue;
                    this.stack = above.extractItem(i, want, false);
                    if (!this.stack.isEmpty()) {
                        this.smartLocked = true;
                        mod = true;
                        break;
                    }
                }
            }
        }

        if (mod) this.sync();
    }

    //region capability access mirrors the parent's lookup (see flag note above)
    private IItemHandler getSmartInv(BlockPos pos, EnumFacing side) {
        TileEntity te = this.world.getTileEntity(pos);
        if (te == null || !te.hasCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side))
            return null;
        if (te instanceof TileEntitySmartChute && ((TileEntitySmartChute) te).isSmartLocked()) return null;
        return te.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
    }
    //endregion

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!this.canPullStack(stack)) return stack;
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack tryInsertItem(ItemStack stack) {
        if (!this.canPullStack(stack)) return stack;
        return super.tryInsertItem(stack);
    }

    @Override
    public boolean isInsertionSlotEmpty(ItemStack stack) {
        return super.isInsertionSlotEmpty(stack) && this.canPullStack(stack);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (this.filter != null) nbt.setTag("Filter", this.filter.serialize(new NBTTagCompound()));
        nbt.setInteger("Extraction", this.extractionAmount);
        nbt.setBoolean("ExtractionExact", this.extractionExact);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("Filter", 10)) {
            this.filter = IItemFilter.deserialize(nbt.getCompoundTag("Filter"));
        } else this.filter = null;
        if (nbt.hasKey("Extraction", 3)) {
            this.extractionAmount = MathHelper.clamp(nbt.getInteger("Extraction"), 1, 64);
        }
        this.extractionExact = nbt.getBoolean("ExtractionExact");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        if (this.filter != null) {
            buf.writeBoolean(true);
            this.filter.serialize(buf);
        } else buf.writeBoolean(false);
        buf.writeByte(this.extractionAmount);
        buf.writeBoolean(this.extractionExact);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        if (buf.readBoolean()) {
            this.filter = IItemFilter.deserialize(buf);
        } else this.filter = null;
        this.extractionAmount = MathHelper.clamp(buf.readUnsignedByte(), 1, 64);
        this.extractionExact = buf.readBoolean();
    }
}
