package nl.melonstudios.create.tileentity.train;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import nl.melonstudios.create.entity.train.EntityTrain;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Station TE: assembles bogeys + frames in a 3x3x3 above into a train
 * (frames stored, carts spawned), or disassembles back. Schedule: each
 * station adds itself; the train loops all visited stations.
 */
public class TileEntityStation extends TileEntityOptimizedBase {
    public boolean assembled;
    public String statusCache = "Idle";

    public String status() {
        return this.statusCache;
    }

    public void assemble() {
        if (this.assembled) {
            this.statusCache = "Already assembled";
            return;
        }
        List<BlockPos> bogeys = new ArrayList<>();
        List<FrameBlock> frames = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = this.pos.add(dx, dy, dz);
                    if (!this.world.isBlockLoaded(p)) continue;
                    IBlockState s = this.world.getBlockState(p);
                    String name = s.getBlock().getRegistryName() == null ? "" : s.getBlock().getRegistryName().toString();
                    if (name.contains("bogey")) {
                        bogeys.add(p.toImmutable());
                    } else if (!s.getBlock().isAir(s, this.world, p) && s.getBlockHardness(this.world, p) >= 0
                            && !(s.getBlock() instanceof net.minecraft.block.BlockRailBase)) {
                        TileEntity te = this.world.getTileEntity(p);
                        NBTTagCompound tenbt = null;
                        if (te != null) {
                            tenbt = new NBTTagCompound();
                            te.writeToNBT(tenbt);
                        }
                        @SuppressWarnings("deprecation")
                        int meta = s.getBlock().getMetaFromState(s);
                        frames.add(new FrameBlock(p.toImmutable(), s.getBlock().getRegistryName().toString(), meta, tenbt));
                    }
                }
            }
        }
        if (bogeys.isEmpty()) {
            this.statusCache = "No bogeys above";
            return;
        }
        // Clear frames into storage, spawn one cart per bogey.
        for (FrameBlock f : frames) {
            this.world.setBlockToAir(f.pos);
        }
        EntityTrain lead = null;
        for (BlockPos b : bogeys) {
            EntityTrain cart = new EntityTrain(this.world);
            cart.setPosition(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
            cart.addStop(this.pos);
            this.world.spawnEntity(cart);
            if (lead == null) lead = cart;
        }
        if (lead != null) {
            // Persist frames on the lead cart.
            NBTTagCompound tag = new NBTTagCompound();
            NBTTagList list = new NBTTagList();
            for (FrameBlock f : frames) {
                NBTTagCompound e = new NBTTagCompound();
                e.setLong("Pos", f.pos.toLong());
                e.setString("Block", f.block);
                e.setInteger("Meta", f.meta);
                if (f.te != null) e.setTag("TE", f.te);
                list.appendTag(e);
            }
            tag.setTag("Frames", list);
            lead.getEntityData().setTag("CreateFrames", tag);
            lead.start();
        }
        this.assembled = true;
        this.statusCache = "Assembled " + bogeys.size() + " bogey(s), " + frames.size() + " frame blocks";
        this.sync();
    }

    public void disassemble() {
        if (!this.assembled) {
            this.statusCache = "Nothing assembled";
            return;
        }
        List<EntityTrain> trains = this.world.getEntitiesWithinAABB(EntityTrain.class,
                new AxisAlignedBB(this.pos).grow(16), e -> e != null && e.isEntityAlive());
        for (EntityTrain t : trains) {
            NBTTagCompound tag = t.getEntityData().getCompoundTag("CreateFrames");
            NBTTagList list = tag.getTagList("Frames", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound e = list.getCompoundTagAt(i);
                BlockPos p = BlockPos.fromLong(e.getLong("Pos"));
                if (!this.world.isBlockLoaded(p)) continue;
                Block b = Block.getBlockFromName(e.getString("Block"));
                if (b == null) continue;
                @SuppressWarnings("deprecation")
                IBlockState s = b.getStateFromMeta(e.getInteger("Meta"));
                this.world.setBlockState(p, s, 2);
                TileEntity te = this.world.getTileEntity(p);
                if (te != null && e.hasKey("TE", 10)) {
                    try {
                        te.readFromNBT(e.getCompoundTag("TE"));
                    } catch (Exception ignored) {
                    }
                }
            }
            // Drop carts as minecart items.
            StackUtil.spawnItemNoVelocity(this.world, t.posX, t.posY, t.posZ,
                    new ItemStack(net.minecraft.init.Items.MINECART));
            t.setDead();
        }
        this.assembled = false;
        this.statusCache = "Disassembled";
        this.sync();
    }

    @Override
    public void tick() {
    }

    @Override
    public void tickLazy() {
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("assembled", this.assembled);
        nbt.setString("status", this.statusCache);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.assembled = nbt.getBoolean("assembled");
        this.statusCache = nbt.getString("status");
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
        nbt.setBoolean("assembled", this.assembled);
        nbt.setString("status", this.statusCache);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.assembled = nbt.getBoolean("assembled");
        this.statusCache = nbt.getString("status");
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 2);
    }

    private static class FrameBlock {
        final BlockPos pos;
        final String block;
        final int meta;
        final NBTTagCompound te;

        FrameBlock(BlockPos pos, String block, int meta, NBTTagCompound te) {
            this.pos = pos;
            this.block = block;
            this.meta = meta;
            this.te = te;
        }
    }
}
