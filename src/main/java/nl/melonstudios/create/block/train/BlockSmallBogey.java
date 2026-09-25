package nl.melonstudios.create.block.train;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Small bogey: reference SMALL size (wheel radius 6.5/16). Additive split of
 * the existing single BOGEY concept — BlockBogey is untouched and keeps its
 * registry name, so old worlds and the station assembly keep working. Lower
 * frame (half-block tall) for light carriages.
 */
public class BlockSmallBogey extends BlockBogey {
    public BlockSmallBogey() {
        super();
        this.setRegistryName("small_bogey");
        this.setUnlocalizedName("create.small_bogey");
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0.1875, 0, 0.1875, 0.8125, 0.5, 0.8125);
    }
}
