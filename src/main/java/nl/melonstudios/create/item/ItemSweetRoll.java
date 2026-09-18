package nl.melonstudios.create.item;

import net.minecraft.item.ItemFood;
import nl.melonstudios.create.init.ItemInit;

/** Sweet Roll food (6 hunger, 0.8 saturation). Milk-filling recipe needs the lead. */
public class ItemSweetRoll extends ItemFood {
    public ItemSweetRoll() {
        super(6, 0.8F, false);
        this.setRegistryName("sweet_roll");
        this.setUnlocalizedName("create.sweet_roll");
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }
}
