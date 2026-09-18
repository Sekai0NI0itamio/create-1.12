package nl.melonstudios.create.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.TileEntityItemDrain;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public class BlockItemDrain extends Block implements ITileEntityProvider {
    public BlockItemDrain() {
        super(Material.IRON, MapColor.ORANGE_STAINED_HARDENED_CLAY);

        this.setHarvestLevel("pickaxe", 0);
        this.setHardness(3.0F);
        this.setResistance(BlockProperties.IRON_RESISTANCE);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    //region this is not a full block
    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isTranslucent(IBlockState state) {
        return true;
    }

    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
    //endregion

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BlockProperties.CASING_12PX_UP;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityItemDrain();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOptimizedBase) ((TileEntityOptimizedBase)te).destroy();
        if (this.hasTileEntity(state)) worldIn.removeTileEntity(pos);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntityItemDrain te = Utils.cast(worldIn.getTileEntity(pos), TileEntityItemDrain.class);
        if (te == null) return false;
        ItemStack held = playerIn.getHeldItem(hand);
        if (held.isEmpty()) {
            if (te.draining.isEmpty()) return false;
            ItemStack stack = te.draining.copy();
            playerIn.addItemStackToInventory(stack);
            te.draining = ItemStack.EMPTY;
            te.sync();
            return true;
        }
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(held);
        if (handler == null) return false;
        FluidStack prev = te.tank.getFluid();
        if (prev == null) {
            te.tank.fillInternal(handler.drain(1500, true), true);
            FluidStack fluid = te.tank.getFluid();
            if (fluid != null) {
                worldIn.playSound(null, pos, fluid.getFluid().getEmptySound(fluid), SoundCategory.BLOCKS, 1.0F, 1.0F);
                playerIn.setHeldItem(hand, handler.getContainer());
                return true;
            }
            return false;
        }
        if (prev.amount == 0) return false;
        worldIn.playSound(null, pos, prev.getFluid().getFillSound(prev), SoundCategory.BLOCKS, 1.0F, 1.0F);
        te.tank.drainInternal(handler.fill(te.tank.drainInternal(1500, false), true), true);
        playerIn.setHeldItem(hand, handler.getContainer());
        return true;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
