package nl.melonstudios.create.block.actor;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import nl.melonstudios.create.tileentity.actor.TileEntityClockworkBearing;

import javax.annotation.Nullable;

/**
 * Clockwork bearing: a bearing that winds two hands (hour + minute) instead of
 * oneattachment. Paraphrased from the reference ClockworkBearingBlock: same
 * placement/assembly rules as the mechanical bearing (inherited), but the tile
 * entity tracks two contraption angles and a running flag.
 */
public class BlockClockworkBearing extends BlockBearingBase {
    public BlockClockworkBearing() {
        super();
        this.setRegistryName("clockwork_bearing");
        this.setUnlocalizedName("create.clockwork_bearing");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityClockworkBearing();
    }
}
