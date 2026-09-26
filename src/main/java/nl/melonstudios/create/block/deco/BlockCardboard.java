package nl.melonstudios.create.block.deco;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.BlockProperties;

/**
 * Cardboard block: the plain building block matching the existing cardboard
 * armor set. Soft (cloth sound, low hardness), slightly blast-resistant for
 * its weight, mineable by hand.
 */
public class BlockCardboard extends Block {
    public BlockCardboard() {
        super(Material.CLOTH, MapColor.SAND);
        this.setSoundType(SoundType.CLOTH);
        this.setHardness(BlockProperties.WOOL_HARDNESS);
        this.setResistance(BlockProperties.WOOL_RESISTANCE);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("cardboard_block");
        this.setUnlocalizedName("create.cardboard_block");
    }
}
