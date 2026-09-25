package nl.melonstudios.create.block.redstone;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;

/**
 * Redstone contact: wireless redstone over a gap. Own-words summary of the
 * reference behavior: two contacts facing each other (nose to nose, up to 8
 * blocks apart, nothing solid between) share their signal; either side going
 * high pulls the other high, which then outputs 15 to every side except its
 * own nose. No tile entity in the reference; the state carries it all.
 */
@SuppressWarnings("deprecation")
public class BlockRedstoneContact extends BlockDirectional implements IWrenchable {
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    /** How far apart a matching pair may sit (reference range). */
    public static final int RANGE = 8;

    private static final AxisAlignedBB BOX_DOWN = new AxisAlignedBB(0.0, 2.0 / 16.0, 0.0, 1.0, 1.0, 1.0);
    private static final AxisAlignedBB BOX_UP = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 14.0 / 16.0, 1.0);
    private static final AxisAlignedBB BOX_NORTH = new AxisAlignedBB(0.0, 2.0 / 16.0, 2.0 / 16.0, 1.0, 1.0, 1.0);
    private static final AxisAlignedBB BOX_SOUTH = new AxisAlignedBB(0.0, 2.0 / 16.0, 0.0, 1.0, 1.0, 14.0 / 16.0);
    private static final AxisAlignedBB BOX_WEST = new AxisAlignedBB(2.0 / 16.0, 2.0 / 16.0, 0.0, 1.0, 1.0, 1.0);
    private static final AxisAlignedBB BOX_EAST = new AxisAlignedBB(0.0, 2.0 / 16.0, 0.0, 14.0 / 16.0, 1.0, 1.0);
    private static final AxisAlignedBB[] BOXES = {BOX_DOWN, BOX_UP, BOX_NORTH, BOX_SOUTH, BOX_WEST, BOX_EAST};

    public BlockRedstoneContact() {
        super(Material.ROCK);
        this.setSoundType(SoundType.STONE);
        this.fullBlock = false;
        this.translucent = true;
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(POWERED, false));
        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(ItemInit.TAB_CREATE);
        this.setRegistryName("redstone_contact");
        this.setUnlocalizedName("create.redstone_contact");
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() | (state.getValue(POWERED) ? 8 : 0);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.VALUES[(meta & 7) % 6])
                .withProperty(POWERED, (meta & 8) != 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        Vec3d look = placer.getLookVec();
        EnumFacing direction = EnumFacing.getFacingFromVector((float) look.x, (float) look.y, (float) look.z);
        if (placer.isSneaking()) direction = direction.getOpposite();
        return this.getDefaultState().withProperty(FACING, direction.getOpposite()).withProperty(POWERED, false);
    }

    /** Partner position when a nose-to-nose contact sits in range, else null. */
    @Nullable
    public static BlockPos findPartner(World world, BlockPos pos, IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        for (int i = 1; i <= RANGE; i++) {
            BlockPos at = pos.offset(facing, i);
            IBlockState there = world.getBlockState(at);
            if (there.getBlock() instanceof BlockRedstoneContact) {
                EnumFacing other = there.getValue(FACING);
                return other == facing.getOpposite() ? at : null;
            }
            if (there.isOpaqueCube()) return null;
        }
        return null;
    }

    /** True when a nose-to-nose partner with a live signal is in range. */
    public static boolean hasValidContact(World world, BlockPos pos, IBlockState state) {
        BlockPos partner = findPartner(world, pos, state);
        if (partner == null) return false;
        IBlockState there = world.getBlockState(partner);
        return there.getValue(POWERED) || world.isBlockPowered(partner);
    }

    private void refresh(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) return;
        boolean fed = world.isBlockPowered(pos) || hasValidContact(world, pos, state);
        if (fed != state.getValue(POWERED)) {
            world.setBlockState(pos, state.withProperty(POWERED, fed), 3);
            // The gap blocks normal neighbor updates, so poke the partner directly.
            BlockPos partner = findPartner(world, pos, state);
            if (partner != null) {
                IBlockState partnerState = world.getBlockState(partner);
                if (partnerState.getBlock() instanceof BlockRedstoneContact) {
                    ((BlockRedstoneContact) partnerState.getBlock()).refresh(world, partner, partnerState);
                }
            }
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        this.refresh(worldIn, pos, state);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        this.refresh(worldIn, pos, state);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        if (!state.getValue(POWERED)) return 0;
        return side == state.getValue(FACING) ? 0 : 15;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return this.getWeakPower(state, world, pos, side);
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
