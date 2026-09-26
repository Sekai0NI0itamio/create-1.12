package nl.melonstudios.create.block.redstone;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
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
import nl.melonstudios.create.tileentity.redstone.TileEntityLecternController;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;

/**
 * Lectern controller: a wooden lectern that holds a linked-controller
 * stack, binding the redstone link network to a reading stand. Right-click
 * with the controller in hand to slot it; empty-hand click takes it back;
 * sneak-clicking with an empty lectern does nothing. A slotted controller
 * drives the comparator to full power, translated from the reference
 * fixed-strength analog output (which reads 15 while the stand is in use).
 */
@SuppressWarnings("deprecation")
public class BlockLecternController extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.875, 1.0);

    public BlockLecternController() {
        super(Material.WOOD, MapColor.WOOD);
        this.blockSoundType = SoundType.WOOD;
        this.setHardness(2.0F);
        this.setResistance(4.0F);
        this.setHarvestLevel("axe", 0);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("lectern_controller");
        this.setUnlocalizedName("create.lectern_controller");
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
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
        return AABB;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                   EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntityLecternController lectern =
                Utils.cast(world.getTileEntity(pos), TileEntityLecternController.class);
        if (lectern == null) return false;
        if (world.isRemote) return true;
        if (!player.capabilities.allowEdit) return false;

        ItemStack inHand = player.getHeldItem(hand);
        ItemStack slotted = lectern.getController();

        if (slotted.isEmpty()) {
            if (player.isSneaking() || inHand.isEmpty()) return false;
            ItemStack copy = inHand.copy();
            copy.setCount(1);
            lectern.setController(copy);
            if (!player.capabilities.isCreativeMode) inHand.shrink(1);
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5F, 1.2F);
            return true;
        }

        if (player.isSneaking()) return false;
        lectern.setController(ItemStack.EMPTY);
        if (!player.capabilities.isCreativeMode) {
            player.inventory.placeItemBackInInventory(world, slotted);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, 0.6F);
        return true;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side,
                             float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        Utils.setBlockTESafe(world, pos, state.withProperty(FACING, state.getValue(FACING).rotateY()), 3);
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityLecternController();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntityLecternController lectern =
                Utils.cast(world.getTileEntity(pos), TileEntityLecternController.class);
        if (lectern != null && !lectern.getController().isEmpty() && !world.isRemote) {
            world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    lectern.getController()));
        }
        world.removeTileEntity(pos);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntityLecternController lectern =
                Utils.cast(world.getTileEntity(pos), TileEntityLecternController.class);
        return lectern != null && !lectern.getController().isEmpty() ? 15 : 0;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "axe".equals(type);
    }
}
