package nl.melonstudios.create.block.logistics;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
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
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.tileentity.logistics.TileEntityTableCloth;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Table cloth: thin decorative cover that presents up to 4 items on top.
 * Right-click with an item to lay one down, empty hand to pick the last back up.
 * Backport of the reference display side only; the shop/purchase flow needs the
 * stock-ticker network and shopping-list items, so it is out of scope here.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class BlockTableCloth extends Block implements ITileEntityProvider {
    private static final AxisAlignedBB CLOTH_AABB = new AxisAlignedBB(0, 0, 0, 1, 1 / 16.0, 1);

    public BlockTableCloth() {
        super(Material.CLOTH, MapColor.SNOW);
        this.blockSoundType = SoundType.CLOTH;
        this.setHardness(0.2F);
        this.setResistance(1.0F);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return CLOTH_AABB;
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
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityTableCloth();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityTableCloth) {
            TileEntityTableCloth cloth = (TileEntityTableCloth) te;
            for (ItemStack stack : cloth.displayed) {
                if (!stack.isEmpty()) {
                    EntityItem drop = new EntityItem(worldIn, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack.copy());
                    worldIn.spawnEntity(drop);
                }
            }
            cloth.displayed = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        }
        if (te instanceof TileEntityOptimizedBase) ((TileEntityOptimizedBase) te).destroy();
        if (this.hasTileEntity(state)) worldIn.removeTileEntity(pos);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (facing != EnumFacing.UP) return false;
        if (worldIn.isRemote) return true;
        return Boolean.TRUE.equals(BlockKineticBase.withTEDo(worldIn, pos, TileEntityTableCloth.class, (te) -> {
            ItemStack held = playerIn.getHeldItem(hand);
            if (held.isEmpty()) {
                ItemStack taken = te.takeLast();
                if (taken.isEmpty()) return false;
                playerIn.inventory.addItemStackToInventory(taken);
                worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 0.5F, 1.0F);
                te.sync();
                return true;
            }
            if (te.filledSlots() >= 4) return false;
            ItemStack shown = held.copy();
            shown.setCount(1);
            te.addShown(shown);
            if (!playerIn.isCreative()) held.shrink(1);
            worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, SoundCategory.BLOCKS, 0.5F, 1.0F);
            te.sync();
            return true;
        }));
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityTableCloth)) return 0;
        int filled = ((TileEntityTableCloth) te).filledSlots();
        if (filled <= 0) return 0;
        return Math.max(1, Math.min(15, filled * 15 / 4));
    }
}
