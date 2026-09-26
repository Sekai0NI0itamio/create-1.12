package nl.melonstudios.create.block.logistics;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
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
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.tileentity.logistics.TileEntityCreativeCrate;

import javax.annotation.Nullable;

/**
 * Creative crate: full-cube stone block supplying infinite copies of its
 * filter item through the item-handler capability, mirroring the 1.20.1
 * CreativeCrateBlock behaviour through 1.12 idioms.
 *
 * Interaction: right-click holding an item sets it as the supply filter;
 * sneak + right-click with an empty hand clears the filter. Extraction is
 * done by pipes/funnels/hoppers through the capability, never via GUI.
 */
public class BlockCreativeCrate extends Block implements ITileEntityProvider, IWrenchable {
    public BlockCreativeCrate() {
        super(Material.ROCK, MapColor.PURPLE);
        this.blockSoundType = SoundType.STONE;

        this.setRegistryName("creative_crate");
        this.setUnlocalizedName("create.creative_crate");

        this.setHardness(2.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityCreativeCrate();
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || "axe".equals(type);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOptimizedBase) {
            ((TileEntityOptimizedBase) te).destroy();
        }
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
        if (!(te instanceof TileEntityCreativeCrate)) {
            return false;
        }
        TileEntityCreativeCrate crate = (TileEntityCreativeCrate) te;
        ItemStack held = playerIn.getHeldItem(hand);
        if (held.isEmpty()) {
            if (!playerIn.isSneaking()) {
                return false;
            }
            crate.setFilter(ItemStack.EMPTY);
            playerIn.sendStatusMessage(new TextComponentString("Crate supply cleared"), true);
            return true;
        }
        if (playerIn.isSneaking()) {
            return false;
        }
        crate.setFilter(held);
        playerIn.sendStatusMessage(new TextComponentString("Crate now supplies: "
                + held.getDisplayName()), true);
        return true;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side,
                              float hitX, float hitY, float hitZ) {
        return false;
    }
}
