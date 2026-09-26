package nl.melonstudios.create.block.train;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import nl.melonstudios.create.init.ItemInit;

/**
 * Railway casing: decorative train-blue casing for stations, signals and
 * train bodies. Plain solid cube (reference layered/CTM sprites collapse to
 * the side + top textures here).
 */
public class BlockRailwayCasing extends Block {
    public BlockRailwayCasing() {
        super(Material.IRON, MapColor.CYAN);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(3.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("railway_casing");
        this.setUnlocalizedName("create.railway_casing");
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
