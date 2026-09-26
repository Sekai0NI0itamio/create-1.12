package nl.melonstudios.create.block.actor;

import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.actor.TileEntityPlough;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;

/**
 * Standalone mechanical plough. Same behavior values as the AUTO_FARM plough
 * variant (12px tall blade, horizontal facing, tills plowable soil into
 * farmland while moving); split out additively, BlockAutoFarm untouched.
 */
@SuppressWarnings("deprecation")
public class BlockMechanicalPlough extends BlockHorizontal implements ITileEntityProvider {
    public BlockMechanicalPlough() {
        super(Material.ROCK, MapColor.IRON);
        this.setSoundType(SoundType.STONE);
        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("mechanical_plough");
        this.setUnlocalizedName("create.mechanical_plough");
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityPlough();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.HORIZONTALS[meta & 3]);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        if (facing.getAxis() == EnumFacing.Axis.Y) {
            return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
        }
        return this.getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withProperty(FACING, mirrorIn.mirror(state.getValue(FACING)));
    }

    private static final AxisAlignedBB[] BOXES = {
            AABB.create(0.0, 0.0, 0.0, 16.0, 12.0, 15.0),
            AABB.create(1.0, 0.0, 0.0, 16.0, 12.0, 16.0),
            AABB.create(0.0, 0.0, 1.0, 16.0, 12.0, 16.0),
            AABB.create(0.0, 0.0, 0.0, 15.0, 12.0, 16.0)
    };

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOXES[state.getValue(FACING).getHorizontalIndex()];
    }

    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
}
