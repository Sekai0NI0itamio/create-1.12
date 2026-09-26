package nl.melonstudios.create.block.fluid;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.fluid.IFluidPipeConnectable;
import nl.melonstudios.create.tileentity.fluid.TileEntityFluidPipe;
import nl.melonstudios.create.util.BlockProperties;

/**
 * Glass (straight, windowed) fluid pipe.
 *
 * <p>Ports the reference {@code AxisPipeBlock} / {@code GlassFluidPipeBlock}
 * idea: unlike the standard pipe this variant never bends — it is a
 * straight pole locked to one axis ({@code X}, {@code Y} or {@code Z},
 * packed into metadata like a log) and only ever connects along that
 * axis. Flow visibility and branch-free runs are the point; anything
 * needing a bend uses the standard pipe.</p>
 *
 * <p>Placement rule: the axis follows the clicked face, so placing
 * against a tank or machine face points the pipe straight at it.
 * Wrenching cycles the axis X -&gt; Y -&gt; Z.</p>
 */
@SuppressWarnings("deprecation")
public class BlockGlassFluidPipe extends Block implements IFluidPipeConnectable, IWrenchable {
    public static final PropertyEnum<EnumFacing.Axis> AXIS =
            PropertyEnum.create("axis", EnumFacing.Axis.class);

    private static final double C0 = 0.25;
    private static final double C1 = 0.75;

    public static final AxisAlignedBB POLE_X_BOX =
            new AxisAlignedBB(0.0, C0, C0, 1.0, C1, C1);
    public static final AxisAlignedBB POLE_Y_BOX =
            new AxisAlignedBB(C0, 0.0, C0, C1, 1.0, C1);
    public static final AxisAlignedBB POLE_Z_BOX =
            new AxisAlignedBB(C0, C0, 0.0, C1, C1, 1.0);

    public BlockGlassFluidPipe() {
        super(Material.GLASS);
        this.setHardness(3.0F);
        this.setResistance(BlockProperties.IRON_RESISTANCE);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("glass_fluid_pipe");
        this.setUnlocalizedName("create.glass_fluid_pipe");
        this.setDefaultState(this.blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AXIS).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing.Axis[] axes = EnumFacing.Axis.values();
        return this.getDefaultState().withProperty(AXIS, axes[meta % axes.length]);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(AXIS, facing.getAxis());
    }

    /** Pure state check used for the pipe-to-pipe handshake (no world
     * access, so it is safe from rendering code paths). */
    public static boolean isOpenAlong(IBlockState state, EnumFacing side) {
        return state.getBlock() instanceof BlockGlassFluidPipe
                && side.getAxis() == state.getValue(AXIS);
    }

    @Override
    public boolean canConnectPipe(World world, BlockPos pos, EnumFacing side) {
        IBlockState state = world.getBlockState(pos);
        return isOpenAlong(state, side) && BlockFluidPipe.canConnectTo(world, pos, side);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityFluidPipe();
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

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state,
                              EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        EnumFacing.Axis[] axes = EnumFacing.Axis.values();
        EnumFacing.Axis next = axes[(state.getValue(AXIS).ordinal() + 1) % axes.length];
        world.setBlockState(pos, state.withProperty(AXIS, next), 3);
        return true;
    }

    public static AxisAlignedBB poleBox(EnumFacing.Axis axis) {
        switch (axis) {
            case X: return POLE_X_BOX;
            case Z: return POLE_Z_BOX;
            case Y:
            default: return POLE_Y_BOX;
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return poleBox(state.getValue(AXIS));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    //region not a full block
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
