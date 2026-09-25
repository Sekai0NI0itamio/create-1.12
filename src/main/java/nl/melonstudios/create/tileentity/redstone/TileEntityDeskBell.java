package nl.melonstudios.create.tileentity.redstone;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import nl.melonstudios.create.block.redstone.BlockDeskBell;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.Utils;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Releases the bell 20 ticks after a ding, translated from the reference
 * block-state timer.
 */
public class TileEntityDeskBell extends TileEntityOptimizedBase {
    private int timer = 0;

    public void ding() {
        if (this.world == null || this.world.isRemote) return;
        this.timer = 20;
        this.sync();
        this.markDirty();
    }

    @Override
    public void tick() {
        if (this.world != null && !this.world.isRemote && this.timer > 0) {
            this.timer--;
            if (this.timer == 0) {
                IBlockState state = this.world.getBlockState(this.pos);
                if (state.getBlock() instanceof BlockDeskBell && state.getValue(BlockDeskBell.POWERED)) {
                    IBlockState off = state.withProperty(BlockDeskBell.POWERED, false);
                    Utils.setBlockTESafe(this.world, this.pos, off, 3);
                    ((BlockDeskBell) state.getBlock()).notifyNeighbors(this.world, this.pos, state);
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
        nbt.setInteger("Timer", this.timer);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.timer = Math.max(0, nbt.getInteger("Timer"));
    }

    @Override
    public NBTTagCompound writePacket() {
        return new NBTTagCompound();
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
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
