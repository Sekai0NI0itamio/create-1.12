package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import nl.melonstudios.create.tileentity.TileEntityKinetic;

import java.io.IOException;
import java.util.List;

/**
 * Elevator pulley: moves riders + the platform column smoothly between Y
 * levels. call(+1/-1) targets the next contact level up/down (contacts =
 * elevator contact blocks in the column, or every 3 blocks fallback).
 * Movement is entity-riding based (no full contraption re-assembly per stop),
 * matching the feel while staying backport-cheap.
 */
public class TileEntityElevatorPulley extends TileEntityKinetic {
    public double currentY;
    public double targetY = Double.NaN;
    public boolean assembled;

    @Override
    public void tick() {
        super.tick();
        if (this.world.isRemote) return;
        if (Double.isNaN(this.targetY)) {
            this.targetY = this.pos.getY();
            this.currentY = this.pos.getY();
            return;
        }
        if (this.getSpeed() == 0) return;
        double speed = Math.min(0.15, Math.abs(this.getSpeed()) / 256.0 * 0.15 + 0.02);
        double diff = this.targetY - this.currentY;
        if (Math.abs(diff) < 0.02) {
            this.currentY = this.targetY;
            return;
        }
        double step = Math.signum(diff) * Math.min(speed, Math.abs(diff));
        this.currentY += step;
        // Carry riders standing on the platform.
        AxisAlignedBB cab = new AxisAlignedBB(this.pos.getX(), this.currentY - 2, this.pos.getZ(),
                this.pos.getX() + 1, this.currentY + 1, this.pos.getZ() + 1);
        List<Entity> riders = this.world.getEntitiesWithinAABB(Entity.class, cab,
                e -> e != null && e.isEntityAlive() && !(e instanceof net.minecraft.entity.item.EntityItem));
        for (Entity e : riders) {
            e.moveEntity(0, step, 0);
            e.fallDistance = 0;
        }
        this.markDirty();
        if (this.world.getTotalWorldTime() % 10 == 0) this.sync();
    }

    public void call(int dir) {
        int base = (int) Math.round(this.currentY);
        int next = base + (dir > 0 ? 3 : -3);
        // Snap to nearest contact level in that direction.
        Integer contact = this.findContact(dir > 0 ? 1 : -1);
        this.targetY = contact != null ? contact : next;
        this.sync();
    }

    private Integer findContact(int dir) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(this.pos);
        for (int i = 1; i <= 24; i++) {
            p.setY(this.pos.getY() + dir * i);
            if (!this.world.isBlockLoaded(p)) break;
            String name = this.world.getBlockState(p).getBlock().getRegistryName() == null ? ""
                    : this.world.getBlockState(p).getBlock().getRegistryName().toString();
            if (name.contains("elevator") && name.contains("contact")) {
                return p.getY();
            }
        }
        return null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setDouble("cy", this.currentY);
        nbt.setDouble("ty", this.targetY);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.currentY = nbt.getDouble("cy");
        this.targetY = nbt.getDouble("ty");
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setDouble("cy", this.currentY);
        nbt.setDouble("ty", this.targetY);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.currentY = nbt.getDouble("cy");
        this.targetY = nbt.getDouble("ty");
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
