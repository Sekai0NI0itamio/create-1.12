package nl.melonstudios.create.tileentity.logistics;

import com.melonstudios.melonlib.misc.StackUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.item.ItemPackage;

/**
 * Repackager TE: pulls package items from the inventory below, holds each
 * one for one iris cycle, and re-emits it with the configured address.
 * Ports the reference RepackagerBlockEntity observable behaviour: 20-tick
 * cooldown between pulls, 20-tick (CYCLE) inward iris animation per box,
 * speed-gated, contents preserved, address applied on re-emit.
 */
public class TileEntityRepackager extends TileEntityPackager {
    /** One package held through the iris cycle before re-emit. */
    public ItemStack heldBox = ItemStack.EMPTY;
    private int cooldown;

    public boolean hasBox() {
        return !this.heldBox.isEmpty();
    }

    @Override
    public void tick() {
        this.lastAnimationTicks = this.animationTicks;
        if (this.world.isRemote) {
            if (this.animationTicks > 0) this.animationTicks--;
            return;
        }
        if (this.getSpeed() == 0) return;
        if (this.animationTicks > 0 && --this.animationTicks == 0) {
            if (!this.heldBox.isEmpty()) {
                ItemStack box = this.heldBox.copy();
                this.heldBox = ItemStack.EMPTY;
                if (!this.address.isEmpty()) ItemPackage.setAddress(box, this.address);
                StackUtil.spawnItemNoVelocity(this.world,
                        this.pos.getX() + 0.5, this.pos.getY() + 1.1, this.pos.getZ() + 0.5, box);
                this.world.playSound(null, this.pos, SoundInit.packager, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            this.sync();
        }
        if (!this.heldBox.isEmpty()) return;
        if (--this.cooldown > 0) return;
        this.cooldown = 20;

        IItemHandler source = this.attachedInventory();
        if (source == null) return;
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack inSlot = source.getStackInSlot(i);
            if (inSlot.isEmpty() || inSlot.getItem() != ItemInit.PACKAGE) continue;
            ItemStack taken = source.extractItem(i, 1, false);
            if (taken.isEmpty()) continue;
            this.heldBox = taken.copy();
            this.animationTicks = CYCLE;
            this.sync();
            return;
        }
    }

    private IItemHandler attachedInventory() {
        TileEntity te = this.world.getTileEntity(this.pos.down());
        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP)) {
            return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        }
        return null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (!this.heldBox.isEmpty()) nbt.setTag("HeldBox", this.heldBox.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("RepackCooldown", this.cooldown);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.heldBox = nbt.hasKey("HeldBox", 10) ? new ItemStack(nbt.getCompoundTag("HeldBox")) : ItemStack.EMPTY;
        this.cooldown = nbt.getInteger("RepackCooldown");
    }
}
