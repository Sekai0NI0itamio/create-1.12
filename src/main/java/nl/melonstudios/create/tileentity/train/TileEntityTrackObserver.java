package nl.melonstudios.create.tileentity.train;

import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import nl.melonstudios.create.block.train.BlockTrackObserver;
import nl.melonstudios.create.entity.train.EntityTrain;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import java.util.List;

/**
 * Track observer TE: watches a 3x3x3 box above itself for EntityTrain or
 * vanilla minecart traffic and holds POWERED for 8 ticks after each
 * sighting, notifying neighbours on change (reference observe-and-emit).
 */
public class TileEntityTrackObserver extends TileEntityOptimizedBase {
    private int pulse;

    @Override
    public void tick() {
        if (this.world == null || this.world.isRemote) return;
        boolean seen = false;
        AxisAlignedBB box = new AxisAlignedBB(this.pos).grow(1.0, 2.0, 1.0);
        List<EntityMinecart> carts = this.world.getEntitiesWithinAABB(EntityMinecart.class, box);
        List<EntityTrain> trains = this.world.getEntitiesWithinAABB(EntityTrain.class, box);
        seen = !carts.isEmpty() || !trains.isEmpty();
        if (seen) this.pulse = 8;
        else if (this.pulse > 0) this.pulse--;

        IBlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() instanceof BlockTrackObserver) {
            boolean powered = state.getValue(BlockTrackObserver.POWERED);
            boolean want = this.pulse > 0;
            if (powered != want) {
                this.world.setBlockState(this.pos, state.withProperty(BlockTrackObserver.POWERED, want), 3);
                this.world.notifyNeighborsOfStateChange(this.pos, state.getBlock(), false);
            }
        }
    }

    @Override
    public void tickLazy() {
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("pulse", this.pulse);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.pulse = nbt.getInteger("pulse");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("pulse", this.pulse);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.pulse = nbt.getInteger("pulse");
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
