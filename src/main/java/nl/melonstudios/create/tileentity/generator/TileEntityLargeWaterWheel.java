package nl.melonstudios.create.tileentity.generator;

/**
 * Large water wheel generator (translated from the reference
 * LargeWaterWheelBlockEntity, MIT). Only the size changes: size 2 selects
 * the LARGE_OFFSETS rim ring and halves the output to 4 RPM max via the
 * inherited clamp(flow, -1, 1) * 8 / size formula.
 */
public class TileEntityLargeWaterWheel extends TileEntityWaterWheel {
    @Override
    protected int getSize() {
        return 2;
    }
}
