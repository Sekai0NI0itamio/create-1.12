package nl.melonstudios.create.tileentity.redstone;

import net.minecraft.block.state.IBlockState;
import nl.melonstudios.create.block.redstone.BlockPulseDiode;

/**
 * Re-fires the latched output for the set delay: the countdown runs past
 * the input, flipping the output on near the end and off one step later.
 */
public class TileEntityPulseRepeater extends TileEntityBrassDiode {
    public TileEntityPulseRepeater() {
        super(2);
    }

    @Override
    protected void updateState(boolean powered, boolean powering, boolean atMax, boolean atMin,
                              BlockPulseDiode block, IBlockState blockState) {
        if (atMin && !powered) return;
        if (this.state > this.delay + 1) {
            if (!powered && !powering) this.state = 0;
            return;
        }

        this.state++;
        if (this.state == this.delay - 1 && !powering) {
            block.setPowering(this.world, this.pos, blockState, true);
        }
        if (this.state == this.delay + 1 && powering) {
            block.setPowering(this.world, this.pos, blockState, false);
        }
    }
}
