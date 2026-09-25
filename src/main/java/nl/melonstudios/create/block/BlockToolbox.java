package nl.melonstudios.create.block;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.TileEntityToolbox;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Toolbox block (single brown variant; the 16 dye colors are NEEDS-LEAD).
 * Reference: ToolboxBlock + ToolboxHandler (GUI menu, lid/drawer animation,
 * keybind auto-equip from the paired toolbox). No container/GUI precedent
 * exists in this port, so use is capability-style: plain use inserts the
 * held stack, empty-hand use pulls the last stored stack out, sneak + empty
 * hand pulls from the first slot. Comparator reads fill level.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockToolbox extends Block implements ITileEntityProvider {
    public BlockToolbox() {
        super(Material.WOOD);
        this.blockSoundType = SoundType.WOOD;
        this.setHardness(BlockProperties.WOOD_HARDNESS);
        this.setResistance(BlockProperties.WOOD_RESISTANCE);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityToolbox();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityToolbox();
    }

    @Nullable
    private static TileEntityToolbox toolboxAt(World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        return te instanceof TileEntityToolbox ? (TileEntityToolbox) te : null;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntityToolbox toolbox = toolboxAt(worldIn, pos);
        if (toolbox == null) return false;
        if (worldIn.isRemote) return true;
        IItemHandler inv = toolbox.getInventory();
        ItemStack held = playerIn.getHeldItem(hand);
        if (!held.isEmpty()) {
            ItemStack rest = ItemHandlerHelper.insertItemStacked(inv, held.copy(), false);
            playerIn.setHeldItem(hand, rest);
            return true;
        }
        // Empty hand: take out. Sneak takes from slot 0, otherwise last filled.
        int slot = -1;
        if (playerIn.isSneaking()) {
            if (!inv.getStackInSlot(0).isEmpty()) slot = 0;
        } else {
            for (int i = inv.getSlots() - 1; i >= 0; i--) {
                if (!inv.getStackInSlot(i).isEmpty()) {
                    slot = i;
                    break;
                }
            }
        }
        if (slot < 0) {
            playerIn.sendStatusMessage(new TextComponentString("Toolbox is empty."), true);
            return true;
        }
        ItemStack out = inv.extractItem(slot, inv.getStackInSlot(slot).getCount(), false);
        playerIn.setHeldItem(hand, out);
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntityToolbox toolbox = toolboxAt(worldIn, pos);
        if (toolbox != null && !worldIn.isRemote && worldIn.getGameRules().getBoolean("doTileDrops")) {
            IItemHandler inv = toolbox.getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (!s.isEmpty()) spawnAsEntity(worldIn, pos, s.copy());
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntityToolbox toolbox = toolboxAt(worldIn, pos);
        if (toolbox == null) return 0;
        return ItemHandlerHelper.calcRedstoneFromInventory(toolbox.getInventory());
    }
}
