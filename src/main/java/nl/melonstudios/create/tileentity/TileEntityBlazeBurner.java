package nl.melonstudios.create.tileentity;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import nl.melonstudios.create.block.BlockBlazeBurner;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.Utils;

public class TileEntityBlazeBurner extends TileEntityOptimizedBase {
    public static final int MAX_HEAT_CAPACITY = 10000;
    public int fuelTicks = 0; //Above MAX_HEAT_CAPACITY is superheated
    public boolean isCreative = false;

    public TileEntityBlazeBurner() {

    }

    @Override
    public void tick() {
        if (this.isCreative) {
            return;
        }
        if (this.fuelTicks > 0 && !this.isVirtual()) {
            this.fuelTicks--;
            this.markDirty();
            if (!this.world.isRemote) {
                IBlockState oldState = this.getState();
                IBlockState newState = oldState.withProperty(BlockBlazeBurner.VARIANT, this.fuelTicks > MAX_HEAT_CAPACITY ? BlockBlazeBurner.Variant.SUPERHEATED :
                        this.fuelTicks != 0 ? BlockBlazeBurner.Variant.HEATED : BlockBlazeBurner.Variant.PASSIVE);
                if (oldState != newState) {
                    Utils.setBlockTESafe(this.world, this.pos, newState, 3);
                    this.syncNextTick();
                }
            }
        }
    }

    @Override
    public void tickLazy() {

    }

    public void feed(int ticks) {
        if (this.isCreative) {
            return;
        }
        this.fuelTicks = Math.min(this.fuelTicks + ticks, MAX_HEAT_CAPACITY);
        this.sync();
    }
    public void blazecake(int ticks) {
        if (this.isCreative) {
            return;
        }
        this.fuelTicks = MAX_HEAT_CAPACITY + ticks;
        this.sync();
    }

    public static boolean isCreativeFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ItemInit.CREATIVE_BLAZE_CAKE;
    }

    /**
     * Reference applyCreativeFuel: infinite fuel, cycle the heat level.
     * HEATED -> SUPERHEATED -> HEATED (stays in TE-backed states).
     * The cake itself is never consumed.
     */
    public void applyCreativeFuel() {
        this.isCreative = true;
        this.fuelTicks = 0;
        if (this.world != null && !this.world.isRemote) {
            IBlockState oldState = this.getState();
            BlockBlazeBurner.Variant current = oldState.getValue(BlockBlazeBurner.VARIANT);
            BlockBlazeBurner.Variant next = current == BlockBlazeBurner.Variant.SUPERHEATED
                    ? BlockBlazeBurner.Variant.HEATED
                    : BlockBlazeBurner.Variant.SUPERHEATED;
            if (oldState.getValue(BlockBlazeBurner.VARIANT) != next) {
                Utils.setBlockTESafe(this.world, this.pos, oldState.withProperty(BlockBlazeBurner.VARIANT, next), 3);
            }
            this.markDirty();
            this.sync();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setInteger("fuelTicks", this.fuelTicks);
        nbt.setBoolean("creative", this.isCreative);

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        this.fuelTicks = nbt.getInteger("fuelTicks");
        this.isCreative = nbt.getBoolean("creative");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) {
        buf.writeShort(this.fuelTicks);
        buf.writeByte(this.isCreative ? 1 : 0);
    }

    @Override
    public void readPacket(ByteBuf buf) {
        this.fuelTicks = buf.readUnsignedShort();
        this.isCreative = buf.readByte() != 0;
    }
}
