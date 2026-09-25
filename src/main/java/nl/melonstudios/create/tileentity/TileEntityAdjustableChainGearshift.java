package nl.melonstudios.create.tileentity;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;

/**
 * Adjustable chain gearshift (translated from the reference
 * ChainGearshiftBlockEntity, MIT). A chain drive whose bridge ratio follows
 * the analog redstone signal: unpowered 1:1, otherwise
 * 1 + (signal + 1) / 16, up to 2:1 at signal 15.
 *
 * The ratio is applied in propagateRotationTo (chain bridges and
 * axis-adjacent shafts alike) because the kinetic propagator consults the
 * custom connection first; the plain 1.0 chain relay is scaled by the
 * current modifier. Signal polling mirrors the reference: neighbor changes
 * set a flag, the per-tick pass applies it with a detach/reattach so the
 * network rebuilds at the new ratio.
 */
public class TileEntityAdjustableChainGearshift extends TileEntityChainDrive {
    public int signal;
    private boolean signalChanged;

    public TileEntityAdjustableChainGearshift() {
        super();
        this.signal = 0;
        this.signalChanged = false;
        this.setTickRateLazy(40);
    }

    public float getModifier() {
        return getModifierForSignal(this.signal);
    }

    public static float getModifierForSignal(int power) {
        if (power == 0) return 1.0F;
        return 1.0F + (power + 1) / 16.0F;
    }

    /** Called by the block on neighbor changes and by the lazy tick. */
    public void pollSignal() {
        if (this.world == null || this.world.isRemote) return;
        int power = this.world.getRedstonePowerFromNeighbors(this.pos);
        if (power != this.signal) this.signalChanged = true;
    }

    @Override
    public void tickLazy() {
        super.tickLazy();
        this.pollSignal();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.world == null || this.world.isRemote) return;
        if (this.signalChanged) {
            this.signalChanged = false;
            this.analogSignalChanged(this.world.getRedstonePowerFromNeighbors(this.pos));
        }
    }

    protected void analogSignalChanged(int newSignal) {
        this.detachKinetics();
        this.removeSource();
        this.signal = newSignal;
        this.attachKinetics();
        this.markDirty();
    }

    @Override
    public float propagateRotationTo(TileEntityKinetic target,
                                     IBlockState stateFrom, IBlockState stateTo, BlockPos diff,
                                     boolean connectedViaAxes, boolean connectedViaCogs) {
        float chain = super.propagateRotationTo(target, stateFrom, stateTo, diff,
                connectedViaAxes, connectedViaCogs);
        if (chain != 0.0F) return chain * this.getModifier();
        if (connectedViaAxes) return this.getModifier();
        return 0.0F;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.signal = compound.getInteger("Signal");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setInteger("Signal", this.signal);

        return compound;
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeInt(this.signal);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        this.signal = buf.readInt();
    }
}
