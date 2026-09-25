package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;

/**
 * Clockwork bearing tile: behaves like the mechanical bearing (assembly state
 * machine inherited) and additionally winds two hands. Reference values: the
 * minute hand turns 12x the hour hand (12 hours per dial); both angles persist
 * in NBT and sync to clients for rendering.
 */
public class TileEntityClockworkBearing extends TileEntityBearingBase {
    /** Minutes shown on the dial; hour hand derives as minuteAngle / 12. */
    public float minuteAngle;
    public float hourAngle;
    public boolean running;

    public void setRunning(boolean running) {
        this.running = running;
        this.sync();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setFloat("MinuteAngle", this.minuteAngle);
        nbt.setFloat("HourAngle", this.hourAngle);
        nbt.setBoolean("Running", this.running);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.minuteAngle = nbt.getFloat("MinuteAngle");
        this.hourAngle = nbt.getFloat("HourAngle");
        this.running = nbt.getBoolean("Running");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeFloat(this.minuteAngle);
        buf.writeFloat(this.hourAngle);
        buf.writeBoolean(this.running);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        this.minuteAngle = buf.readFloat();
        this.hourAngle = buf.readFloat();
        this.running = buf.readBoolean();
    }
}
