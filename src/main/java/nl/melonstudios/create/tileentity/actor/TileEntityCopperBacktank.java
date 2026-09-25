package nl.melonstudios.create.tileentity.actor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

/**
 * Shared breathing-tank logic for the placed backtank blocks. Reference values
 * (BacktankUtil/CEquipment): 900 base air, enchant step 300 (item-side only).
 * The block spends 1 air per player topped up (once per second, 3-block
 * radius, refills to the vanilla 300 max) and sips 1 air back every 2 seconds
 * while idle and below capacity.
 */
public class TileEntityCopperBacktank extends TileEntity implements ITickable {
    public TileEntityCopperBacktank() {
    }

    public int air = this.maxAir();

    public int maxAir() {
        return 900;
    }

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) return;
        long time = this.world.getTotalWorldTime();
        if (this.air > 0 && time % 20 == 0) {
            AxisAlignedBB box = new AxisAlignedBB(this.pos).grow(3.0D);
            List<EntityPlayer> players = this.world.getEntitiesWithinAABB(EntityPlayer.class, box);
            for (EntityPlayer player : players) {
                if (player.getAir() < 300) {
                    player.setAir(300);
                    this.air = Math.max(0, this.air - 1);
                    this.markDirty();
                    if (this.air <= 0) break;
                }
            }
        } else if (this.air < this.maxAir() && time % 40 == 0) {
            this.air = Math.min(this.maxAir(), this.air + 1);
            this.markDirty();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Air", this.air);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.air = Math.max(0, Math.min(this.maxAir(), compound.getInteger("Air")));
    }
}
