package nl.melonstudios.create.tileentity.logistics;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import java.io.IOException;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.Nullable;

/**
 * Packager link: sits against one packager and lends that packager's stock
 * to the addressed order network, so the ticker path can find packagers that
 * are not standing right next to a ticker. A redstone signal mutes the link.
 */
public class TileEntityPackagerLink extends TileEntityOptimizedBase {
    public String address = "";
    public boolean linked;

    public TileEntityPackagerLink() {
        this.setTickRateLazy(10);
    }

    /** The packager this link faces, or null when unlinked or muted. */
    @Nullable
    public TileEntityPackager packager() {
        if (this.world == null) return null;
        if (this.world.isBlockPowered(this.pos)) return null;
        for (EnumFacing side : EnumFacing.VALUES) {
            TileEntity te = this.world.getTileEntity(this.pos.offset(side));
            if (te instanceof TileEntityPackager) return (TileEntityPackager) te;
        }
        return null;
    }

    public int comparatorLevel() {
        return this.linked ? 15 : 0;
    }

    @Override
    public void tick() {
    }

    @Override
    public void tickLazy() {
        if (this.world.isRemote) return;
        TileEntityPackager packager = this.packager();
        boolean was = this.linked;
        String oldAddress = this.address;
        this.linked = packager != null;
        if (packager != null) this.address = packager.address;
        if (was != this.linked || !oldAddress.equals(this.address)) this.sync();
    }

    /**
     * Finds one packager serving an address: direct neighbours first, then
     * packagers joined through nearby links. Empty matches empty.
     */
    @Nullable
    public static TileEntityPackager packagerForAddress(World world, BlockPos near, String address, int radius) {
        String want = address == null ? "" : address;
        for (BlockPos candidate : BlockPos.getAllInBox(
                near.add(-radius, -radius, -radius), near.add(radius, radius, radius))) {
            TileEntity te = world.getTileEntity(candidate);
            if (te instanceof TileEntityPackager) {
                TileEntityPackager packager = (TileEntityPackager) te;
                if (want.equals(packager.address == null ? "" : packager.address)) return packager;
            } else if (te instanceof TileEntityPackagerLink) {
                TileEntityPackager packager = ((TileEntityPackagerLink) te).packager();
                if (packager != null
                        && want.equals(packager.address == null ? "" : packager.address)) return packager;
            }
        }
        return null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("Address", this.address == null ? "" : this.address);
        nbt.setBoolean("Linked", this.linked);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.address = nbt.getString("Address");
        this.linked = nbt.getBoolean("Linked");
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("Address", this.address == null ? "" : this.address);
        nbt.setBoolean("Linked", this.linked);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.address = nbt.getString("Address");
        this.linked = nbt.getBoolean("Linked");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        io.netty.buffer.ByteBuf temp = io.netty.buffer.Unpooled.buffer();
        net.minecraftforge.fml.common.network.ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(net.minecraftforge.fml.common.network.ByteBufUtils.readTag(buf));
    }
}
