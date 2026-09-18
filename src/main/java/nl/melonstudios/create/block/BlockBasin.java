package nl.melonstudios.create.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.TileEntityBasin;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.SubInteractionBox;
import nl.melonstudios.create.util.TextBuilder;
import nl.melonstudios.create.util.Utils;
import nl.melonstudios.create.util.interfaces.IGoggleInfo;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockBasin extends Block implements ITileEntityProvider, IGoggleInfo {
    public BlockBasin() {
        super(Material.ROCK, MapColor.IRON);
        this.setSoundType(SoundType.ANVIL);

        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);

        this.setHarvestLevel("pickaxe", 0);

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
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityBasin();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOptimizedBase) ((TileEntityOptimizedBase)te).destroy();
        if (this.hasTileEntity(state)) worldIn.removeTileEntity(pos);
    }

    @Override
    public List<String> getGoggleInfo(World world, BlockPos pos, IBlockState state) {
        List<String> list = BlockKineticBase.withTEDo(world, pos, TileEntityBasin.class, (te) -> {
            boolean inv = !te.inventory.isEmpty();
            boolean fluid = te.hasAnyFluid();
            if (!inv && !fluid) return null;
            TextBuilder builder = new TextBuilder();
            if (inv) {
                builder.translate("goggles.inventory_contents").enter();
                for (ItemStack stack : te.inventory) {
                    builder.space().space();
                    builder.formatting(TextFormatting.GRAY).text(stack.getCount() + "x ");
                    builder.formatting(TextFormatting.AQUA).text(stack.getDisplayName()).enter();
                }
            }
            if (fluid) {
                builder.translate("goggles.fluid_contents").enter();
                for (FluidTank tank : te.fluid.getHandlers()) {
                    if (tank.getFluidAmount() <= 0) continue;
                    FluidStack stack = tank.getFluid();
                    if (stack == null) continue;
                    builder.space().space();
                    builder.formatting(TextFormatting.GRAY).text(stack.amount + "mB ");
                    builder.formatting(TextFormatting.AQUA).text(stack.getLocalizedName()).enter();
                }
            }
            return builder.build();
        });
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = playerIn.getHeldItem(hand);
        if (SubInteractionBox.handleInteraction(worldIn, pos, playerIn, false, playerIn.isSneaking(), held, hitX, hitY, hitZ)) return true;
        TileEntityBasin basin = Utils.cast(worldIn.getTileEntity(pos), TileEntityBasin.class);
        if (basin == null) return false;
        if (held.isEmpty()) {
            if (basin.inventory.isEmpty()) return false;
            for (ItemStack stack : basin.inventory) {
                playerIn.addItemStackToInventory(stack.copy());
            }
            basin.inventory.clear();
            return true;
        }
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(held);
        if (handler == null) return false;
        FluidStack contained = FluidUtil.getFluidContained(held);
        if (contained == null || contained.amount <= 0) {
            FluidStack fluid;
            basin.cleanupFluids();
            List<FluidTank> handlers = basin.fluid.getHandlers();
            int size = handlers.size();
            for (int i = size - 1; i >= 0; i--) {
                FluidTank tank = handlers.get(i);
                if (tank.getFluidAmount() > 0) {
                    if (handler.fill(tank.getFluid(), false) > 0) {
                        tank.drainInternal(handler.fill(fluid = tank.getFluid(), true), true);
                        playerIn.setHeldItem(hand, handler.getContainer());
                        if (fluid != null) {
                            worldIn.playSound(null, pos, fluid.getFluid().getFillSound(fluid), SoundCategory.BLOCKS, 1.0F, 1.0F);
                        }
                        return true;
                    }
                }
            }
        } else {
            IFluidTankProperties[] properties = handler.getTankProperties();
            if (properties.length == 0) return false;
            FluidStack fluid;
            basin.cleanupFluids();
            if (basin.fluid.fill(handler.drain(1000, false), false) > 0) {
                basin.fluid.fill(fluid = handler.drain(1000, true), true);
                playerIn.setHeldItem(hand, handler.getContainer());
                if (fluid != null) {
                    worldIn.playSound(null, pos, fluid.getFluid().getFillSound(fluid), SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn) {
        if (entityIn instanceof EntityItem) {
            TileEntityBasin basin = Utils.cast(worldIn.getTileEntity(pos), TileEntityBasin.class);
            if (basin != null) {
                EntityItem item = (EntityItem) entityIn;
                ItemStack stack = item.getItem();
                stack = basin.tryInsertItem(stack);
                if (stack.isEmpty()) item.setDead();
                else item.setItem(stack);
            }
        }
    }
}
