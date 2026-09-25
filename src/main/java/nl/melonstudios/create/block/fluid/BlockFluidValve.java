package nl.melonstudios.create.block.fluid;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticDirectionalBase;
import nl.melonstudios.create.tileentity.TileEntityFluidValve;
import nl.melonstudios.create.tileentity.fluid.IFluidPipeConnectable;

import javax.annotation.Nullable;

/**
 * Fluid valve: rotation-gated shutoff on a pipe run.
 * Translated from the reference FluidValveBlock (never pasted):
 * <ul>
 * <li>FACING is the shaft direction; the pipe axis is derived from it
 * (reference keeps both on the state, but 1.12 metadata only fits
 * FACING(6) + ENABLED(2) = 12 states, so the second axis is fixed:
 * shaft X -&gt; pipe Z, shaft Z -&gt; pipe X, shaft Y -&gt; pipe X).</li>
 * <li>Flow is open along the pipe axis only, both directions
 * (reference {@code canHaveFlowToward} is axis-based, not one-way).</li>
 * <li>Gating is by rotation, not redstone: ENABLED tracks whether the
 * shaft spins (reference pointer chases 1 iff speed &gt; 0).</li>
 * </ul>
 */
@SuppressWarnings("deprecation")
public class BlockFluidValve extends BlockKineticDirectionalBase implements ITileEntityProvider, IPipePassThrough, IFluidPipeConnectable {
    public static final PropertyBool ENABLED = PropertyBool.create("enabled");

    public BlockFluidValve() {
        super(Material.ROCK, MapColor.STONE);
        this.setRegistryName("fluid_valve");
        this.setUnlocalizedName("create.fluid_valve");
        this.blockSoundType = SoundType.STONE;
        this.setHardness(3.0F);
        this.setResistance(6.0F);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(ENABLED, false));
    }

    /**
     * Pipe axis derived from the shaft facing. Fixed rule (see class doc):
     * X-shaft crosses a Z pipe, anything else crosses an X pipe.
     */
    public static EnumFacing.Axis getPipeAxis(IBlockState state) {
        return state.getValue(FACING).getAxis() == EnumFacing.Axis.X
                ? EnumFacing.Axis.Z : EnumFacing.Axis.X;
    }

    /** Flow ends of the valve: both faces along the pipe axis. */
    public static boolean isOpenAt(IBlockState state, EnumFacing side) {
        return side.getAxis() == getPipeAxis(state);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ENABLED);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getIndex();
        if (state.getValue(ENABLED))
            meta |= 8;
        return meta;
    }

    @Override
    @Deprecated
    public IBlockState getStateFromMeta(int meta) {
        // Same guard as BlockKineticDirectionalBase: meta & 7 can exceed 5.
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.VALUES[(meta & 7) % 6])
                .withProperty(ENABLED, (meta & 8) != 0);
    }

    @Override
    public boolean isPipeOpenAt(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return isOpenAt(state, side);
    }

    /**
     * Join the sibling pipe graph ({@code FluidPipeNetwork}) only while
     * open: a shut valve splits the graph, mirroring the reference
     * {@code ValvePipeBehaviour}, which refuses pull while ENABLED is
     * false. The axis check mirrors {@link #isOpenAt}; note the sibling
     * graph walk ({@code FluidPipeNetwork.walk}) is side-blind, so an open
     * valve also relays shaft-side neighbours there, while direct
     * tank contact stays axis-gated via the TE valve faces.
     */
    @Override
    public boolean canConnectPipe(World world, BlockPos pos, EnumFacing side) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockFluidValve))
            return false;
        if (!state.getValue(ENABLED))
            return false;
        return side.getAxis() == getPipeAxis(state);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityFluidValve();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
