package nl.melonstudios.create.tileentity.redstone;

import com.melonstudios.melonlib.misc.MetaItem;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import com.melonstudios.melonlib.tileentity.ISyncedTE;
import com.melonstudios.melonlib.tileentity.TileEntityCachedRenderBB;
import io.netty.buffer.ByteBuf;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.block.redstone.BlockRedstoneLinker;
import nl.melonstudios.create.savedata.WorldRedstoneSignals;
import nl.melonstudios.create.util.Utils;
import nl.melonstudios.create.util.interfaces.IFrequencyReceiver;

import java.io.IOException;

public class TileEntityRedstoneLinker extends TileEntityCachedRenderBB implements ISyncedTE, IFrequencyReceiver {
    private ItemStack freq1Stack = null;
    private ItemStack freq2Stack = null;
    public WorldRedstoneSignals.LinkFrequency frequency = WorldRedstoneSignals.LinkFrequency.get(MetaItem.of(Items.DIAMOND, 0), MetaItem.AIR);

    public int signal = 0;

    private ItemStack asStack(MetaItem metaItem) {
        return metaItem.isAir() ? ItemStack.EMPTY : metaItem.asItemStack();
    }
    public ItemStack getFreq1Stack() {
        if (this.freq1Stack == null) this.freq1Stack = this.asStack(this.frequency.first);
        return this.freq1Stack;
    }
    public ItemStack getFreq2Stack() {
        if (this.freq2Stack == null) this.freq2Stack = this.asStack(this.frequency.second);
        return this.freq2Stack;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        if (this.frequency != WorldRedstoneSignals.LinkFrequency.EMPTY) {
            nbt.setTag("Frequency", this.frequency.write(new NBTTagCompound()));
        }
        nbt.setInteger("signal", this.signal);

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        if (nbt.hasKey("Frequency", 10)) {
            this.frequency = WorldRedstoneSignals.LinkFrequency.read(nbt.getCompoundTag("Frequency"));
        }
        this.signal = nbt.getInteger("signal");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        buf.writeByte(this.signal);
        buf.writeInt(Item.getIdFromItem(this.frequency.first.getItem()));
        buf.writeShort(this.frequency.first.getMetadata());
        buf.writeInt(Item.getIdFromItem(this.frequency.second.getItem()));
        buf.writeShort(this.frequency.second.getMetadata());
    }
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.signal = buf.readUnsignedByte();
        this.frequency = WorldRedstoneSignals.LinkFrequency.get(
                MetaItem.of(Item.getItemById(buf.readInt()), buf.readUnsignedShort()),
                MetaItem.of(Item.getItemById(buf.readInt()), buf.readUnsignedShort())
        );
    }

    @Override
    public void updateSignal(int signal) {
        CreateLegacy.logger.debug("Received signal strength of {} (had: {})", signal, this.signal);
        boolean changed = signal != this.signal;
        this.signal = signal;
        Utils.setBlockTESafe(this.world, this.pos,
                this.getBlockType().getDefaultState()
                        .withProperty(BlockRedstoneLinker.FACING, EnumFacing.VALUES[this.getBlockMetadata() & 0b0111])
                        .withProperty(BlockRedstoneLinker.POWERED, signal > 0), 2
        );
        if (changed && this.world != null && this.world.isRemote) {
            CreateLegacy.proxy.wifiFX(this.world,
                    this.pos.getX() + 0.5D, this.pos.getY() + 1.1D, this.pos.getZ() + 0.5D,
                    0xBFFFEF);
        }
        this.sync();
    }

    @Override
    public boolean isAttuned(WorldRedstoneSignals.LinkFrequency frequency) {
        this.getBlockType();
        if (!(this.blockType instanceof BlockRedstoneLinker)) return false;
        return ((BlockRedstoneLinker)this.blockType).isReceiving && this.frequency.equals(frequency);
    }

    @Override
    public void setWorld(World worldIn) {
        if (this.world != null) {
            WorldRedstoneSignals signals = WorldRedstoneSignals.get(this.world);
            synchronized (signals.receivers) {
                signals.receivers.remove(this);
            }
        }
        super.setWorld(worldIn);
        if (this.world != null) {
            WorldRedstoneSignals signals = WorldRedstoneSignals.get(this.world);
            synchronized (signals.receivers) {
                signals.receivers.add(this);
            }
        }
    }
}
