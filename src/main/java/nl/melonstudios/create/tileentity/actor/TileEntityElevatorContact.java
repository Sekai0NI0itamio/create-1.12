package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import nl.melonstudios.create.block.actor.BlockElevatorContact;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import java.io.IOException;

/**
 * Elevator contact TE: powers its block while any elevator cabin is docked
 * within 1 block above.
 */
public class TileEntityElevatorContact extends TileEntityOptimizedBase {
    @Override
    public void tick() {
        if (this.world == null || this.world.isRemote) return;
        if (this.world.getTotalWorldTime() % 10 != 0) return;
        boolean docked = false;
        for (Object o : this.world.loadedTileEntityList) {
            if (o instanceof TileEntityElevatorPulley) {
                TileEntityElevatorPulley p = (TileEntityElevatorPulley) o;
                if (Math.abs(p.getPos().getX() - this.pos.getX()) <= 2 && Math.abs(p.getPos().getZ() - this.pos.getZ()) <= 2
                        && Math.abs(p.currentY - this.pos.getY()) < 1.0) {
                    docked = true;
                    break;
                }
            }
        }
        try {
            if (this.world.getBlockState(this.pos).getValue(BlockElevatorContact.POWERING) != docked) {
                this.world.setBlockState(this.pos, this.world.getBlockState(this.pos)
                        .withProperty(BlockElevatorContact.POWERING, docked), 2);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void tickLazy() {
    }

    @Override
    public NBTTagCompound writePacket() {
        return new NBTTagCompound();
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        ByteBuf temp = Unpooled.buffer();
        ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(ByteBufUtils.readTag(buf));
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
