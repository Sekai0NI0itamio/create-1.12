package nl.melonstudios.create.item;

import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import nl.melonstudios.create.init.CreateEnums;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;

public class ItemArmorCardboard extends ItemArmor {
    public ItemArmorCardboard(EntityEquipmentSlot slot, String part) {
        super(CreateEnums.ARMOR_MATERIAL_CARDBOARD, 0, slot);
        this.setRegistryName(part + "_cardboard");
        this.setUnlocalizedName("create." + part + "_cardboard");
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        return "create:textures/armor/cardboard" + (slot == EntityEquipmentSlot.LEGS ? "_overlay.png" : ".png");
    }

    @Override
    public int getItemBurnTime(ItemStack stack) {
        // Reference CardboardArmorItem#getBurnTime returns 1000.
        return 1000;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        // Reference repair ingredient is the cardboard item (meta 13 of ItemIngredient).
        return repair.getItem() == ItemInit.INGREDIENT && repair.getMetadata() == 13;
    }
}
