package nl.melonstudios.create.block.fluid;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockKineticDirectionalBase;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.fluid.IFluidPipeConnectable;
import nl.melonstudios.create.tileentity.fluid.TileEntitySmartFluidPipe;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Smart fluid pipe: a straight filtered segment of the pipe graph.
 *
 * <p>Translated from the reference SmartFluidPipeBlock (never pasted): the
 * pipe carries one axis (the block's FACING axis), flow is allowed only
 * along that axis, and a ghost fluid filter decides which fluid may pass
 * (reference {@code canPullFluidFrom} + filter test). The face-attached
 * placement of the reference collapses to a plain directional block in
 * 1.12: FACING stores the run direction, one of six states.</p>
 *
 * <p>Filtering itself lives in {@link TileEntitySmartFluidPipe}, which
 * reuses the allow-list hook the pipe network already consults
 * ({@code matchesFilter} on every walk). Right-click with a fluid
 * container sets the filter to the contained fluid; empty hand clears.</p>
 */
@SuppressWarnings("deprecation")
public class BlockSmartFluidPipe extends Block implements IFluidPipeConnectable {
    private static final double C0 = 0.25;
    private static final double C1 = 0.75;

    public static final AxisAlignedBB CORE_BOX =
            new AxisAlignedBB(C0, C0, C0, C1, C1, C1);
    public static final AxisAlignedBB ARM_X_BOX =
            new AxisAlignedBB(0.0, C0, C0, 1.0, C1, C1);
    public static final AxisAlignedBB ARM_Y_BOX =
            new AxisAlignedBB(C0, 0.0, C0, C1, 1.0, C1);
    public static final AxisAlignedBB ARM_Z_BOX =
            new AxisAlignedBB(C0, C0, 0.0, C1, C1, 1.0);

    public BlockSmartFluidPipe() {
        super(Material.IRON);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(3.0F);
        this.setResistance(BlockProperties.IRON_RESISTANCE);
        this.setHarvestLevel("pickaxe", 1);
        this.setRegistryName("smart_fluid_pipe");
        this.setUnlocalizedName("create.smart_fluid_pipe");
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(BlockKineticDirectionalBase.FACING, EnumFacing.NORTH));
    }

    /** Reference {@code getPipeAxis}: the run direction of this segment. */
    public static EnumFacing.Axis getPipeAxis(IBlockState state) {
        return state.getValue(BlockKineticDirectionalBase.FACING).getAxis();
    }

    /** Reference {@code isOpenAt}: open only along the run axis. */
    public static boolean isOpenAt(IBlockState state, EnumFacing side) {
        return side.getAxis() == getPipeAxis(state);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BlockKineticDirectionalBase.FACING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(BlockKineticDirectionalBase.FACING).getIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(BlockKineticDirectionalBase.FACING, EnumFacing.VALUES[(meta & 7) % 6]);
    }

    /** The run follows the face clicked at placement, like the glass pipe. */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(BlockKineticDirectionalBase.FACING, facing);
    }

    /**
     * Axis-gated graph membership: the segment joins the pipe network on
     * its two run faces. The fluid-type half of the reference rule is
     * enforced by the tile's filter during the network walk.
     */
    @Override
    public boolean canConnectPipe(World world, BlockPos pos, EnumFacing side) {
        return isOpenAt(world.getBlockState(pos), side);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (playerIn.isSpectator()) return false;
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntitySmartFluidPipe)) return false;
        ItemStack held = playerIn.getHeldItem(hand);
        if (worldIn.isRemote) return true;
        TileEntitySmartFluidPipe pipe = (TileEntitySmartFluidPipe) te;
        if (held.isEmpty()) {
            if (playerIn.isSneaking()) pipe.setFluidFilter(null, ItemStack.EMPTY);
            return true;
        }
        FluidStack contained = FluidUtil.getFluidContained(held);
        if (contained != null) {
            pipe.setFluidFilter(contained.getFluid(), held);
            return true;
        }
        return false;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    @Nullable
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntitySmartFluidPipe();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        worldIn.removeTileEntity(pos);
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos,
                                Block blockIn, BlockPos fromPos) {
        worldIn.notifyBlockUpdate(pos, state, state, 3);
    }

    //region shapes: hub plus the run-axis arm
    public static AxisAlignedBB armBoxFor(IBlockState state) {
        switch (getPipeAxis(state)) {
            case X:
                return ARM_X_BOX;
            case Y:
                return ARM_Y_BOX;
            case Z:
            default:
                return ARM_Z_BOX;
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return armBoxFor(state);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos,
                                      AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
                                      @Nullable Entity entityIn, boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, CORE_BOX);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, armBoxFor(state));
    }
    //endregion

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    //region not a full block
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
