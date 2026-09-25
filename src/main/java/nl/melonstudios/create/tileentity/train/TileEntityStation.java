package nl.melonstudios.create.tileentity.train;

import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.AxisAlignedBB;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Station TE: assembles bogeys + frames on the neighbouring track run into a
 * train (blocks captured into carriage data, carts spawned linked to this
 * station), or disassembles a stopped linked train back into blocks.
 * Schedule: each station adds itself; the train loops all visited stations.
 *
 * Assembly rules live in TrainAssembly; carriage snapshots persist here as
 * NBT ("Carriages") and as a copy on every spawned cart.
 */
public class TileEntityStation extends TileEntityOptimizedBase {
    public boolean assembled;
    public String statusCache = "Idle";
    private final List<TrainCarriageData> carriages = new ArrayList<>();

    public String status() {
        return this.statusCache;
    }

    public void assemble() {
        if (this.world == null || this.world.isRemote)
            return;
        if (this.assembled) {
            this.statusCache = "Already assembled";
            return;
        }
        TrainAssembly.Result result = TrainAssembly.assemble(this.world, this.pos);
        this.statusCache = result.message;
        if (result.ok) {
            this.carriages.clear();
            this.carriages.addAll(result.carriages);
            this.assembled = true;
        }
        this.sync();
    }

    public void disassemble() {
        if (this.world == null || this.world.isRemote)
            return;
        if (!this.assembled) {
            this.statusCache = "Nothing assembled";
            return;
        }
        String message = TrainAssembly.disassemble(this.world, this.pos, this.carriages);
        if (message.startsWith("Disassembled")) {
            this.carriages.clear();
            this.assembled = false;
        }
        this.statusCache = message;
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
        nbt.setTag("Carriages", TrainCarriageData.writeList(this.carriages));
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.assembled = nbt.getBoolean("assembled");
        this.statusCache = nbt.getString("status");
        this.carriages.clear();
        NBTTagList list = nbt.getTagList("Carriages", 10);
        this.carriages.addAll(TrainCarriageData.readList(list));
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 2);
    }
}
