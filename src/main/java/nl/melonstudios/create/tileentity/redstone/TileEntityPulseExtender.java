package nl.melonstudios.create.tileentity.redstone;

import net.minecraft.block.state.IBlockState;
import nl.melonstudios.create.block.redstone.BlockPulseDiode;

/**
 * Any powered input (or a running countdown) latches the output on at the
 * full delay; the countdown only drains while the input is off and drops
 * the output on its last tick.
 */
public class TileEntityPulseExtender extends TileEntityBrassDiode {
    public TileEntityPulseExtender() {
        super(2);
    }

    @Override
    protected void updateState(boolean powered, boolean powering, boolean atMax, boolean atMin,
                              BlockPulseDiode block, IBlockState blockState) {
        if (atMin && !powered) return;
        if (atMin || powered) {
            block.setPowering(this.world, this.pos, blockState, true);
            this.state = this.delay;
            return;
        }

        if (this.state == 1) {
            if (powering) block.setPowering(this.world, this.pos, blockState, false);
            if (!powered) this.state = 0;
            return;
        }

        if (!powered) this.state--;
    }
}
