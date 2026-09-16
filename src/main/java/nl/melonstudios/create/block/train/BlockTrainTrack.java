package nl.melonstudios.create.block.train;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Train track: vanilla-rail-compatible base (so minecarts and pathing work)
 * with Create styling. Curves/switches come from vanilla rail shapes;
 * stations/bogeys/carriages build on top.
 */
public class BlockTrainTrack extends BlockRailBase {
    public static final PropertyEnum<EnumRailDirection> SHAPE = PropertyEnum.create("shape", EnumRailDirection.class);

    public BlockTrainTrack() {
        super(false);
        this.blockSoundType = SoundType.METAL;
        this.setUnlocalizedName("create.train_track");
        this.setDefaultState(this.blockState.getBaseState().withProperty(SHAPE, EnumRailDirection.NORTH_SOUTH));
    }

    @Override
    public PropertyEnum<EnumRailDirection> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, SHAPE);
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

    @Override
    public net.minecraft.block.material.MapColor getMapColor(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos) {
        return net.minecraft.block.material.MapColor.STONE;
    }
}
