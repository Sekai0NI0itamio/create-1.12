package nl.melonstudios.create.item;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;

public class ItemGoggles extends ItemArmor {
    // Reference GogglesItem is a plain Item with no protection; ItemArmor is the
    // 1.12 idiom for head-slot equip + dispenser behaviour, so use a material
    // with zero protection instead of GOLD (gold helmet would grant 2 armor).
    private static final ArmorMaterial GOGGLES_MATERIAL = EnumHelper.addArmorMaterial(
            "CREATE_GOGGLES", "create:goggles", 0,
            new int[]{0, 0, 0, 0}, 0, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0.0F);

    public ItemGoggles() {
        super(GOGGLES_MATERIAL, 0, EntityEquipmentSlot.HEAD);
        this.setRegistryName("goggles");
        this.setUnlocalizedName("create.goggles");
        this.setMaxStackSize(1);
        this.setMaxDamage(0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return false;
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        return "create:textures/armor/goggles.png";
    }

    @Override
    public int getItemEnchantability() {
        return 0;
    }

    @Override
    protected boolean isInCreativeTab(CreativeTabs targetTab) {
        return targetTab == CreativeTabs.SEARCH || targetTab == CreateTabs.TAB_CREATE;
    }
}
