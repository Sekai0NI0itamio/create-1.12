package nl.melonstudios.create.block.fluid;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.fluid.IFluidPipeConnectable;
import nl.melonstudios.create.tileentity.fluid.TileEntityFluidPipe;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Standard copper fluid pipe (Create backport, 1.12.2 idioms).
 *
 * <p>Reference behavior ported (paraphrased from the modern
 * {@code FluidPipeBlock}): a pipe opens one arm per side that accepts a
 * fluid connection, renders a rim ring where an open arm faces a
 * non-pipe, and when exactly one side connects it also opens the
 * opposite side so the run reads as a straight stub.</p>
 *
 * <p>1.12 meta scheme: six open sides plus six rim flags cannot fit in 4
 * bits of metadata, so — like vanilla fences ({@code BlockFence}) and
 * the backport funnel wall's {@code TALL} flag — nothing connection
 * related is stored. {@code getMetaFromState} always returns 0 and every
 * flag is derived live in {@code getActualState}. Rendering uses a Forge
 * multipart blockstate (core always, one arm part per open side, one rim
 * part per machine-facing open side).</p>
 *
 * <p>Connection rules ported from the reference:</p>
 * <ul>
 * <li>pipe-to-pipe always links (standard and encased accept every side;
 * glass only along its axis, decided by a direct state check so no world
 * recursion is needed);</li>
 * <li>pipes link to any neighbour tile exposing the Forge fluid-handler
 * capability on the touched face — tanks, machines, pumps, spouts and
 * drains are all covered by that one check, present and future;</li>
 * <li>unloaded neighbours (server side) never connect.</li>
 * </ul>
 *
 * <p>Query API for the transfer worker: {@link #isPipe},
 * {@link #canConnectTo}, {@link #isOpenAt} and {@link #getOpenSides}.
 * This block hosts a {@link TileEntityFluidPipe} relay tile; the
 * tile's tick is what drives the network walks, so a graph with no
 * tiles would never move fluid.</p>
 */
@SuppressWarnings("deprecation")
public class BlockFluidPipe extends Block implements IFluidPipeConnectable {
    public static final PropertyBool DOWN = PropertyBool.create("down");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool EAST = PropertyBool.create("east");

    public static final PropertyBool RIM_DOWN = PropertyBool.create("rim_down");
    public static final PropertyBool RIM_UP = PropertyBool.create("rim_up");
    public static final PropertyBool RIM_NORTH = PropertyBool.create("rim_north");
    public static final PropertyBool RIM_SOUTH = PropertyBool.create("rim_south");
    public static final PropertyBool RIM_WEST = PropertyBool.create("rim_west");
    public static final PropertyBool RIM_EAST = PropertyBool.create("rim_east");

    private static final double C0 = 0.25;
    private static final double C1 = 0.75;

    public static final AxisAlignedBB CORE_BOX =
            new AxisAlignedBB(C0, C0, C0, C1, C1, C1);
    public static final AxisAlignedBB ARM_DOWN_BOX =
            new AxisAlignedBB(C0, 0.0, C0, C1, C0, C1);
    public static final AxisAlignedBB ARM_UP_BOX =
            new AxisAlignedBB(C0, C1, C0, C1, 1.0, C1);
    public static final AxisAlignedBB ARM_NORTH_BOX =
            new AxisAlignedBB(C0, C0, 0.0, C1, C1, C0);
    public static final AxisAlignedBB ARM_SOUTH_BOX =
            new AxisAlignedBB(C0, C0, C1, C1, C1, 1.0);
    public static final AxisAlignedBB ARM_WEST_BOX =
            new AxisAlignedBB(0.0, C0, C0, C0, C1, C1);
    public static final AxisAlignedBB ARM_EAST_BOX =
            new AxisAlignedBB(C1, C0, C0, 1.0, C1, C1);

    public BlockFluidPipe() {
        super(Material.IRON);
        this.setHardness(3.0F);
        this.setResistance(BlockProperties.IRON_RESISTANCE);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("fluid_pipe");
        this.setUnlocalizedName("create.fluid_pipe");
    }

    //region state: meta carries nothing, connections are derived live
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this,
                DOWN, UP, NORTH, SOUTH, WEST, EAST,
                RIM_DOWN, RIM_UP, RIM_NORTH, RIM_SOUTH, RIM_WEST, RIM_EAST);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        if (worldIn == null || pos == null) return state;
        boolean[] open = computeOpens(worldIn, pos);
        return state.withProperty(DOWN, open[EnumFacing.DOWN.getIndex()])
                .withProperty(UP, open[EnumFacing.UP.getIndex()])
                .withProperty(NORTH, open[EnumFacing.NORTH.getIndex()])
                .withProperty(SOUTH, open[EnumFacing.SOUTH.getIndex()])
                .withProperty(WEST, open[EnumFacing.WEST.getIndex()])
                .withProperty(EAST, open[EnumFacing.EAST.getIndex()])
                .withProperty(RIM_DOWN, open[EnumFacing.DOWN.getIndex()] && shouldDrawRim(worldIn, pos, EnumFacing.DOWN))
                .withProperty(RIM_UP, open[EnumFacing.UP.getIndex()] && shouldDrawRim(worldIn, pos, EnumFacing.UP))
                .withProperty(RIM_NORTH, open[EnumFacing.NORTH.getIndex()] && shouldDrawRim(worldIn, pos, EnumFacing.NORTH))
                .withProperty(RIM_SOUTH, open[EnumFacing.SOUTH.getIndex()] && shouldDrawRim(worldIn, pos, EnumFacing.SOUTH))
                .withProperty(RIM_WEST, open[EnumFacing.WEST.getIndex()] && shouldDrawRim(worldIn, pos, EnumFacing.WEST))
                .withProperty(RIM_EAST, open[EnumFacing.EAST.getIndex()] && shouldDrawRim(worldIn, pos, EnumFacing.EAST));
    }
    //endregion

    //region connection rules (ported reference behavior, 1.12 capability idiom)
    /** True for every pipe block in this package (standard, glass, encased). */
    public static boolean isPipe(@Nullable Block block) {
        return block instanceof IFluidPipeConnectable;
    }

    /** True when the blockstate belongs to a pipe block. */
    public static boolean isPipe(@Nullable IBlockState state) {
        return state != null && isPipe(state.getBlock());
    }

    /**
     * May a pipe at {@code selfPos} connect toward {@code side}?
     * Pipe neighbours link block-to-block; anything else must expose the
     * fluid-handler capability on the touched face.
     */
    public static boolean canConnectTo(IBlockAccess world, BlockPos selfPos, EnumFacing side) {
        if (world instanceof World && !((World) world).isBlockLoaded(selfPos.offset(side))) return false;
        BlockPos neighbourPos = selfPos.offset(side);
        IBlockState neighbourState;
        try {
            neighbourState = world.getBlockState(neighbourPos);
        } catch (Exception e) {
            return false;
        }
        Block neighbour = neighbourState.getBlock();
        if (neighbour instanceof BlockFluidPipe) return true;
        if (neighbour instanceof BlockEncasedFluidPipe) return true;
        if (neighbour instanceof BlockGlassFluidPipe) {
            return BlockGlassFluidPipe.isOpenAlong(neighbourState, side.getOpposite());
        }
        if (neighbour instanceof IFluidPipeConnectable) return true;
        TileEntity te;
        try {
            te = world.getTileEntity(neighbourPos);
        } catch (Exception e) {
            return false;
        }
        return te != null && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite());
    }

    /** Raw per-side openness for one position, with the reference
     * single-connection rule applied: exactly one link also opens the
     * opposite side so the pipe reads as a straight stub. */
    private static boolean[] computeOpens(IBlockAccess world, BlockPos pos) {
        boolean[] open = new boolean[6];
        int count = 0;
        EnumFacing only = null;
        for (EnumFacing side : EnumFacing.VALUES) {
            boolean connects = false;
            try {
                connects = canConnectTo(world, pos, side);
            } catch (Exception e) {
                connects = false;
            }
            open[side.getIndex()] = connects;
            if (connects) {
                count++;
                only = side;
            }
        }
        if (count == 1 && only != null) {
            open[only.getOpposite().getIndex()] = true;
        }
        return open;
    }

    /** True when the arm toward {@code side} is rendered (and, by the
     * handshake in {@link #canConnectTo}, accepted on both ends). */
    public static boolean isOpenAt(IBlockAccess world, BlockPos pos, EnumFacing side) {
        return computeOpens(world, pos)[side.getIndex()];
    }

    /** All open sides at a position, for the transfer worker's graph walk. */
    public static EnumSet<EnumFacing> getOpenSides(IBlockAccess world, BlockPos pos) {
        EnumSet<EnumFacing> set = EnumSet.noneOf(EnumFacing.class);
        boolean[] open = computeOpens(world, pos);
        for (EnumFacing side : EnumFacing.VALUES) {
            if (open[side.getIndex()]) set.add(side);
        }
        return set;
    }

    /** Rim ring (end cap) where an open arm faces a machine or dead end
     * rather than another pipe — ported from the reference rim rule. */
    public static boolean shouldDrawRim(IBlockAccess world, BlockPos pos, EnumFacing side) {
        IBlockState neighbour;
        try {
            neighbour = world.getBlockState(pos.offset(side));
        } catch (Exception e) {
            return true;
        }
        return !isPipe(neighbour);
    }

    @Override
    public boolean canConnectPipe(World world, BlockPos pos, EnumFacing side) {
        return isOpenAt(world, pos, side);
    }
    //endregion

    //region relay tile: one TileEntityFluidPipe per pipe drives the network
    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    @Nullable
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
    //endregion

    //region shapes: core hub plus one arm box per open side
    public static AxisAlignedBB armBox(EnumFacing side) {
        switch (side) {
            case DOWN: return ARM_DOWN_BOX;
            case UP: return ARM_UP_BOX;
            case NORTH: return ARM_NORTH_BOX;
            case SOUTH: return ARM_SOUTH_BOX;
            case WEST: return ARM_WEST_BOX;
            case EAST:
            default: return ARM_EAST_BOX;
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        IBlockState actual;
        try {
            actual = this.getActualState(state, source, pos);
        } catch (Exception e) {
            return CORE_BOX;
        }
        double minX = C0, minY = C0, minZ = C0, maxX = C1, maxY = C1, maxZ = C1;
        if (actual.getValue(DOWN)) minY = 0.0;
        if (actual.getValue(UP)) maxY = 1.0;
        if (actual.getValue(NORTH)) minZ = 0.0;
        if (actual.getValue(SOUTH)) maxZ = 1.0;
        if (actual.getValue(WEST)) minX = 0.0;
        if (actual.getValue(EAST)) maxX = 1.0;
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos,
                                      AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
                                      @Nullable Entity entityIn, boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, CORE_BOX);
        boolean[] open = computeOpens(worldIn, pos);
        for (EnumFacing side : EnumFacing.VALUES) {
            if (open[side.getIndex()]) {
                addCollisionBoxToList(pos, entityBox, collidingBoxes, armBox(side));
            }
        }
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
