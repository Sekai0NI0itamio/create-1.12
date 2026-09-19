package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import nl.melonstudios.create.kinetics.KineticPropagator;
import nl.melonstudios.create.tileentity.TileEntitySplitShaftBase;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Sequenced gearshift: each redstone pulse advances one instruction —
 * rotate by the step angle (90°/180°/360° by mode), then pause. Acts as a
 * clutch between steps (output held while waiting for the next pulse).
 */
public class TileEntitySequencedGearshift extends TileEntitySplitShaftBase {
    public int stepMode = 0;
    public int stepIndex = 0;
    public float heldAngle;
    public boolean holding = true;

    public void onPulse() {
        if (this.world != null && !this.world.isRemote) {
            this.holding = false;
            this.stepIndex++;
            this.sync();
        }
    }

    public void reset() {
        this.stepIndex = 0;
        this.holding = true;
        this.heldAngle = 0;
        if (this.world != null && !this.world.isRemote) {
            KineticPropagator.handleRemoved(this.world, this.pos, this);
        }
        this.sync();
    }

    public float stepAngle() {
        switch (this.stepMode) {
            case 1: return 180.0F;
            case 2: return 360.0F;
            default: return 90.0F;
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Between pulses the shaft is clutched (no motion transmitted).
        // Motion resumes for exactly one step after each pulse.
        if (!this.holding && this.getSpeed() != 0) {
            this.heldAngle += Math.abs(this.getSpeed()) * 0.9F;
            if (this.heldAngle >= this.stepAngle() * 4) {
                this.heldAngle = 0;
                this.holding = true;
                if (this.world != null && !this.world.isRemote) {
                    KineticPropagator.handleRemoved(this.world, this.pos, this);
                }
                this.sync();
            }
            this.markDirty();
        }
    }

    @Override
    public float getRotationSpeedModifier(EnumFacing side) {
        if (this.hasSource()) {
            if (side != this.getSourceFacing() && this.holding) return 0.0F;
        }
        return 1.0F;
    }

    @Override
    public float getSpeed() {
        if (this.holding) return 0.0F;
        return super.getSpeed();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("stepMode", this.stepMode);
        nbt.setInteger("stepIndex", this.stepIndex);
        nbt.setBoolean("holding", this.holding);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.stepMode = nbt.getInteger("stepMode") % 3;
        this.stepIndex = nbt.getInteger("stepIndex");
        this.holding = !nbt.hasKey("holding") || nbt.getBoolean("holding");
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeByte(this.stepMode);
        buf.writeInt(this.stepIndex);
        buf.writeBoolean(this.holding);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        this.stepMode = buf.readUnsignedByte() % 3;
        this.stepIndex = buf.readInt();
        this.holding = buf.readBoolean();
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
