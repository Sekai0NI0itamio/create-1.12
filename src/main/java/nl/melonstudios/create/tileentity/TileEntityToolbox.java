package nl.melonstudios.create.tileentity;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Toolbox storage. Reference: ToolboxBlockEntity + ToolboxInventory (8
 * compartments x 4 stacks = 32 slots, capability-exposed, per-compartment
 * settling). The radial equip menu and auto-equip handler need client GUI +
 * key handling, so this port keeps the 32-slot capability inventory with
 * insert/extract through block use instead.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TileEntityToolbox extends TileEntity {
    /** Reference: 8 compartments * STACKS_PER_COMPARTMENT(4). */
    public static final int SLOT_COUNT = 32;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            TileEntityToolbox.this.markDirty();
        }
    };

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound nbt = super.writeToNBT(compound);
        nbt.setTag("Items", this.inventory.serializeNBT());
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Items", 10)) this.inventory.deserializeNBT(compound.getCompoundTag("Items"));
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) this.inventory;
        }
        return super.getCapability(capability, facing);
    }
}
