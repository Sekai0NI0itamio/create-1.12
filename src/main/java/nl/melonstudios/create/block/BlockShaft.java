package nl.melonstudios.create.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.BlockProperties;

public class BlockShaft extends BlockSimpleShaftBase {
    public BlockShaft(Material blockMaterialIn, MapColor blockMapColorIn) {
        super(blockMaterialIn, blockMapColorIn);

        this.setRegistryName("shaft");
        this.setUnlocalizedName("create.shaft");

        this.setResistance(BlockProperties.STONE_RESISTANCE);
        this.setHardness(BlockProperties.STONE_HARDNESS);

        this.setHarvestLevel("pickaxe", 0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    public static boolean isShaft(IBlockState state) {
        return state.getBlock() instanceof BlockShaft;
    }
}
