package nl.melonstudios.create.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.tileentity.TileEntityChainDrive;

import javax.annotation.Nullable;

/**
 * Backport of the reference ChainDriveBlock (translated, MIT).
 * A chain drive relays rotation along a straight chain run between two
 * shafts. The run direction is always perpendicular to the rotation axis;
 * this backport fixes one run direction per axis (Y runs along X, X runs
 * along Z, Z runs along X) so the state fits 1.12's 4-bit metadata.
 * The PART trait (start/middle/end/none) is derived from neighbours via
 * getActualState and costs no metadata.
 */
public class BlockChainDrive extends BlockKineticRotatedPillarBase implements ITileEntityProvider {
    public static final PropertyEnum<Part> PART = PropertyEnum.create("part", Part.class);

    public BlockChainDrive(Material blockMaterialIn, MapColor blockMapColorIn) {
        super(blockMaterialIn, blockMapColorIn);

        this.setRegistryName("chain_drive");
        this.setUnlocalizedName("create.chain_drive");

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(AXIS, EnumFacing.Axis.Y)
                .withProperty(PART, Part.NONE));
    }

    /** Chain run direction for a given rotation axis (always perpendicular). */
    public static EnumFacing.Axis getConnectionAxis(IBlockState state) {
        return getConnectionAxis(state.getValue(AXIS));
    }

    public static EnumFacing.Axis getConnectionAxis(EnumFacing.Axis rotationAxis) {
        switch (rotationAxis) {
            case X:
                return EnumFacing.Axis.Z;
            case Z:
                return EnumFacing.Axis.X;
            case Y:
            default:
                return EnumFacing.Axis.X;
        }
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS, PART);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        EnumFacing.Axis axis = state.getValue(AXIS);
        EnumFacing.Axis run = getConnectionAxis(axis);
        boolean neg = isChainNeighbour(worldIn, pos, run, -1, axis);
        boolean posSide = isChainNeighbour(worldIn, pos, run, 1, axis);
        Part part;
        if (neg && posSide) part = Part.MIDDLE;
        else if (posSide) part = Part.START;
        else if (neg) part = Part.END;
        else part = Part.NONE;
        return state.withProperty(PART, part);
    }

    private static boolean isChainNeighbour(IBlockAccess world, BlockPos pos,
                                            EnumFacing.Axis run, int dir, EnumFacing.Axis axis) {
        EnumFacing facing = EnumFacing.getFacingFromAxis(
                dir > 0 ? EnumFacing.AxisDirection.POSITIVE : EnumFacing.AxisDirection.NEGATIVE, run);
        IBlockState neighbour = world.getBlockState(pos.offset(facing));
        return neighbour.getBlock() instanceof BlockChainDrive
                && neighbour.getValue(AXIS) == axis;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityChainDrive();
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side.getAxis() == state.getValue(AXIS);
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        // Pure static models (chain links baked in); no TESR registration needed.
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return Block.FULL_BLOCK_AABB;
    }

    public enum Part implements IStringSerializable {
        START("start"),
        MIDDLE("middle"),
        END("end"),
        NONE("none");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }
}
