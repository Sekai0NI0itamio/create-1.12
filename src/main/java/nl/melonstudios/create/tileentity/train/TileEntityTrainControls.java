package nl.melonstudios.create.tileentity.train;

import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

/**
 * Train controls TE: throttle lever state (-100..100). The assembled train
 * reads this as target speed each tick (station links the nearest controls
 * panel when assembling; NEEDS-LEAD: station link line).
 */
public class TileEntityTrainControls extends TileEntityOptimizedBase {
    private int throttle;

    public int getThrottle() {
        return this.throttle;
    }

    public void setThrottle(int value) {
        this.throttle = Math.max(-100, Math.min(100, value));
    }

    public String status() {
        if (this.throttle == 0) return "Throttle: idle";
        return "Throttle: " + (this.throttle > 0 ? "+" : "") + this.throttle;
    }

    @Override
    public void tick() {
    }

    @Override
    public void tickLazy() {
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("throttle", this.throttle);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.setThrottle(nbt.getInteger("throttle"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("throttle", this.throttle);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.setThrottle(nbt.getInteger("throttle"));
    }

    @Override
    public void writePacket(com.melonstudios.melonlib.network.TrackedByteBuf buf) throws java.io.IOException {
        io.netty.buffer.ByteBuf temp = io.netty.buffer.Unpooled.buffer();
        net.minecraftforge.fml.common.network.ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @Override
    public void readPacket(io.netty.buffer.ByteBuf buf) throws java.io.IOException {
        this.readPacket(net.minecraftforge.fml.common.network.ByteBufUtils.readTag(buf));
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
