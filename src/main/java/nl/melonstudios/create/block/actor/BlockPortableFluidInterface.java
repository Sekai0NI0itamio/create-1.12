package nl.melonstudios.create.block.actor;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.actor.TileEntityContraptionInterfaceBase;
import nl.melonstudios.create.tileentity.actor.TileEntityPortableFluidInterface;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;

/**
 * Portable fluid interface: the fluid counterpart of the storage interface.
 * Reference ({@code PortableStorageInterfaceBlock#forFluids}): a plain directional,
 * non-kinetic block (copper casing, axe-or-pickaxe harvest, 0-16-14 directional
 * shape, comparator signal while linked, redstone disables the link, zero stress).
 */
@SuppressWarnings("deprecation")
public class BlockPortableFluidInterface extends BlockDirectional implements ITileEntityProvider, IWrenchable {
    private static final AxisAlignedBB BOX_DOWN = new AxisAlignedBB(0.0, 2.0 / 16.0, 0.0, 1.0, 1.0, 1.0);
    private static final AxisAlignedBB BOX_UP = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 14.0 / 16.0, 1.0);
    private static final AxisAlignedBB BOX_NORTH = new AxisAlignedBB(0.0, 2.0 / 16.0, 2.0 / 16.0, 1.0, 1.0, 1.0);
    private static final AxisAlignedBB BOX_SOUTH = new AxisAlignedBB(0.0, 2.0 / 16.0, 0.0, 1.0, 1.0, 14.0 / 16.0);
    private static final AxisAlignedBB BOX_WEST = new AxisAlignedBB(2.0 / 16.0, 2.0 / 16.0, 0.0, 1.0, 1.0, 1.0);
    private static final AxisAlignedBB BOX_EAST = new AxisAlignedBB(0.0, 2.0 / 16.0, 0.0, 14.0 / 16.0, 1.0, 1.0);
    private static final AxisAlignedBB[] BOXES = {BOX_DOWN, BOX_UP, BOX_NORTH, BOX_SOUTH, BOX_WEST, BOX_EAST};

    public BlockPortableFluidInterface() {
        super(Material.ROCK);
        this.setSoundType(SoundType.STONE);
        this.fullBlock = false;
        this.translucent = true;

        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);

        this.setHarvestLevel("pickaxe", 0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || "axe".equals(type);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.VALUES[meta % 6]);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        // Reference: direction = nearest looking direction (all 6 sides);
        // shift inverts; stored FACING is the opposite of that direction.
        Vec3d look = placer.getLookVec();
        EnumFacing direction = EnumFacing.getFacingFromVector((float) look.x, (float) look.y, (float) look.z);
        if (placer.isSneaking()) direction = direction.getOpposite();
        return this.getDefaultState().withProperty(FACING, direction.getOpposite());
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityPortableFluidInterface();
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityContraptionInterfaceBase) {
            ((TileEntityContraptionInterfaceBase) te).neighbourChanged();
        }
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityContraptionInterfaceBase) {
            return ((TileEntityContraptionInterfaceBase) te).isConnected() ? 15 : 0;
        }
        return 0;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        EnumFacing facing = state.getValue(FACING);
        if (facing.getAxis() == side.getAxis()) return false;
        EnumFacing rotated = facing.rotateAround(side.getAxis());
        Utils.setBlockTESafe(world, pos, state.withProperty(FACING, rotated), 3);
        return true;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOXES[state.getValue(FACING).getIndex()];
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
}
