package nl.melonstudios.create.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
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
import nl.melonstudios.create.tileentity.TileEntityFluidTank;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.Nullable;

/**
 * Fluid tank: stores 8000 mB per block, stacks vertically into one multiblock
 * (bottom TE owns the fluid, like official). Wrench/glass visuals via TESR.
 */
@SuppressWarnings("deprecation")
public class BlockFluidTank extends Block implements ITileEntityProvider {
    public BlockFluidTank() {
        super(Material.GLASS, MapColor.AIR);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(3.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityFluidTank();
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
        return FULL_BLOCK_AABB;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOptimizedBase) ((TileEntityOptimizedBase) te).destroy();
        if (this.hasTileEntity(state)) worldIn.removeTileEntity(pos);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityFluidTank)) return false;
        TileEntityFluidTank tank = (TileEntityFluidTank) te;
        ItemStack held = playerIn.getHeldItem(hand);
        if (held.isEmpty()) return false;
        if (FluidUtil.interactWithFluidHandler(playerIn, hand, tank.effectiveTank())) {
            FluidStack fluid = tank.getFluid();
            if (fluid != null) {
                worldIn.playSound(null, pos, fluid.getFluid().getEmptySound(fluid), SoundCategory.BLOCKS, 0.5F, 1.0F);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityFluidTank)) return 0;
        TileEntityFluidTank tank = (TileEntityFluidTank) te;
        FluidStack fluid = tank.getFluid();
        if (fluid == null || fluid.amount <= 0) return 0;
        int capacity = tank.getCapacity();
        if (capacity <= 0) return 0;
        return Math.min(15, 1 + (int) (14.0F * fluid.amount / capacity));
    }
}
