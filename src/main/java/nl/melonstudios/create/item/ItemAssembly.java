package nl.melonstudios.create.item;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.recipe.sequence.SequenceRecipe;
import nl.melonstudios.create.recipe.sequence.SequencedRecipes;

public class ItemAssembly extends Item {
    public ItemAssembly() {
        super();
        this.setRegistryName("assembly");
        this.setUnlocalizedName("create.assembly");
        this.setHasSubtypes(true);
        this.setMaxStackSize(1);
        this.setMaxDamage(0);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - this.getProgress(stack);
    }

    public double getProgress(ItemStack stack) {
        NBTTagCompound data = stack.getSubCompound("SequencedAssembly");
        if (data == null) return 0.0;
        SequenceRecipe recipe = SequencedRecipes.instance.getRecipe(data.getString("id"));
        if (recipe == null) return 0.0;
        int step = data.getInteger("step");
        return this.progress(recipe, step);
    }
    private double progress(SequenceRecipe recipe, int step) {
        int total = recipe.steps.size() * recipe.repetitions;
        if (total <= 0) return 0.0;
        double progress = (double) step / (double) total;
        if (progress < 0.0) return 0.0;
        if (progress > 1.0) return 1.0;
        return progress;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        double progress = 0.0;
        try {
            progress = this.getProgress(stack);
        } catch (Exception ignored) {
        }
        return mixColors(0xFFC074, 0x46FFE0, (float) progress);
    }

    private static int mixColors(int first, int second, float progress) {
        if (progress < 0.0F) progress = 0.0F;
        if (progress > 1.0F) progress = 1.0F;
        int r = Math.round(((first >> 16) & 0xFF) * (1.0F - progress) + ((second >> 16) & 0xFF) * progress);
        int g = Math.round(((first >> 8) & 0xFF) * (1.0F - progress) + ((second >> 8) & 0xFF) * progress);
        int b = Math.round((first & 0xFF) * (1.0F - progress) + (second & 0xFF) * progress);
        return (r << 16) | (g << 8) | b;
    }

    public static final String[] NAME_LOOKUP = {
            "precision_mechanism", "sturdy_sheet"
    };

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        // NAME_LOOKUP holds model keys ("assembly/" + key must match a model file);
        // the display name for meta 1 lives under the read-only lang key
        // item.create.assembly_plate_obsidian, so it is mapped explicitly.
        switch (stack.getMetadata() & 1) {
            case 1:
                return "item.create.assembly_plate_obsidian";
            default:
                return "item.create.assembly_" + NAME_LOOKUP[0];
        }
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
}
