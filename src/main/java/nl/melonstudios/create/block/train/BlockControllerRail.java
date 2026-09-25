package nl.melonstudios.create.block.train;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;

/**
 * Controller rail: powered rail with a fixed drive direction. While it
 * receives redstone it accelerates carts along its pointing vector, scaled
 * by signal strength (0.02 base + 0.01 per power level, reference
 * speed-controlled push); unpowered it brakes gently. BACKWARDS flips the
 * drive direction (wrench toggles). Supports flats and ascents.
 */
@SuppressWarnings("deprecation")
public class BlockControllerRail extends BlockRailBase implements IWrenchable {
    public static final PropertyEnum<EnumRailDirection> SHAPE = PropertyEnum.create("shape",
            EnumRailDirection.class, EnumRailDirection.NORTH_SOUTH, EnumRailDirection.EAST_WEST,
            EnumRailDirection.ASCENDING_EAST, EnumRailDirection.ASCENDING_WEST,
            EnumRailDirection.ASCENDING_NORTH, EnumRailDirection.ASCENDING_SOUTH);
    public static final PropertyBool BACKWARDS = PropertyBool.create("backwards");

    public BlockControllerRail() {
        super(true);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(0.7F);
        this.setResistance(0.7F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(ItemInit.TAB_CREATE);
        this.setRegistryName("controller_rail");
        this.setUnlocalizedName("create.controller_rail");
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(SHAPE, EnumRailDirection.NORTH_SOUTH)
                .withProperty(BACKWARDS, false));
    }

    @Override
    public PropertyEnum<EnumRailDirection> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, SHAPE, BACKWARDS);
    }

    private static int shapeIndex(EnumRailDirection shape) {
        switch (shape) {
            case EAST_WEST: return 1;
            case ASCENDING_EAST: return 2;
            case ASCENDING_WEST: return 3;
            case ASCENDING_NORTH: return 4;
            case ASCENDING_SOUTH: return 5;
            default: return 0;
        }
    }

    private static EnumRailDirection shapeFromIndex(int i) {
        switch (i % 6) {
            case 1: return EnumRailDirection.EAST_WEST;
            case 2: return EnumRailDirection.ASCENDING_EAST;
            case 3: return EnumRailDirection.ASCENDING_WEST;
            case 4: return EnumRailDirection.ASCENDING_NORTH;
            case 5: return EnumRailDirection.ASCENDING_SOUTH;
            default: return EnumRailDirection.NORTH_SOUTH;
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(SHAPE, shapeFromIndex(meta & 7))
                .withProperty(BACKWARDS, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return shapeIndex(state.getValue(SHAPE)) | (state.getValue(BACKWARDS) ? 8 : 0);
    }

    /** Drive vector: west on EW, north on NS, flipped by BACKWARDS. */
    public static Vec3i driveVector(IBlockState state) {
        EnumRailDirection shape = state.getValue(SHAPE);
        boolean ew = shape == EnumRailDirection.EAST_WEST
                || shape == EnumRailDirection.ASCENDING_EAST
                || shape == EnumRailDirection.ASCENDING_WEST;
        EnumFacing dir = ew ? EnumFacing.WEST : EnumFacing.NORTH;
        if (state.getValue(BACKWARDS)) dir = dir.getOpposite();
        return dir.getDirectionVec();
    }

    @Override
    public void onMinecartPass(World world, EntityMinecart cart, BlockPos pos) {
        if (world.isRemote) return;
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockControllerRail)) return;
        int power = world.getRedstonePowerFromNeighbors(pos);
        if (power > 0) {
            Vec3i drive = driveVector(state);
            double accel = 0.02 + 0.01 * power;
            cart.motionX += drive.getX() * accel;
            cart.motionZ += drive.getZ() * accel;
            if (state.getValue(SHAPE).isAscending()) cart.motionY += accel * 0.5;
            cart.velocityChanged = true;
        } else {
            cart.motionX *= 0.96;
            cart.motionZ *= 0.96;
        }
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY,
                             float hitZ) {
        world.setBlockState(pos, state.withProperty(BACKWARDS, !state.getValue(BACKWARDS)), 3);
        return true;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
