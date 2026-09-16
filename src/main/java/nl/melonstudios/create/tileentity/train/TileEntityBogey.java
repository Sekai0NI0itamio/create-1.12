package nl.melonstudios.create.tileentity.train;

import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

/**
 * Bogey TE: spins with network speed; assembles with the station into trains.
 */
public class TileEntityBogey extends TileEntityOptimizedBase {
    public float wheelAngle;

    @Override
    public void tick() {
        if (this.world != null && !this.world.isRemote) {
            // Wheel angle driven by nearby train speed; idle spin when powered kinetically.
            this.wheelAngle += 2.0F;
            if (this.world.getTotalWorldTime() % 20 == 0) this.sync();
        }
    }

    @Override
    public void tickLazy() {
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setFloat("angle", this.wheelAngle);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.wheelAngle = nbt.getFloat("angle");
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
