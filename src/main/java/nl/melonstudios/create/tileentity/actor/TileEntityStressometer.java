package nl.melonstudios.create.tileentity.actor;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import nl.melonstudios.create.tileentity.TileEntityGaugeBase;
import nl.melonstudios.create.util.Color;

public class TileEntityStressometer extends TileEntityGaugeBase {
    static BlockPos lastSent;

    @Override
    public void updateFromNetwork(float capacity, float stress, int networkSize) {
        super.updateFromNetwork(capacity, stress, networkSize);

        if (this.overstressed) this.dialTarget = 1.125F;
        else if (capacity == 0.0F) this.dialTarget = 0.0F;
        else {
            this.dialTarget = stress / capacity;
        }

        if (this.dialTarget > 0) {
            if (this.dialTarget < 0.5F) this.color = Color.mixColors(0x00FF00, 0xFFFF00, this.dialTarget * 2);
            else if (this.dialTarget < 1.0F) this.color = Color.mixColors(0xFFFF00, 0xFF0000, this.dialTarget * 2 - 1);
            else this.color = 0xFF0000;
        }

        this.sync();
        this.markDirty();
    }

    @Override
    public void onSpeedChanged(float lastSpeed) {
        super.onSpeedChanged(lastSpeed);

        if (this.getSpeed() == 0) {
            this.dialTarget = 0.0F;
            this.markDirty();
            return;
        }

        this.updateFromNetwork(this.capacity, this.stress, this.getOrCreateNetwork().getSize());
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        super.readPacket(nbt);

        if (this.pos != null && this.pos.equals(lastSent)) lastSent = null;
    }

    public float getNetworkStress() {
        return this.stress;
    }
    public float getNetworkCapacity() {
        return this.capacity;
    }
}
