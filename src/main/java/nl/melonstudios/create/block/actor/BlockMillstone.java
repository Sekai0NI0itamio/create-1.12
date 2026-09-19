package nl.melonstudios.create.block.actor;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.recipe.server.MillingRecipes;
import nl.melonstudios.create.tileentity.actor.TileEntityMillstone;
import nl.melonstudios.create.util.TextBuilder;
import nl.melonstudios.create.util.interfaces.ICogwheel;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockMillstone extends BlockKineticBase implements ITileEntityProvider, ICogwheel {
    public BlockMillstone(Material blockMaterialIn, MapColor blockMapColorIn) {
        super(blockMaterialIn, blockMapColorIn);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityMillstone();
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side == EnumFacing.DOWN;
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return EnumFacing.Axis.Y;
    }

    @Override
    public void onFallenUpon(World worldIn, BlockPos pos, Entity entityIn, float fallDistance) {
        super.onFallenUpon(worldIn, pos, entityIn, fallDistance);

        if (worldIn.isRemote) return;
        if (!(entityIn instanceof EntityItem)) return;
        if (!entityIn.isEntityAlive()) return;
        ItemStack stack = ((EntityItem)entityIn).getItem();
        if (MillingRecipes.instance.getRecipeForInput(stack) == null) return;

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityMillstone) {
            TileEntityMillstone millstone = (TileEntityMillstone) te;
            if (millstone.input.isEmpty()) {
                millstone.input = stack;
                entityIn.setDead();
                millstone.sync();
            }
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!playerIn.getHeldItem(hand).isEmpty()) return false;
        if (worldIn.isRemote) return true;
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityMillstone) {
            TileEntityMillstone millstone = (TileEntityMillstone) te;
            boolean switchToMain = true;
            for (int i = 0; i < millstone.output.length; i++) {
                ItemStack stack = millstone.output[i];
                if (!stack.isEmpty()) {
                    switchToMain = false;
                    playerIn.inventory.addItemStackToInventory(stack.copy());
                    millstone.output[i] = ItemStack.EMPTY;
                    millstone.sync();
                }
            }
            if (switchToMain) {
                if (millstone.input.isEmpty()) return false;
                playerIn.inventory.addItemStackToInventory(millstone.input.copy());
                millstone.input = ItemStack.EMPTY;
                millstone.sync();
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> getGoggleInfo(World world, BlockPos pos, IBlockState state) {
        List<String> list0 = super.getGoggleInfo(world, pos, state);
        List<String> list = list0.equals(Collections.emptyList()) ? new ArrayList<>() : list0;
        withTEDo(world, pos, TileEntityMillstone.class, (te) -> {
            if (!te.input.isEmpty()) {
                TextBuilder builder = new TextBuilder();
                builder.enter().formatting(TextFormatting.GRAY).translate("goggles.info.currently_processing")
                        .text(": ").enter().space().space()
                        .formatting(TextFormatting.AQUA).text(te.input.getDisplayName())
                        .enter();
                list.addAll(builder.build());
            }
            Map<String, Integer> counts = new HashMap<>();
            for (ItemStack stack : te.output) {
                if (!stack.isEmpty()) {
                    String key = stack.getDisplayName();
                    counts.put(key, counts.computeIfAbsent(key, k -> 0)+stack.getCount());
                }
            }
            if (!counts.isEmpty()) {
                TextBuilder builder = new TextBuilder();
                builder.enter().formatting(TextFormatting.GRAY).translate("goggles.info.output")
                        .text(": ");
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    builder.enter().space().space().formatting(TextFormatting.AQUA)
                            .number(entry.getValue()).text("x ").text(entry.getKey());
                }
                list.addAll(builder.build());
            }
        });
        return list;
    }
}
