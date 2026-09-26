package nl.melonstudios.create.item;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import nl.melonstudios.create.init.ItemInit;

public class ItemBlockSail extends ItemBlock {
    public ItemBlockSail(Block block) {
        super(block);

        this.setHasSubtypes(false);

        this.setRegistryName("sail");
        this.setUnlocalizedName("create.sail");

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            items.add(new ItemStack(this));
        }
    }
}
