package nl.melonstudios.create.block.logistics;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.item.ItemPackage;
import nl.melonstudios.create.tileentity.logistics.TileEntityFrogport;

import javax.annotation.Nullable;

/**
 * Frogport block: the addressed outlet of the package network. Sneak-use with
 * a box stamps its address as this port's filter; plain use feeds a held box
 * in; sneak-use empty-handed clears the filter.
 */
@SuppressWarnings("deprecation")
public class BlockFrogport extends Block implements ITileEntityProvider {
    public BlockFrogport() {
        super(Material.WOOD, MapColor.WOOD);
        this.setRegistryName("frogport");
        this.setUnlocalizedName("create.frogport");
        this.blockSoundType = SoundType.WOOD;
        this.setHardness(2.0F);
        this.setResistance(5.0F);
        this.setHarvestLevel("axe", 0);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityFrogport();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                   EnumHand hand, EnumFacing facing,
                                   float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityFrogport)) return true;
        TileEntityFrogport frogport = (TileEntityFrogport) te;
        ItemStack held = player.getHeldItem(hand);

        if (player.isSneaking() && !held.isEmpty() && held.getItem() == ItemInit.PACKAGE) {
            String address = ItemPackage.getAddress(held);
            if (address.trim().isEmpty()) {
                player.sendStatusMessage(new TextComponentString(
                        "That box has no address; the filter stays as-is."), true);
                return true;
            }
            frogport.addressFilter = address;
            frogport.sync();
            world.playSound(null, pos, SoundInit.frogport_open, SoundCategory.BLOCKS, 0.9F, 1.0F);
            player.sendStatusMessage(new TextComponentString(
                    "Frogport now accepts: " + address), true);
            return true;
        }
        if (player.isSneaking() && held.isEmpty()) {
            frogport.addressFilter = "";
            frogport.sync();
            player.sendStatusMessage(new TextComponentString(
                    "Frogport accepts every address now."), true);
            return true;
        }
        if (!held.isEmpty() && held.getItem() == ItemInit.PACKAGE) {
            if (frogport.insertPackage(held)) {
                held.shrink(1);
                world.playSound(null, pos, SoundInit.frogport_open, SoundCategory.BLOCKS, 0.9F, 1.2F);
            } else if (!frogport.heldPackage.isEmpty()) {
                world.playSound(null, pos, SoundInit.deny, SoundCategory.BLOCKS, 1.0F, 1.0F);
                player.sendStatusMessage(new TextComponentString(
                        "The frogport is still swallowing a box."), true);
            } else {
                world.playSound(null, pos, SoundInit.deny, SoundCategory.BLOCKS, 1.0F, 1.0F);
                player.sendStatusMessage(new TextComponentString(
                        "Address mismatch: this port takes '" + frogport.addressFilter + "'."), true);
            }
            return true;
        }
        String filter = frogport.addressFilter.trim().isEmpty()
                ? "every address" : "'" + frogport.addressFilter + "'";
        String load = frogport.heldPackage.isEmpty() ? "empty" : "digesting one box";
        player.sendStatusMessage(new TextComponentString(
                "Frogport takes " + filter + " (" + load + ")."), false);
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityFrogport) {
            ItemStack held = ((TileEntityFrogport) te).heldPackage;
            if (!held.isEmpty()) {
                worldIn.spawnEntity(new EntityItem(worldIn,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, held));
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
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityFrogport) return ((TileEntityFrogport) te).comparatorLevel();
        return 0;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "axe".equals(type);
    }
}
