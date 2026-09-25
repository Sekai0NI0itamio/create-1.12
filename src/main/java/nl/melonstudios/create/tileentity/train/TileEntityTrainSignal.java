package nl.melonstudios.create.tileentity.train;

import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import nl.melonstudios.create.block.train.BlockTrainSignal;
import nl.melonstudios.create.entity.train.EntityTrain;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.Utils;

import java.util.List;

/**
 * Signal tile entity (foundation): refreshes the signal aspect from train
 * occupancy on its lazy tick.
 *
 * Red when a train entity occupies the section by the signal (8-block box),
 * yellow when a train is only further out (24-block box), green otherwise.
 * Aspect changes are pushed into the block state so models and redstone
 * update together.
 */
public class TileEntityTrainSignal extends TileEntityOptimizedBase {
    private static final double NEAR_RADIUS = 8.0D;
    private static final double FAR_RADIUS = 24.0D;

    public BlockTrainSignal.SignalAspect aspect = BlockTrainSignal.SignalAspect.GREEN;

    public TileEntityTrainSignal() {
        this.setTickRateLazy(10);
    }

    private boolean hasTrainWithin(double radius) {
        if (this.world == null) return false;
        List<EntityTrain> trains = this.world.getEntitiesWithinAABB(EntityTrain.class,
                new AxisAlignedBB(this.pos.getX() - radius, this.pos.getY() - 4, this.pos.getZ() - radius,
                        this.pos.getX() + 1 + radius, this.pos.getY() + 4, this.pos.getZ() + 1 + radius),
                e -> e != null && e.isEntityAlive());
        return !trains.isEmpty();
    }

    @Override
    public void tick() {
    }

    @Override
    public void tickLazy() {
        if (this.world == null || this.world.isRemote) return;
        BlockTrainSignal.SignalAspect next = this.hasTrainWithin(NEAR_RADIUS)
                ? BlockTrainSignal.SignalAspect.RED
                : this.hasTrainWithin(FAR_RADIUS)
                ? BlockTrainSignal.SignalAspect.YELLOW
                : BlockTrainSignal.SignalAspect.GREEN;
        if (next != this.aspect) {
            this.aspect = next;
            Utils.setBlockTESafe(this.world, this.pos,
                    this.world.getBlockState(this.pos).withProperty(BlockTrainSignal.SIGNAL, next), 3);
            this.sync();
        }
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("aspect", this.aspect.ordinal());
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        BlockTrainSignal.SignalAspect[] values = BlockTrainSignal.SignalAspect.values();
        int i = nbt.getInteger("aspect");
        this.aspect = values[i < 0 || i >= values.length ? 2 : i];
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
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("aspect", this.aspect.ordinal());
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        BlockTrainSignal.SignalAspect[] values = BlockTrainSignal.SignalAspect.values();
        // Missing-key guard: older worlds predate the signal aspect.
        int i = nbt.hasKey("aspect") ? nbt.getInteger("aspect") : BlockTrainSignal.SignalAspect.GREEN.ordinal();
        this.aspect = values[i < 0 || i >= values.length ? 2 : i];
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
