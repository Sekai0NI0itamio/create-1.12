package nl.melonstudios.create.item;

import net.minecraft.item.Item;
import nl.melonstudios.create.init.ItemInit;

/**
 * Raw zinc dropped from zinc ore. Smelts/crushes to zinc;
 * furnace + crushing entries need the lead (RecipeInit/OreDict are shared).
 */
public class ItemRawZinc extends Item {
    public ItemRawZinc() {
        this.setRegistryName("raw_zinc");
        this.setUnlocalizedName("create.raw_zinc");
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }
}
