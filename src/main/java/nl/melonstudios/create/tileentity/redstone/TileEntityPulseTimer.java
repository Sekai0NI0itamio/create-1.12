package nl.melonstudios.create.tileentity.redstone;

import net.minecraft.block.state.IBlockState;
import nl.melonstudios.create.block.redstone.BlockPulseDiode;

/**
 * Free-running clock: counts up to the set period while the back input is
 * off and blips the output on for the first two ticks of each period. Any
 * back input holds the countdown at zero.
 */
public class TileEntityPulseTimer extends TileEntityBrassDiode {
    public TileEntityPulseTimer() {
        super(20);
    }

    @Override
    protected void updateState(boolean powered, boolean powering, boolean atMax, boolean atMin,
                              BlockPulseDiode block, IBlockState blockState) {
        if (powered || this.state >= this.delay - 1) {
            this.state = 0;
        } else {
            this.state++;
        }

        boolean shouldPower = !powered && (this.delay == 2 ? this.state == 0 : this.state <= 1);
        if (powering != shouldPower) {
            block.setPowering(this.world, this.pos, blockState, shouldPower);
        }
    }
}
