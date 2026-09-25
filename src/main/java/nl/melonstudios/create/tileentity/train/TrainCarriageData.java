package nl.melonstudios.create.tileentity.train;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Captured blocks of one assembled carriage.
 *
 * A carriage owns one or two bogey anchors plus every frame block flooded
 * from them at assembly time. Positions are stored absolute so disassembly
 * can put each block back where it came from. Block identity is kept as
 * registry name + metadata (1.12 idiom) with an optional tile-entity copy.
 */
public class TrainCarriageData {
    /** Absolute anchor positions of this carriage's bogeys (1-2). */
    public final List<BlockPos> bogeys = new ArrayList<>();
    /** Captured blocks, bogeys included. */
    public final List<Entry> blocks = new ArrayList<>();

    /** One captured block, stored by absolute position. */
    public static class Entry {
        public final BlockPos pos;
        public final String block;
        public final int meta;
        /** Copy of the tile entity NBT, or null for plain blocks. */
        public final NBTTagCompound te;

        public Entry(BlockPos pos, String block, int meta, NBTTagCompound te) {
            this.pos = pos;
            this.block = block;
            this.meta = meta;
            this.te = te;
        }
    }

    public int blockCount() {
        return this.blocks.size();
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList bogeyList = new NBTTagList();
        for (BlockPos b : this.bogeys) {
            NBTTagCompound e = new NBTTagCompound();
            e.setLong("Pos", b.toLong());
            bogeyList.appendTag(e);
        }
        nbt.setTag("Bogeys", bogeyList);

        NBTTagList blockList = new NBTTagList();
        for (Entry e : this.blocks) {
            NBTTagCompound c = new NBTTagCompound();
            c.setLong("Pos", e.pos.toLong());
            c.setString("Block", e.block);
            c.setInteger("Meta", e.meta);
            if (e.te != null)
                c.setTag("TE", e.te);
            blockList.appendTag(c);
        }
        nbt.setTag("Blocks", blockList);
        return nbt;
    }

    public static TrainCarriageData readFromNBT(NBTTagCompound nbt) {
        TrainCarriageData data = new TrainCarriageData();
        NBTTagList bogeyList = nbt.getTagList("Bogeys", 10);
        for (int i = 0; i < bogeyList.tagCount(); i++) {
            data.bogeys.add(BlockPos.fromLong(bogeyList.getCompoundTagAt(i).getLong("Pos")));
        }
        NBTTagList blockList = nbt.getTagList("Blocks", 10);
        for (int i = 0; i < blockList.tagCount(); i++) {
            NBTTagCompound c = blockList.getCompoundTagAt(i);
            NBTTagCompound te = c.hasKey("TE", 10) ? c.getCompoundTag("TE") : null;
            data.blocks.add(new Entry(
                    BlockPos.fromLong(c.getLong("Pos")),
                    c.getString("Block"),
                    c.getInteger("Meta"),
                    te));
        }
        return data;
    }

    public static NBTTagList writeList(List<TrainCarriageData> carriages) {
        NBTTagList list = new NBTTagList();
        for (TrainCarriageData c : carriages) {
            list.appendTag(c.writeToNBT(new NBTTagCompound()));
        }
        return list;
    }

    public static List<TrainCarriageData> readList(NBTTagList list) {
        List<TrainCarriageData> out = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            out.add(readFromNBT(list.getCompoundTagAt(i)));
        }
        return out;
    }
}
