package nl.melonstudios.create.tileentity.redstone;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import nl.melonstudios.create.block.redstone.BlockAnalogLever;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Holds the 0-15 output level. Level changes apply after a 15-tick delay,
 * translated from the reference change-timer behaviour.
 */
public class TileEntityAnalogLever extends TileEntityOptimizedBase {
    private int level = 0;
    private int changeTimer = 0;

    public int getLevel() {
        return this.level;
    }

    public void changeState(boolean down) {
        int prev = this.level;
        this.level += down ? -1 : 1;
        if (this.level < 0) this.level = 0;
        if (this.level > 15) this.level = 15;
        if (prev != this.level) {
            this.changeTimer = 15;
            this.sync();
            this.markDirty();
        }
    }

    @Override
    public void tick() {
        if (this.world != null && !this.world.isRemote && this.changeTimer > 0) {
            this.changeTimer--;
            if (this.changeTimer == 0) {
                BlockPos p = this.pos;
                IBlockState state = this.world.getBlockState(p);
                if (state.getBlock() instanceof BlockAnalogLever) {
                    ((BlockAnalogLever) state.getBlock()).notifyNeighbors(this.world, p, state);
                }
            }
        }
    }

    @Override
    public void tickLazy() {
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("State", this.level);
        nbt.setInteger("ChangeTimer", this.changeTimer);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.level = Math.max(0, Math.min(15, nbt.getInteger("State")));
        this.changeTimer = Math.max(0, nbt.getInteger("ChangeTimer"));
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("State", this.level);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.level = Math.max(0, Math.min(15, nbt.getInteger("State")));
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        io.netty.buffer.ByteBuf temp = io.netty.buffer.Unpooled.buffer();
        net.minecraftforge.fml.common.network.ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(net.minecraftforge.fml.common.network.ByteBufUtils.readTag(buf));
    }
}
