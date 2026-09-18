package nl.melonstudios.create.init;

import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemArmor;
import net.minecraftforge.common.util.EnumHelper;

import java.util.Objects;

public final class CreateEnums {
    public static final ItemArmor.ArmorMaterial ARMOR_MATERIAL_CARDBOARD = Objects.requireNonNull(EnumHelper.addArmorMaterial(
            "CREATE$CARDBOARD", "cardboard", 4, new int[]{1, 1, 1, 1},
            25, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0.0F
    ), "Could not create ArmorMaterial: cardboard");
}
