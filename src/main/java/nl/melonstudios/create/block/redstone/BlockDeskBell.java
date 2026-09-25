package nl.melonstudios.create.block.redstone;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.redstone.TileEntityDeskBell;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;

/**
 * Desk bell: right-click dings (bell-like pling) and emits full power for
 * 20 ticks, then releases. Translated from the reference press/unpress cycle.
 */
@SuppressWarnings("deprecation")
public class BlockDeskBell extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.1875, 0.0, 0.1875, 0.8125, 0.625, 0.8125);

    public BlockDeskBell() {
        super(Material.IRON, MapColor.GOLD);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(2.0F);
        this.setResistance(4.0F);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(ItemInit.TAB_CREATE);
        this.setRegistryName("desk_bell");
        this.setUnlocalizedName("create.desk_bell");
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.UP)
                .withProperty(POWERED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getFront(meta & 7))
                .withProperty(POWERED, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() | (state.getValue(POWERED) ? 8 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, facing);
    }

    private boolean canStay(World world, BlockPos pos, EnumFacing facing) {
        BlockPos support = pos.offset(facing.getOpposite());
        return world.getBlockState(support).getBlockFaceShape(world, support, facing) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return this.canStay(world, pos, side);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!this.canStay(world, pos, state.getValue(FACING))) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                   EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 0.6F, 1.8F);
        Utils.setBlockTESafe(world, pos, state.withProperty(POWERED, true), 3);
        this.notifyNeighbors(world, pos, state);
        TileEntityDeskBell bell = Utils.cast(world.getTileEntity(pos), TileEntityDeskBell.class);
        if (bell != null) bell.ding();
        return true;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side,
                             float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        EnumFacing next = EnumFacing.getFront((state.getValue(FACING).getIndex() + 1) % 6);
        if (this.canStay(world, pos, next)) {
            Utils.setBlockTESafe(world, pos, state.withProperty(FACING, next), 3);
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityDeskBell();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        world.removeTileEntity(pos);
        if (state.getValue(POWERED)) {
            this.notifyNeighbors(world, pos, state);
        }
    }

    public void notifyNeighbors(World world, BlockPos pos, IBlockState state) {
        world.notifyNeighborsOfStateChange(pos, this, false);
        world.notifyNeighborsOfStateChange(pos.offset(state.getValue(FACING).getOpposite()), this, false);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        return state.getValue(POWERED) && state.getValue(FACING) == side ? 15 : 0;
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos,
                                      @Nullable EnumFacing side) {
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
