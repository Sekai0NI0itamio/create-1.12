package nl.melonstudios.create.tileentity.generator;

import com.melonstudios.melonlib.misc.BlockStateProperties;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.generator.BlockCreativeMotor;
import nl.melonstudios.create.tileentity.TileEntityKineticGeneratorBase;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

public class TileEntityCreativeMotor extends TileEntityKineticGeneratorBase {
    private static final float[] SPEEDS = {-256, -128, -64, -32, -16, -8, -4, -2, -1, 1, 2, 4, 8, 16, 32, 64, 128, 256};
    public int speedIndex = 13;

    private static int clampIndex(int index) {
        return Math.max(0, Math.min(SPEEDS.length - 1, index));
    }

    public void cycleSpeedIndex(int delta) {
        this.speedIndex = clampIndex(this.speedIndex + delta);
    }

    public float getSelectedSpeed() {
        return SPEEDS[clampIndex(this.speedIndex)];
    }

    public TileEntityCreativeMotor() {
        super();
    }

    @Override
    public float getGeneratedSpeed() {
        float speed = this.getSelectedSpeed();
        if (this.world == null) return speed;
        // Reference returns 0 once the block is no longer a motor (tearing down).
        if (!(this.getBlockType() instanceof BlockCreativeMotor)) return 0.0F;
        // Reference folds the facing into the sign, like the hand crank backport.
        return convertToDirection(speed, this.getState().getValue(BlockStateProperties.FACING));
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

        if (nbt.hasKey("speedIndex")) this.speedIndex = clampIndex(nbt.getInteger("speedIndex"));
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
        this.speedIndex = clampIndex(buf.readUnsignedByte());
    }

    @Override
    public void initialize() {
        super.initialize();
        //this.updateGeneratedRotation();
    }

    @SideOnly(Side.CLIENT)
    public EnumFacing getRenderFacing() {
        return this.getState().getValue(BlockStateProperties.FACING);
    }
}
