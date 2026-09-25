package nl.melonstudios.create.tileentity.deco;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Mimic storage for the copycat family (base / step / panel / bars).
 *
 * Reference: CopycatBlockEntity holds a full consumed ItemStack plus the
 * placed material state, and the client renders the mimic through a custom
 * baked model (CopycatModel and friends).
 *
 * 1.12 simplification (documented): the stored consumed item + mimic state
 * round-trip through NBT exactly like the reference, but there is no custom
 * baked-model pipeline here — the blocks render with the neutral
 * copycat_base texture. Wiring a TESR / baked mimic renderer is NEEDS-LEAD
 * client work; the data it needs is all in this class.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TileEntityCopycat extends TileEntity {
    private ItemStack consumed = ItemStack.EMPTY;
    private int mimicMeta;

    public boolean hasMimic() {
        return !this.consumed.isEmpty();
    }

    public ItemStack getConsumed() {
        return this.consumed;
    }

    public void setMimic(ItemStack stack, int meta) {
        this.consumed = stack.copy();
        this.consumed.setCount(1);
        this.mimicMeta = meta;
        this.markDirty();
    }

    public void clearMimic() {
        this.consumed = ItemStack.EMPTY;
        this.mimicMeta = 0;
        this.markDirty();
    }

    /**
     * Resolves the stored mimic to a live block state, or air when the stored
     * block no longer exists (stale NBT after removing another mod).
     */
    public IBlockState getMimicState() {
        if (this.consumed.isEmpty()) return Blocks.AIR.getDefaultState();
        Block block = Block.getBlockFromItem(this.consumed.getItem());
        if (block == null || block == Blocks.AIR) return Blocks.AIR.getDefaultState();
        try {
            return block.getStateFromMeta(this.mimicMeta);
        } catch (Exception e) {
            return block.getDefaultState();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound nbt = super.writeToNBT(compound);
        if (!this.consumed.isEmpty()) {
            nbt.setTag("MimicItem", this.consumed.writeToNBT(new NBTTagCompound()));
            nbt.setInteger("MimicMeta", this.mimicMeta);
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("MimicItem", 10)) {
            this.consumed = new ItemStack(compound.getCompoundTag("MimicItem"));
            this.mimicMeta = compound.getInteger("MimicMeta");
        } else {
            this.consumed = ItemStack.EMPTY;
            this.mimicMeta = 0;
        }
    }
}
