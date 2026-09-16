package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import nl.melonstudios.create.tileentity.TileEntityKineticGeneratorBase;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Speed controller generator: outputs the configured RPM when powered by any
 * adjacent large cogwheel (input speed irrelevant, official behavior).
 */
public class TileEntitySpeedController extends TileEntityKineticGeneratorBase {
    public static final float[] SPEEDS = {-256, -128, -64, -32, -16, -8, -4, -2, -1, 1, 2, 4, 8, 16, 32, 64, 128, 256};
    public int speedIndex = 13;

    @Override
    public float getGeneratedSpeed() {
        if (!this.hasCogwheelInput()) return 0.0F;
        return SPEEDS[this.speedIndex];
    }

    @Override
    public float calculateCapacity() {
        return 0.0F;
    }

    @Override
    public float calculateImpact() {
        return 0.0F;
    }

    private boolean hasCogwheelInput() {
        if (this.world == null) return false;
        for (EnumFacing f : EnumFacing.HORIZONTALS) {
            BlockPos p = this.pos.offset(f);
            if (!this.world.isBlockLoaded(p)) continue;
            Block b = this.world.getBlockState(p).getBlock();
            String name = b.getRegistryName() == null ? "" : b.getRegistryName().toString();
            if (name.contains("cog")) return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.world != null && !this.world.isRemote && this.world.getTotalWorldTime() % 20 == 0) {
            this.updateGeneratedRotation();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("speedIndex", this.speedIndex);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.speedIndex = Math.max(0, Math.min(SPEEDS.length - 1, nbt.getInteger("speedIndex")));
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeByte(this.speedIndex);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        this.speedIndex = buf.readUnsignedByte() % SPEEDS.length;
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
