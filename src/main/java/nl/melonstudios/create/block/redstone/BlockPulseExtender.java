package nl.melonstudios.create.block.redstone;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import nl.melonstudios.create.tileentity.redstone.TileEntityPulseExtender;

import javax.annotation.Nullable;

/**
 * Pulse extender: any rising back input latches the output on, then the
 * output holds for the full set delay after the input drops.
 */
public class BlockPulseExtender extends BlockPulseDiode {
    public BlockPulseExtender() {
        super();
        this.setRegistryName("pulse_extender");
        this.setUnlocalizedName("create.pulse_extender");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityPulseExtender();
    }
}
