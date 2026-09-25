package nl.melonstudios.create.tileentity.actor;

/**
 * Netherite backtank tile: double reservoir (1800 air), same breathing rules
 * as copper (see TileEntityCopperBacktank).
 */
public class TileEntityNetheriteBacktank extends TileEntityCopperBacktank {
    public TileEntityNetheriteBacktank() {
        super();
    }

    @Override
    public int maxAir() {
        return 1800;
    }
}
