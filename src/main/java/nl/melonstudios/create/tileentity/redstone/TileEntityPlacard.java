package nl.melonstudios.create.tileentity.redstone;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import nl.melonstudios.create.block.redstone.BlockPlacard;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.Utils;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Holds the single displayed stack and the output pulse countdown. The
 * pulse drains once per tick and drops the block output when it runs out,
 * translated from the reference powered-ticks behaviour.
 */
public class TileEntityPlacard extends TileEntityOptimizedBase {
    private ItemStack heldItem = ItemStack.EMPTY;
    private int poweredTicks = 0;

    public ItemStack getHeldItem() {
        return this.heldItem;
    }

    public void setHeldItem(ItemStack stack) {
        this.heldItem = stack;
        this.sync();
        this.markDirty();
    }

    public void pulse() {
        this.poweredTicks = 19;
        this.sync();
        this.markDirty();
    }

    @Override
    public void tick() {
        if (this.world == null || this.world.isRemote) return;
        if (this.poweredTicks == 0) return;
        this.poweredTicks--;
        if (this.poweredTicks > 0) return;
        IBlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() instanceof BlockPlacard && state.getValue(BlockPlacard.POWERED)) {
            Utils.setBlockTESafe(this.world, this.pos, state.withProperty(BlockPlacard.POWERED, false), 3);
            this.world.notifyNeighborsOfStateChange(this.pos, state.getBlock(), false);
        }
    }

    @Override
    public void tickLazy() {
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("PoweredTicks", this.poweredTicks);
        nbt.setTag("Item", this.heldItem.writeToNBT(new NBTTagCompound()));
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.poweredTicks = Math.max(0, nbt.getInteger("PoweredTicks"));
        this.heldItem = new ItemStack(nbt.getCompoundTag("Item"));
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Item", this.heldItem.writeToNBT(new NBTTagCompound()));
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.heldItem = new ItemStack(nbt.getCompoundTag("Item"));
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        ByteBuf temp = Unpooled.buffer();
        ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(ByteBufUtils.readTag(buf));
    }
}
