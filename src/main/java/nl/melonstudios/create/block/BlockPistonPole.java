package nl.melonstudios.create.block;

import com.melonstudios.melonlib.misc.BlockStateProperties;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.BlockProperties;

@SuppressWarnings("deprecation")
public class BlockPistonPole extends Block {
    public static final PropertyEnum<EnumFacing.Axis> AXIS = BlockStateProperties.AXIS;

    public BlockPistonPole() {
        super(Material.ROCK, MapColor.WOOD);
        this.blockSoundType = SoundType.WOOD;

        this.blockHardness = BlockProperties.WOOD_HARDNESS;
        this.blockResistance = BlockProperties.WOOD_RESISTANCE;

        this.setHarvestLevel("axe", -1);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    //region this is not a full block
    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isTranslucent(IBlockState state) {
        return true;
    }

    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
    //endregion

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(AXIS, facing.getAxis());
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(AXIS)) {
            case X: return BlockProperties.POLE_X_AABB;
            case Y: return BlockProperties.POLE_Y_AABB;
            case Z: return BlockProperties.POLE_Z_AABB;
            default:throw new IllegalStateException("Unexpected value");
        }
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AXIS).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(AXIS, EnumFacing.Axis.values()[meta % 3]);
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        if (rot == Rotation.NONE || rot == Rotation.CLOCKWISE_180) return state;
        switch (state.getValue(AXIS)) {
            case X: return state.withProperty(AXIS, EnumFacing.Axis.Z);
            case Z: return state.withProperty(AXIS, EnumFacing.Axis.X);
            default:return state;
        }
    }

    @Override
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        EnumFacing.Axis axis = blockState.getValue(AXIS);
        if (side.getAxis() == axis) {
            IBlockState hi = blockAccess.getBlockState(pos.offset(side));
            if (hi == blockState) return false;
        }
        return super.shouldSideBeRendered(blockState, blockAccess, pos, side);
    }
}
