package nl.melonstudios.create.block.train;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Large bogey: reference LARGE size (wheel radius 12.5/16). Additive split
 * of the existing single BOGEY concept — BlockBogey is untouched and keeps
 * its registry name. Full-block-tall drive frame for heavy carriages.
 */
public class BlockLargeBogey extends BlockBogey {
    public BlockLargeBogey() {
        super();
        this.setRegistryName("large_bogey");
        this.setUnlocalizedName("create.large_bogey");
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0.0625, 0, 0.0625, 0.9375, 1.0, 0.9375);
    }
}
