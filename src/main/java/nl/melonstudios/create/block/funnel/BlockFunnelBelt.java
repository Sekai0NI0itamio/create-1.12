package nl.melonstudios.create.block.funnel;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.BlockStateProperties;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.actor.BlockBeltBase;
import nl.melonstudios.create.block.state.EnumFunnelState;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.funnel.TileEntityFunnelBase;
import nl.melonstudios.create.tileentity.funnel.TileEntityFunnelWall;
import nl.melonstudios.create.tileentity.funnel.TileEntityFunnelWallAdvanced;
import nl.melonstudios.create.util.FunnelSets;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * Belt funnel: the funnel variant that sits ON a belt segment instead of a
 * depot/inventory. Ports the reference BeltFunnelBlock behaviour:
 * <ul>
 * <li>requires a belt below; when the belt goes missing it reverts to the
 * matching down-funnel, keeping powered + extracting state,</li>
 * <li>facing across the belt runs as PUSHING (extracting) or PULLING
 * (inserting); facing along the belt parks RETRACTED,</li>
 * <li>wrench toggles PUSHING/PULLING or RETRACTED/EXTENDED.</li>
 * </ul>
 * Meta layout (4 bits): facing(2) + mode(1) + powered(1). The mode bit
 * doubles as the shared funnel-TE extracting flag, exactly like the wall
 * funnel's funnel-state bit. The visible SHAPE refines the mode bit with the
 * belt geometry below (inline vs perpendicular) in getActualState.
 */
@SuppressWarnings("deprecation")
public class BlockFunnelBelt extends BlockFunnelBase implements IWrenchable {
    public static final PropertyDirection FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final PropertyEnum<Shape> SHAPE = PropertyEnum.create("shape", Shape.class);

    public enum Shape implements IStringSerializable {
        RETRACTED,
        EXTENDED,
        PULLING,
        PUSHING;

        @Override
        public String getName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public boolean isModeBit() {
            return this == EXTENDED || this == PUSHING;
        }
    }

    public BlockFunnelBelt(String set, boolean advanced) {
        super(set, advanced);
    }

    @Override
    protected void addStateProperties(List<IProperty<?>> properties) {
        super.addStateProperties(properties);
        properties.add(FACING);
        properties.add(SHAPE);
    }

    public static final AxisAlignedBB BOX_INLINE = AABB.create(0, 0, 0, 16, 10, 16);
    public static final AxisAlignedBB[] BOX_SIDE = {
            AABB.create(0, 0, 0, 16, 14, 8),
            AABB.create(8, 0, 0, 16, 14, 16),
            AABB.create(0, 0, 8, 16, 14, 16),
            AABB.create(0, 0, 0, 8, 14, 16)
    };

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        Shape shape = state.getValue(SHAPE);
        try {
            shape = shapeFor(state, source, pos);
        } catch (Exception ignored) {
        }
        if (shape == Shape.PULLING || shape == Shape.PUSHING) {
            return BOX_SIDE[state.getValue(FACING).getHorizontalIndex()];
        }
        return BOX_INLINE;
    }

    @Nullable
    @Override
    public TileEntityFunnelBase createNewTileEntity(World worldIn, int meta) {
        return this.isAdvanced ? new TileEntityFunnelWallAdvanced() : new TileEntityFunnelWall();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        // Mode bit seeds the shape; getActualState refines it against the
        // belt below. TE reads bit 2 back as the extracting flag.
        boolean mode = (meta & 0b0100) != 0;
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
                .withProperty(SHAPE, mode ? Shape.PUSHING : Shape.PULLING)
                .withProperty(POWERED, (meta & 0b1000) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex()
                | (state.getValue(SHAPE).isModeBit() ? 0b0100 : 0)
                | (state.getValue(POWERED) ? 0b1000 : 0);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state.withProperty(SHAPE, shapeFor(state, worldIn, pos));
    }

    /** Reference getShapeForPosition: perpendicular push/pull, inline parked. */
    public static Shape shapeFor(IBlockState state, IBlockAccess world, BlockPos pos) {
        boolean mode = state.getValue(SHAPE).isModeBit();
        EnumFacing facing = state.getValue(FACING);
        IBlockState below = world.getBlockState(pos.down());
        if (!(below.getBlock() instanceof BlockBeltBase)) {
            return mode ? Shape.PUSHING : Shape.PULLING;
        }
        EnumFacing.Axis axis = ((BlockBeltBase) below.getBlock()).getTransportAxis(below);
        if (facing.getAxis() == axis) {
            return mode ? Shape.EXTENDED : Shape.RETRACTED;
        }
        return mode ? Shape.PUSHING : Shape.PULLING;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                           float hitX, float hitY, float hitZ, int meta,
                                           EntityLivingBase placer, EnumHand hand) {
        EnumFacing side = facing.getAxis().isHorizontal() ? facing
                : (placer == null ? EnumFacing.NORTH : placer.getHorizontalFacing());
        return this.getDefaultState().withProperty(FACING, side)
                .withProperty(SHAPE, Shape.PULLING)
                .withProperty(POWERED, false);
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return super.canPlaceBlockAt(worldIn, pos) && isOnValidBelt(worldIn, pos);
    }

    public static boolean isOnValidBelt(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos.down()).getBlock() instanceof BlockBeltBase;
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!isOnValidBelt(worldIn, pos)) {
            // Reference updateShape: fall back to the matching down-funnel,
            // keeping powered + extracting state.
            int meta = this.getMetaFromState(state);
            boolean extracting = (meta & 0b0100) != 0;
            IBlockState parent = FunnelSets.get(this.set).getDown().getDefaultState()
                    .withProperty(BlockFunnelDown.FUNNEL_STATE,
                            extracting ? EnumFunnelState.EXTRACTING : EnumFunnelState.INSERTING)
                    .withProperty(POWERED, state.getValue(POWERED));
            Utils.setBlockTESafe(worldIn, pos, parent, 3);
            return;
        }
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side,
                              float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        // Toggle the mode bit: PULLING<->PUSHING across the belt,
        // RETRACTED<->EXTENDED along it.
        int meta = this.getMetaFromState(state);
        IBlockState next = this.getStateFromMeta(meta ^ 0b0100);
        Utils.setBlockTESafe(world, pos, next, 3);
        return true;
    }
}
