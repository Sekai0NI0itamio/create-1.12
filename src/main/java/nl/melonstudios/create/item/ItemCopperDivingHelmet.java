package nl.melonstudios.create.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Copper diving helmet.
 * Reference: DivingHelmetItem over AllArmorMaterials.COPPER (durability 7,
 * helmet 2, enchantability 25, copper-ingot repair; Aqua Affinity level 1
 * is innate and cannot be added again). The material lives here because
 * CreateEnums is lead-owned; the boots share this instance.
 */
public class ItemCopperDivingHelmet extends ItemArmor {
    public static final ArmorMaterial MATERIAL_COPPER_DIVING = EnumHelper.addArmorMaterial(
            "CREATE$COPPER_DIVING", "create:copper_diving", 7,
            new int[]{2, 4, 3, 1}, 25, SoundEvents.ITEM_ARMOR_EQUIP_IRON, 0.0F);

    public ItemCopperDivingHelmet() {
        super(MATERIAL_COPPER_DIVING, 0, EntityEquipmentSlot.HEAD);
        this.setRegistryName("copper_diving_helmet");
        this.setUnlocalizedName("create.copper_diving_helmet");
        this.setMaxStackSize(1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.AQUA + "Built-in Aqua Affinity");
        tooltip.add(TextFormatting.GRAY + "Pair with a pressurised backtank to breathe underwater.");
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
