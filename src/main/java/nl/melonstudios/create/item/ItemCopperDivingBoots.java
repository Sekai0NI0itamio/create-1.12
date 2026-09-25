package nl.melonstudios.create.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Copper diving boots.
 * Reference: DivingBootsItem over the same copper material as the helmet
 * (boots 1, copper-ingot repair). Shares the helmet's material instance
 * so both pieces report one armor set.
 */
public class ItemCopperDivingBoots extends ItemArmor {
    public ItemCopperDivingBoots() {
        super(ItemCopperDivingHelmet.MATERIAL_COPPER_DIVING, 0, EntityEquipmentSlot.FEET);
        this.setRegistryName("copper_diving_boots");
        this.setUnlocalizedName("create.copper_diving_boots");
        this.setMaxStackSize(1);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "Keeps its grip on slippery contraption decks.");
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        // Armor art is still lead-owned; item sprite exists, suit texture does not yet.
        return "create:textures/armor/copper_diving.png";
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        // Reference repair ingredient is the copper ingot (meta 16 of ItemIngredient).
        return repair.getItem() == ItemInit.INGREDIENT && repair.getMetadata() == 16;
    }
}
