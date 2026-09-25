package nl.melonstudios.create.tileentity.redstone;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import nl.melonstudios.create.block.redstone.BlockPulseDiode;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Timing core shared by the brass diode family. The reference scroll-value
 * UI (2 ticks to 60 minutes) has no backport equivalent, so right-click
 * steps the delay through a fixed preset ladder instead; the per-tick
 * countdown rules below are translated from the reference behaviours.
 */
public abstract class TileEntityBrassDiode extends TileEntityOptimizedBase {
    private static final int[] PRESETS = new int[]{2, 4, 10, 20, 40, 100, 200};

    protected int delay;
    protected int state;

    protected TileEntityBrassDiode(int defaultDelay) {
        this.delay = defaultDelay;
        this.state = 0;
    }

    public int getDelay() {
        return this.delay;
    }

    public float getProgress() {
        int max = Math.max(2, this.delay);
        return Math.max(0, Math.min(this.state, max)) / (float) max;
    }

    public boolean isIdle() {
        return this.state == 0;
    }

    /** Step to the next delay preset, wrapping around. */
    public void cycleDelay() {
        int next = PRESETS[0];
        for (int preset : PRESETS) {
            if (preset > this.delay) {
                next = preset;
                break;
            }
        }
        this.delay = next;
        this.state = Math.max(0, Math.min(this.state, next));
        this.sync();
        this.markDirty();
    }

    protected abstract void updateState(boolean powered, boolean powering, boolean atMax, boolean atMin,
                                       BlockPulseDiode block, IBlockState blockState);

    @Override
    public void tick() {
        if (this.world == null || this.world.isRemote) return;
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockPulseDiode)) return;
        BlockPulseDiode block = (BlockPulseDiode) state.getBlock();
        boolean powered = BlockPulseDiode.readBackInput(this.world, this.pos, state);
        boolean powering = state.getValue(BlockPulseDiode.POWERING);
        boolean atMax = this.state >= this.delay;
        boolean atMin = this.state <= 0;
        this.updateState(powered, powering, atMax, atMin, block, state);
    }

    @Override
    public void tickLazy() {
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("Delay", this.delay);
        nbt.setInteger("State", this.state);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.delay = Math.max(2, nbt.getInteger("Delay"));
        this.state = Math.max(0, nbt.getInteger("State"));
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("Delay", this.delay);
        nbt.setInteger("State", this.state);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.delay = Math.max(2, nbt.getInteger("Delay"));
        this.state = Math.max(0, nbt.getInteger("State"));
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        ByteBuf temp = Unpooled.buffer();
        ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(ByteBufUtils.readTag(buf));
    }
}
