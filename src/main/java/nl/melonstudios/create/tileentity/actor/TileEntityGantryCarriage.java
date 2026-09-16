package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import nl.melonstudios.create.block.actor.BlockGantryCarriage;
import nl.melonstudios.create.tileentity.TileEntityKinetic;

import java.io.IOException;
import java.util.List;

/**
 * Gantry carriage: slides along its axis while powered, carrying entities
 * standing on/around it. Reverses at solid ends or after 16 blocks.
 */
public class TileEntityGantryCarriage extends TileEntityKinetic {
    public double offset;
    public int dir = 1;

    @Override
    public void tick() {
        super.tick();
        if (this.world.isRemote) return;
        if (this.getSpeed() == 0) return;
        EnumFacing.Axis axis;
        boolean backward;
        try {
            axis = this.getState().getValue(BlockGantryCarriage.AXIS);
            backward = this.getState().getValue(BlockGantryCarriage.MODE) == BlockGantryCarriage.Mode.BACKWARD;
        } catch (Exception e) {
            return;
        }
        double speed = Math.min(0.2, Math.abs(this.getSpeed()) / 256.0 * 0.2 + 0.02) * (backward ? -1 : 1) * this.dir;
        double nx = this.pos.getX() + 0.5 + (axis == EnumFacing.Axis.X ? this.offset + speed : 0);
        double nz = this.pos.getZ() + 0.5 + (axis == EnumFacing.Axis.Z ? this.offset + speed : 0);
        // Stop at solid block or after 16 blocks travel.
        net.minecraft.util.math.BlockPos ahead = new net.minecraft.util.math.BlockPos(
                (int) Math.floor(nx + (axis == EnumFacing.Axis.X ? Math.signum(speed) : 0)), this.pos.getY(),
                (int) Math.floor(nz + (axis == EnumFacing.Axis.Z ? Math.signum(speed) : 0)));
        if (this.world.getBlockState(ahead).isOpaqueCube() || Math.abs(this.offset) > 16) {
            this.dir = -this.dir;
            this.sync();
            return;
        }
        this.offset += speed;
        AxisAlignedBB cab = new AxisAlignedBB(this.pos.getX() - 1, this.pos.getY(), this.pos.getZ() - 1,
                this.pos.getX() + 2, this.pos.getY() + 2, this.pos.getZ() + 2);
        List<Entity> riders = this.world.getEntitiesWithinAABB(Entity.class, cab,
                e -> e != null && e.isEntityAlive() && !(e instanceof net.minecraft.entity.item.EntityItem));
        for (Entity e : riders) {
            if (axis == EnumFacing.Axis.X) e.moveEntity(speed, 0, 0);
            else e.moveEntity(0, 0, speed);
            e.fallDistance = 0;
        }
        this.markDirty();
        if (this.world.getTotalWorldTime() % 10 == 0) this.sync();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setDouble("offset", this.offset);
        nbt.setInteger("dir", this.dir);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.offset = nbt.getDouble("offset");
        this.dir = nbt.getInteger("dir") == 0 ? 1 : nbt.getInteger("dir");
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setDouble("offset", this.offset);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.offset = nbt.getDouble("offset");
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
        return AABB.wrap(this.pos, 4);
    }
}
