package nl.melonstudios.create.tileentity.train;

import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import nl.melonstudios.create.block.train.BlockSteamWhistle;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

/**
 * Whistle TE: counts extension blocks above (max 6) and hoots on redstone
 * rising edge. Pitch drops 2 semitones per extension level plus a size
 * offset (small +0, medium -2, large -4 semitones), translated from the
 * reference pitch formula. Plays the vanilla harp as a stand-in until the
 * whistle oggs are wired (NEEDS-LEAD: sounds.json + SoundInit entries).
 */
public class TileEntitySteamWhistle extends TileEntityOptimizedBase {
    private boolean lastPowered;
    private int extensions;
    /** WhistleSize ordinal. Stored here (not in 4-bit meta) so the size
     * survives chunk reloads; BlockSteamWhistle#getActualState overlays it. */
    private int size = BlockSteamWhistle.WhistleSize.MEDIUM.ordinal();

    public BlockSteamWhistle.WhistleSize getWhistleSize() {
        BlockSteamWhistle.WhistleSize[] sizes = BlockSteamWhistle.WhistleSize.values();
        return sizes[Math.max(0, Math.min(this.size, sizes.length - 1))];
    }

    public void setWhistleSize(BlockSteamWhistle.WhistleSize size) {
        this.size = size.ordinal();
        this.sync();
    }

    public static void queuePitchUpdate(net.minecraft.world.World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntitySteamWhistle) ((TileEntitySteamWhistle) te).recount();
    }

    /** Grow the stack above the whistle at pos (max 6 high, widen before growing). */
    public static void grow(net.minecraft.world.World world, BlockPos whistlePos) {
        IBlockState base = world.getBlockState(whistlePos);
        if (!(base.getBlock() instanceof BlockSteamWhistle)) return;
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(whistlePos);
        BlockSteamWhistle.WhistleSize size = te instanceof TileEntitySteamWhistle
                ? ((TileEntitySteamWhistle) te).getWhistleSize()
                : base.getValue(BlockSteamWhistle.SIZE);
        BlockPos cursor = whistlePos.up();
        for (int i = 1; i <= 6; i++) {
            IBlockState at = world.getBlockState(cursor);
            if (at.getBlock() instanceof nl.melonstudios.create.block.train.BlockSteamWhistleExtension) {
                if (at.getValue(nl.melonstudios.create.block.train.BlockSteamWhistleExtension.SHAPE)
                        == nl.melonstudios.create.block.train.BlockSteamWhistleExtension.Shape.SINGLE) {
                    world.setBlockState(cursor, at.withProperty(
                            nl.melonstudios.create.block.train.BlockSteamWhistleExtension.SHAPE,
                            nl.melonstudios.create.block.train.BlockSteamWhistleExtension.Shape.DOUBLE), 3);
                    return;
                }
                cursor = cursor.up();
                continue;
            }
            if (!at.getBlock().isReplaceable(world, cursor)) return;
            net.minecraft.block.Block ext = net.minecraft.block.Block.REGISTRY.getObject(
                    new net.minecraft.util.ResourceLocation("create", "steam_whistle_extension"));
            if (ext == null) return;
            world.setBlockState(cursor, ext.getDefaultState().withProperty(
                    nl.melonstudios.create.block.train.BlockSteamWhistleExtension.SIZE, size), 3);
            return;
        }
    }

    private void recount() {
        if (this.world == null) return;
        int count = 0;
        BlockPos cursor = this.pos.up();
        for (int i = 0; i < 6; i++) {
            if (this.world.getBlockState(cursor).getBlock()
                    instanceof nl.melonstudios.create.block.train.BlockSteamWhistleExtension) {
                count++;
                cursor = cursor.up();
            } else break;
        }
        this.extensions = count;
        this.sync();
    }

    public void onRedstone(boolean powered) {
        if (powered && !this.lastPowered) this.hoot();
        this.lastPowered = powered;
    }

    public void hoot() {
        if (this.world == null || this.world.isRemote) return;
        this.recount();
        BlockSteamWhistle.WhistleSize size = this.getWhistleSize();
        int sizeDrop = size == BlockSteamWhistle.WhistleSize.SMALL ? 0
                : size == BlockSteamWhistle.WhistleSize.MEDIUM ? 2 : 4;
        float pitch = (float) Math.pow(2.0, -(this.extensions * 2 + sizeDrop) / 12.0);
        this.world.playSound(null, this.pos, SoundEvents.BLOCK_NOTE_HARP, SoundCategory.BLOCKS, 1.5F, pitch);
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
        nbt.setInteger("ext", this.extensions);
        nbt.setBoolean("powered", this.lastPowered);
        nbt.setInteger("size", this.size);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.extensions = nbt.getInteger("ext");
        this.lastPowered = nbt.getBoolean("powered");
        if (nbt.hasKey("size")) this.size = nbt.getInteger("size");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("ext", this.extensions);
        nbt.setBoolean("powered", this.lastPowered);
        nbt.setInteger("size", this.size);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.extensions = nbt.getInteger("ext");
        this.lastPowered = nbt.getBoolean("powered");
        if (nbt.hasKey("size")) this.size = nbt.getInteger("size");
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
