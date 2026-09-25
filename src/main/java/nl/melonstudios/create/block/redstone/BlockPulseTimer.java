package nl.melonstudios.create.block.redstone;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import nl.melonstudios.create.tileentity.redstone.TileEntityPulseTimer;

import javax.annotation.Nullable;

/**
 * Pulse timer: free-running clock that blips the output on for two ticks
 * per set period while the back input stays off; any back input resets it.
 */
public class BlockPulseTimer extends BlockPulseDiode {
    public BlockPulseTimer() {
        super();
        this.setRegistryName("pulse_timer");
        this.setUnlocalizedName("create.pulse_timer");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityPulseTimer();
    }
}
