package nl.melonstudios.create.block;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.TileEntityDepot;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.TextBuilder;
import nl.melonstudios.create.util.interfaces.IGoggleInfo;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class BlockDepot extends Block implements ITileEntityProvider, IGoggleInfo {
    public BlockDepot() {
        super(Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;

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
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BlockProperties.CASING_12PX_UP;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityDepot();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOptimizedBase) ((TileEntityOptimizedBase)te).destroy();
        if (this.hasTileEntity(state)) worldIn.removeTileEntity(pos);
    }

    @Override
    public List<String> getGoggleInfo(World world, BlockPos pos, IBlockState state) {
        List<String> list = BlockKineticBase.withTEDo(world, pos, TileEntityDepot.class, (te) -> {
            if (te.isEmpty()) return null;
            TextBuilder builder = new TextBuilder();
            builder.translate("goggles.inventory_contents").enter();
            if (!te.mainItem.isEmpty()) {
                builder.space().space().formatting(TextFormatting.GRAY).text(te.mainItem.getCount() + "x ")
                        .formatting(TextFormatting.AQUA).text(te.mainItem.getDisplayName()).enter();
            }
            for (int i = 0; i < 8; i++) {
                if (!te.additionalItems[i].isEmpty()) {
                    builder.space().space().formatting(TextFormatting.GRAY).text(te.additionalItems[i].getCount() + "x ")
                            .formatting(TextFormatting.AQUA).text(te.additionalItems[i].getDisplayName()).enter();
                }
            }
            return builder.build();
        });
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (facing == EnumFacing.UP) {
            return Boolean.TRUE.equals(BlockKineticBase.withTEDo(worldIn, pos, TileEntityDepot.class, (te) -> {
                if (te.isEmpty()) {
                    te.mainItem = playerIn.getHeldItem(hand).copy();
                    playerIn.setHeldItem(hand, ItemStack.EMPTY);
                } else {
                    if (!te.mainItem.isEmpty()) {
                        playerIn.inventory.addItemStackToInventory(te.mainItem.copy());
                        te.mainItem = ItemStack.EMPTY;
                    }
                    for (int i = 0; i < 8; i++) {
                        if (!te.additionalItems[i].isEmpty()) {
                            playerIn.inventory.addItemStackToInventory(te.additionalItems[i].copy());
                            te.additionalItems[i] = ItemStack.EMPTY;
                        }
                    }
                    worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS,
                            1.0F, 0.9F + worldIn.rand.nextFloat() * 0.2F);
                }
                te.sync();
                return true;
            }));
        }
        return false;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || "axe".equals(type);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityDepot)) return 0;
        ItemStack held = ((TileEntityDepot) te).mainItem;
        if (held.isEmpty()) return 0;
        float fill = (float) held.getCount() / (float) Math.max(1, held.getMaxStackSize());
        return Math.max(0, Math.min(15, (int) (fill * 14.0F) + 1));
    }
}
