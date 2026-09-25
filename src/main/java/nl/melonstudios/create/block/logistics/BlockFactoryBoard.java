package nl.melonstudios.create.block.logistics;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.tileentity.logistics.TileEntityFactoryBoard;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Factory board: wall-mounted gauge panel with 4 filter slots.
 * Right-click with an item to pin its ghost into the first free slot
 * (nothing is consumed); sneak-right-click wipes the board.
 * Emits redstone while any slot is pinned and reports fill level
 * to comparators. Backport of the reference panel display side only;
 * the logistics-network promise/stock wiring is out of scope here.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class BlockFactoryBoard extends Block implements ITileEntityProvider {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockFactoryBoard() {
        super(Material.IRON, MapColor.IRON);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(3.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(ItemInit.TAB_CREATE);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(POWERED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(POWERED)) meta |= 4;
        return meta;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
                .withProperty(POWERED, (meta & 4) != 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityFactoryBoard();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOptimizedBase) ((TileEntityOptimizedBase) te).destroy();
        if (this.hasTileEntity(state)) worldIn.removeTileEntity(pos);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        switch (facing) {
            case NORTH:
                return new AxisAlignedBB(0, 2 / 16.0, 14 / 16.0, 1, 14 / 16.0, 1);
            case SOUTH:
                return new AxisAlignedBB(0, 2 / 16.0, 0, 1, 14 / 16.0, 2 / 16.0);
            case WEST:
                return new AxisAlignedBB(14 / 16.0, 2 / 16.0, 0, 1, 14 / 16.0, 1);
            case EAST:
            default:
                return new AxisAlignedBB(0, 2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 1);
        }
    }

    private void refreshPower(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        boolean lit = te instanceof TileEntityFactoryBoard && ((TileEntityFactoryBoard) te).filledSlots() > 0;
        if (state.getValue(POWERED) != lit) {
            worldIn.setBlockState(pos, state.withProperty(POWERED, lit), 3);
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        return Boolean.TRUE.equals(BlockKineticBase.withTEDo(worldIn, pos, TileEntityFactoryBoard.class, (te) -> {
            ItemStack held = playerIn.getHeldItem(hand);
            if (playerIn.isSneaking()) {
                te.clear();
                worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 0.5F, 1.0F);
                te.sync();
                this.refreshPower(worldIn, pos, state);
                return true;
            }
            if (held.isEmpty() || te.filledSlots() >= 4) return false;
            ItemStack ghost = held.copy();
            ghost.setCount(1);
            te.pinGhost(ghost);
            worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 0.5F, 1.0F);
            te.sync();
            this.refreshPower(worldIn, pos, state);
            return true;
        }));
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityFactoryBoard)) return 0;
        int filled = ((TileEntityFactoryBoard) te).filledSlots();
        if (filled <= 0) return 0;
        return Math.max(1, Math.min(15, filled * 15 / 4));
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
