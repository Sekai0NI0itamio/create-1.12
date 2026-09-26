package nl.melonstudios.create.block.logistics;

import nl.melonstudios.create.init.CreateTabs;
import com.melonstudios.melonlib.misc.BlockStateProperties;
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
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.item.ItemPackage;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.tileentity.logistics.TileEntityItemHatch;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Item hatch: wall-mounted plate feeding player items into the inventory of
 * the block it faces. Right-click deposits the held stack; sneak +
 * right-click deposits the whole main inventory. Sneak + right-click holding
 * an item sets the ghost filter instead; sneak + empty hand clears it.
 * Opens briefly on a successful deposit and closes after 10 ticks, mirroring
 * the 1.20.1 ItemHatchBlock behaviour through 1.12 idioms.
 */
@SuppressWarnings("deprecation")
public class BlockItemHatch extends Block implements ITileEntityProvider {
    public static final PropertyDirection FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final PropertyBool OPEN = PropertyBool.create("open");

    private static final double THICK = 3.0D / 16.0D;
    private static final AxisAlignedBB AABB_NORTH = new AxisAlignedBB(0.0D, 0.0D, 1.0D - THICK, 1.0D, 1.0D, 1.0D);
    private static final AxisAlignedBB AABB_SOUTH = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, THICK);
    private static final AxisAlignedBB AABB_WEST = new AxisAlignedBB(1.0D - THICK, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static final AxisAlignedBB AABB_EAST = new AxisAlignedBB(0.0D, 0.0D, 0.0D, THICK, 1.0D, 1.0D);

    public BlockItemHatch() {
        super(Material.IRON, MapColor.BLUE);
        this.blockSoundType = SoundType.METAL;

        this.setRegistryName("item_hatch");
        this.setUnlocalizedName("create.item_hatch");

        this.setHardness(3.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 1);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(OPEN, false));

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, OPEN);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
                .withProperty(OPEN, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(OPEN)) {
            meta |= 4;
        }
        return meta;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer) {
        if (facing.getAxis().isVertical()) {
            return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
        }
        return this.getDefaultState().withProperty(FACING, facing.getOpposite());
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
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case NORTH:
            default:
                return AABB_NORTH;
            case SOUTH:
                return AABB_SOUTH;
            case WEST:
                return AABB_WEST;
            case EAST:
                return AABB_EAST;
        }
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityItemHatch();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOptimizedBase) {
            ((TileEntityOptimizedBase) te).destroy();
        }
        super.breakBlock(worldIn, pos, state);
        if (this.hasTileEntity(state)) {
            worldIn.removeTileEntity(pos);
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                   EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityItemHatch)) {
            return false;
        }
        TileEntityItemHatch hatch = (TileEntityItemHatch) te;
        ItemStack held = playerIn.getHeldItem(hand);

        if (playerIn.isSneaking() && !held.isEmpty()) {
            hatch.setFilter(held);
            playerIn.sendStatusMessage(new TextComponentString("Hatch filter: "
                    + held.getDisplayName()), true);
            return true;
        }
        if (playerIn.isSneaking() && held.isEmpty()) {
            if (!hatch.getFilter().isEmpty()) {
                hatch.setFilter(ItemStack.EMPTY);
                playerIn.sendStatusMessage(new TextComponentString("Hatch filter cleared"), true);
                return true;
            }
        }

        TileEntity target = worldIn.getTileEntity(pos.offset(state.getValue(FACING)));
        if (target == null) {
            return false;
        }
        IItemHandler targetInv = target.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                state.getValue(FACING).getOpposite());
        if (targetInv == null) {
            return false;
        }

        boolean depositHeld = !playerIn.isSneaking();
        List<Integer> slots = new ArrayList<>();
        if (depositHeld) {
            slots.add(playerIn.inventory.currentItem);
        } else {
            for (int i = 0; i < playerIn.inventory.mainInventory.size(); i++) {
                if (i < 9) {
                    continue;
                }
                slots.add(i);
            }
        }

        boolean anyInserted = false;
        for (int slot : slots) {
            ItemStack item = playerIn.inventory.getStackInSlot(slot);
            if (item.isEmpty()) {
                continue;
            }
            if (item.getMaxStackSize() <= 1 && !(item.getItem() instanceof ItemPackage)) {
                continue;
            }
            if (!hatch.test(item)) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(targetInv, item, true);
            int moved = item.getCount() - remainder.getCount();
            if (moved <= 0) {
                continue;
            }
            ItemStack extracted = playerIn.inventory.decrStackSize(slot, moved);
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(targetInv, extracted, false);
            if (!leftover.isEmpty()) {
                playerIn.inventory.addItemStackToInventory(leftover);
            }
            anyInserted = true;
        }

        if (!anyInserted) {
            return true;
        }
        worldIn.setBlockState(pos, state.withProperty(OPEN, true), 3);
        worldIn.scheduleUpdate(pos, this, 10);
        playerIn.sendStatusMessage(new TextComponentString(depositHeld
                ? "Item deposited" : "Inventory deposited"), true);
        return true;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (state.getValue(OPEN)) {
            worldIn.setBlockState(pos, state.withProperty(OPEN, false), 3);
        }
    }
}
