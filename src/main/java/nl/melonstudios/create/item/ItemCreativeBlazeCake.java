package nl.melonstudios.create.item;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import nl.melonstudios.create.init.ItemInit;

/**
 * Creative-only blaze burner fuel that never runs out
 * (reference burn time Integer.MAX_VALUE). No recipe by design.
 */
public class ItemCreativeBlazeCake extends Item {
    public ItemCreativeBlazeCake() {
        this.setRegistryName("creative_blaze_cake");
        this.setUnlocalizedName("create.creative_blaze_cake");
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public int getItemBurnTime(ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }
}
