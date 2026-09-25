package nl.melonstudios.create.tileentity.generator;

import com.melonstudios.melonlib.misc.BlockStateProperties;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.generator.BlockHandCrank;
import nl.melonstudios.create.tileentity.TileEntityKineticGeneratorBase;

public class TileEntityHandCrank extends TileEntityKineticGeneratorBase {
    public int inUse;
    public boolean backwards;
    // Chased handle angle (degrees). Smoothed visual state derived from the
    // synced kinetic speed each tick on both sides, like the gauge dial state:
    // no packet of its own, both sides converge from the same speed.
    public float independentAngle;
    public float chasingVelocity;

    public void turn(boolean back) {
        boolean update = this.getGeneratedSpeed() == 0 || back != this.backwards;

        this.inUse = 10;
        this.backwards = back;
        if (update && !this.world.isRemote) {
            this.updateGeneratedRotation();
        }
    }

    public float getIndependentAngle(float partialTicks) {
        return this.independentAngle + partialTicks * this.chasingVelocity;
    }

    @Override
    public float getGeneratedSpeed() {
        final Block block = this.getBlockType();
        if (!(block instanceof BlockHandCrank)) return 0.0F;
        int speed = (this.inUse == 0 ? 0 : this.backwards ? -1 : 1) * ((BlockHandCrank)block).getRotationSpeed();
        return convertToDirection(speed, this.getState().getValue(BlockHandCrank.FACING));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setInteger("inUse", this.inUse);
        compound.setBoolean("backwards", this.backwards);

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.inUse = compound.getInteger("inUse");
        this.backwards = compound.getBoolean("backwards");
    }

    @Override
    public void tick() {
        super.tick();

        // Handle chases the shaft speed with a lag so it spins up/down
        // smoothly instead of snapping. Runs on both sides; both derive
        // from the synced speed, so server and client stay in agreement.
        float actualSpeed = this.getSpeed();
        this.chasingVelocity += ((actualSpeed * 10.0F / 3.0F) - this.chasingVelocity) * 0.25F;
        this.independentAngle += this.chasingVelocity;

        if (this.inUse > 0) {
            this.inUse--;

            if (this.inUse == 0 && !this.world.isRemote) {
                this.updateGeneratedRotation();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public EnumFacing getRenderFacing() {
        return this.getState().getValue(BlockStateProperties.FACING);
    }
}
