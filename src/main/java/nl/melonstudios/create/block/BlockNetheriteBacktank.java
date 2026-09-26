package nl.melonstudios.create.block;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.actor.TileEntityNetheriteBacktank;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;

/**
 * Netherite backtank block: the sturdier placed air tank. Same breathing rules
 * as copper but double the reservoir (1800 air) and a higher harvest tier.
 */
public class BlockNetheriteBacktank extends Block implements ITileEntityProvider {
    public BlockNetheriteBacktank() {
        super(Material.IRON, MapColor.BLACK);
        this.setSoundType(SoundType.METAL);
        this.setHardness(5.0F);
        this.setResistance(1200.0F);
        this.setHarvestLevel("pickaxe", 2);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("netherite_backtank");
        this.setUnlocalizedName("create.netherite_backtank");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityNetheriteBacktank();
    }
}
