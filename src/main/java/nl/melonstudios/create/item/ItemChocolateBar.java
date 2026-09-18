package nl.melonstudios.create.item;

import net.minecraft.item.ItemFood;
import nl.melonstudios.create.init.ItemInit;

/** Bar of Chocolate food (6 hunger, 0.3 saturation). Chocolate-compacting recipe needs the lead. */
public class ItemChocolateBar extends ItemFood {
    public ItemChocolateBar() {
        super(6, 0.3F, false);
        this.setRegistryName("bar_of_chocolate");
        this.setUnlocalizedName("create.bar_of_chocolate");
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }
}
