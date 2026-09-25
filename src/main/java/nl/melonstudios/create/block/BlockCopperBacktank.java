package nl.melonstudios.create.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.actor.TileEntityCopperBacktank;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;

/**
 * Copper backtank block: the placed form of the wearable air tank. Holds 900
 * air (the reference airInBacktank default) and spends it topping up the
 * breath of nearby players, refilling slowly while idle. Breaks back into an
 * item keeping its air via the tile entity NBT.
 */
public class BlockCopperBacktank extends Block implements ITileEntityProvider {
    public BlockCopperBacktank() {
        super(Material.IRON, MapColor.ADOBE);
        this.setSoundType(SoundType.METAL);
        this.setHardness(3.0F);
        this.setResistance(BlockProperties.IRON_RESISTANCE);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(ItemInit.TAB_CREATE);
        this.setRegistryName("copper_backtank");
        this.setUnlocalizedName("create.copper_backtank");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityCopperBacktank();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityCopperBacktank && !worldIn.isRemote) {
            ItemStack drop = new ItemStack(this);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Air", ((TileEntityCopperBacktank) te).air);
            drop.setTagCompound(tag);
            EntityItem entity = new EntityItem(worldIn, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop);
            entity.setDefaultPickupDelay();
            worldIn.spawnEntity(entity);
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public java.util.List<ItemStack> getDrops(net.minecraft.world.IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        return new java.util.ArrayList<>();
    }
}
