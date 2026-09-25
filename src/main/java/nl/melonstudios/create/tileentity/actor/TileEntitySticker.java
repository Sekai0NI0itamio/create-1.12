package nl.melonstudios.create.tileentity.actor;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.kinetics.contraption.IContraptionActor;
import nl.melonstudios.create.kinetics.contraption.accessor.IContraptionAccessor;
import org.joml.Vector3fc;

/**
 * Sticker actor: remembers whether its sticky face is currently holding.
 * The grabbing itself is resolved by the contraption assembly (glue layer);
 * this tile only carries the on/off + attached flags through NBT.
 */
public class TileEntitySticker extends TileEntity implements IContraptionActor {
    public TileEntitySticker() {
    }

    public boolean moving = false;
    public boolean attached = false;

    @Override
    public void setOnContraption(boolean onContraption) {
        this.moving = onContraption;
        if (!onContraption) this.attached = false;
    }

    @Override
    public boolean isOnContraption() {
        return this.moving;
    }

    @Override
    public void contraptionTick(IContraptionAccessor contraption, World world, Vector3fc position, BlockPos blockPosition, boolean moved, Vector3fc movement) {
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("Attached", this.attached);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.attached = compound.getBoolean("Attached");
    }
}
