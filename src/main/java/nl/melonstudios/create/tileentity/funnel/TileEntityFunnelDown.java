package nl.melonstudios.create.tileentity.funnel;

import net.minecraft.util.EnumFacing;

/**
 * Down-facing (standard) funnel logic. Reuses the wall funnel transfer machine
 * unchanged: 8-tick extraction cooldown, redstone pauses and resets the
 * cooldown, output goes to the depot below (or drops as an item entity),
 * flap state + flap sound on every transfer. The only difference is the source
 * side: a down funnel always draws from the inventory ABOVE it, matching the
 * original mod's FACING=DOWN funnel whose InvManipulationBehaviour targets
 * the opposite side of its facing.
 */
public class TileEntityFunnelDown extends TileEntityFunnelWall {
    public TileEntityFunnelDown() {
        super();
    }

    @Override
    public EnumFacing getFacing(int meta) {
        return EnumFacing.DOWN;
    }
}
