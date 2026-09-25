package nl.melonstudios.create.tileentity;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import nl.melonstudios.create.item.ItemClipboard;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Clipboard wall-block storage. Reference: ClipboardBlockEntity
 * (dataContainer ItemStack + WRITTEN state + null-safe read that resets to a
 * fresh clipboard when the stored stack is not a clipboard).
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TileEntityClipboard extends TileEntity {
    private ItemStack dataContainer = ItemStack.EMPTY;

    public ItemStack getData() {
        return this.dataContainer;
    }

    public void setData(ItemStack stack) {
        this.dataContainer = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        this.markDirty();
        if (this.world != null) {
            this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos),
                    this.world.getBlockState(this.pos), 3);
        }
    }

    public boolean isWritten() {
        return ItemClipboard.isWritten(this.dataContainer);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound nbt = super.writeToNBT(compound);
        if (!this.dataContainer.isEmpty()) nbt.setTag("Item", this.dataContainer.writeToNBT(new NBTTagCompound()));
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.dataContainer = compound.hasKey("Item", 10)
                ? new ItemStack(compound.getCompoundTag("Item"))
                : ItemStack.EMPTY;
        // Reference null-safety: never keep a non-clipboard stack.
        if (!this.dataContainer.isEmpty() && !(this.dataContainer.getItem() instanceof ItemClipboard)) {
            this.dataContainer = ItemStack.EMPTY;
        }
    }
}
