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
import net.minecraft.entity.item.EntityItem;
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
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.redstone.TileEntityPlacard;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;

/**
 * Placard: a wall plate that displays one item. Right-click with an empty
 * plate to mount one copy of the held stack; offering a matching stack
 * while a display is mounted pulses full power for 19 ticks. Punching the
 * plate returns the display. The reference floor/ceiling mount and filter
 * matching collapse to a six-side mount plus same-item check because 1.12
 * meta cannot hold the full face set.
 */
@SuppressWarnings("deprecation")
public class BlockPlacard extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockPlacard() {
        super(Material.WOOD, MapColor.WOOD);
        this.blockSoundType = SoundType.WOOD;
        this.setHardness(1.0F);
        this.setResistance(3.0F);
        this.setHarvestLevel("axe", 0);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("placard");
        this.setUnlocalizedName("create.placard");
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
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
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess access, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        switch (facing) {
            case DOWN: return new AxisAlignedBB(0.1875, 0.875, 0.1875, 0.8125, 1.0, 0.8125);
            case UP: return new AxisAlignedBB(0.1875, 0.0, 0.1875, 0.8125, 0.125, 0.8125);
            case NORTH: return new AxisAlignedBB(0.1875, 0.25, 0.875, 0.8125, 0.875, 1.0);
            case SOUTH: return new AxisAlignedBB(0.1875, 0.25, 0.0, 0.8125, 0.875, 0.125);
            case WEST: return new AxisAlignedBB(0.875, 0.25, 0.1875, 1.0, 0.875, 0.8125);
            case EAST:
            default: return new AxisAlignedBB(0.0, 0.25, 0.1875, 0.125, 0.875, 0.8125);
        }
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
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, facing);
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
        if (player.isSneaking()) return false;
        TileEntityPlacard placard = Utils.cast(world.getTileEntity(pos), TileEntityPlacard.class);
        if (placard == null) return false;
        if (world.isRemote) return true;

        ItemStack inHand = player.getHeldItem(hand);
        ItemStack inBlock = placard.getHeldItem();

        if (inBlock.isEmpty()) {
            if (inHand.isEmpty() || !player.capabilities.allowEdit) return false;
            ItemStack display = inHand.copy();
            display.setCount(1);
            placard.setHeldItem(display);
            if (!player.capabilities.isCreativeMode) inHand.shrink(1);
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5F, 1.2F);
            return true;
        }

        if (!inHand.isEmpty() && !state.getValue(POWERED) && player.capabilities.allowEdit
                && inHand.getItem() == inBlock.getItem()
                && inHand.getMetadata() == inBlock.getMetadata()) {
            Utils.setBlockTESafe(world, pos, state.withProperty(POWERED, true), 3);
            world.notifyNeighborsOfStateChange(pos, this, false);
            world.notifyNeighborsOfStateChange(pos.offset(state.getValue(FACING).getOpposite()), this, false);
            placard.pulse();
            world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.5F, 0.7F);
            return true;
        }
        return false;
    }

    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
        if (world.isRemote) return;
        TileEntityPlacard placard = Utils.cast(world.getTileEntity(pos), TileEntityPlacard.class);
        if (placard == null) return;
        ItemStack held = placard.getHeldItem();
        if (held.isEmpty()) return;
        placard.setHeldItem(ItemStack.EMPTY);
        if (!player.capabilities.isCreativeMode) {
            player.inventory.placeItemBackInInventory(world, held);
        }
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5F, 0.8F);
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
        return new TileEntityPlacard();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntityPlacard placard = Utils.cast(world.getTileEntity(pos), TileEntityPlacard.class);
        if (placard != null && !placard.getHeldItem().isEmpty() && !world.isRemote) {
            world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    placard.getHeldItem()));
        }
        world.removeTileEntity(pos);
        if (state.getValue(POWERED)) {
            world.notifyNeighborsOfStateChange(pos, this, false);
        }
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return state.getValue(POWERED);
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        return state.getValue(POWERED) && side == state.getValue(FACING) ? 15 : 0;
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos,
                                      @Nullable EnumFacing side) {
        return side != null;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "axe".equals(type);
    }
}
