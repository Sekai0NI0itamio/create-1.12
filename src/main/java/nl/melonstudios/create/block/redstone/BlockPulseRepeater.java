package nl.melonstudios.create.block.redstone;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import nl.melonstudios.create.tileentity.redstone.TileEntityPulseRepeater;

import javax.annotation.Nullable;

/**
 * Pulse repeater: re-fires the latched output for the set delay after the
 * back input drops, so short button presses still read downstream.
 */
public class BlockPulseRepeater extends BlockPulseDiode {
    public BlockPulseRepeater() {
        super();
        this.setRegistryName("pulse_repeater");
        this.setUnlocalizedName("create.pulse_repeater");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityPulseRepeater();
    }
}
