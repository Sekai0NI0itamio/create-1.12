package nl.melonstudios.create.block.train;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Train track: vanilla-rail-compatible base (so minecarts and pathing work)
 * with Create styling. Curves/switches come from vanilla rail shapes;
 * stations/bogeys/carriages build on top.
 */
public class BlockTrainTrack extends BlockRailBase {
    public BlockTrainTrack() {
        super(false);
        this.blockSoundType = SoundType.METAL;
        this.setRegistryName("train_track");
        this.setUnlocalizedName("create.train_track");
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(SHAPE, EnumRailDirection.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(SHAPE).getMetadata();
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }

    public MapColor getMapColor(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos) {
        return MapColor.STONE;
    }
}
