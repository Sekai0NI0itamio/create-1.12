package nl.melonstudios.create.item;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import nl.melonstudios.create.init.ItemInit;

public final class ItemIngredient extends Item {
    public ItemIngredient() {
        this.setRegistryName("ingredient");
        this.setUnlocalizedName("create.ingredient");

        this.setHasSubtypes(true);
        this.setMaxDamage(0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    public static final String[] NAME_LOOKUP = {
            "wheat_flour", "dough", "cinder_flour", "rose_quartz", "polished_rose_quartz", "powdered_obsidian",
            "sturdy_sheet", "propeller", "whisk", "brass_hand", "electron_tube", "transmitter", "pulp",
            "cardboard", "precision_mechanism", "andesite_alloy", "copper_ingot", "zinc_ingot", "brass_ingot",
            "copper_nugget", "zinc_nugget", "brass_nugget", "copper_sheet", "brass_sheet", "iron_sheet",
            "gold_sheet", "crushed_iron_ore", "crushed_gold_ore", "crushed_copper_ore", "crushed_zinc_ore",
            "blazecake_base", "blazecake"
    };

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int metadata = stack.getMetadata();
        return metadata < NAME_LOOKUP.length ? "item.create." + NAME_LOOKUP[metadata] : "item.create.ingredient";
    }

    @Override
    protected boolean isInCreativeTab(CreativeTabs targetTab) {
        return targetTab == CreateTabs.TAB_CREATE || targetTab == CreativeTabs.SEARCH;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            for (int i = 0; i < NAME_LOOKUP.length; i++) items.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public int getItemBurnTime(ItemStack stack) {
        // Reference CARDBOARD (CombustibleItem) burn time is 1000; meta 13 is cardboard.
        return stack.getMetadata() == 13 ? 1000 : super.getItemBurnTime(stack);
    }
}
