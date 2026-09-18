package nl.melonstudios.create.tileentity.funnel;

import net.minecraft.util.EnumFacing;

/**
 * Brass (filtered) down funnel. Filter slot, adjustable extraction amount and
 * client sync are inherited from the brass wall funnel; the inherited filter
 * interaction box is already facing-agnostic (centered on the funnel mouth),
 * so only the facing is fixed to DOWN here.
 *
 * Reference default with no filter active is a single item, hence the default
 * extraction amount of 1 (the brass wall keeps its own default).
 */
public class TileEntityFunnelDownAdvanced extends TileEntityFunnelWallAdvanced {
    public TileEntityFunnelDownAdvanced() {
        super();
        this.extractionAmount = 1;
    }

    @Override
    public EnumFacing getFacing(int meta) {
        return EnumFacing.DOWN;
    }
}
