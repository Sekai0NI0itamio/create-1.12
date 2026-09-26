package nl.melonstudios.create.item;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.EnumHelper;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

/**
 * Cosmetic crew hats. Reference: EntityHats (no items at all — the train hat
 * and logistics hats auto-render on entities riding scheduled trains or
 * sitting at staffed stock-ticker stations). 1.12 equivalent: wearable head
 * items with zero protection so players can dress the part; the automatic
 * entity-hat rendering is out of scope (needs a render-layer hook,
 * NEEDS-LEAD).
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemHat extends ItemArmor {
    public static final ArmorMaterial MATERIAL_HAT = Objects.requireNonNull(EnumHelper.addArmorMaterial(
            "CREATE$HAT", "hat", 5, new int[]{0, 0, 0, 0},
            0, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0.0F
    ), "Could not create ArmorMaterial: hat");

    public static final String[] NAMES = {"conductor", "engineer"};

    public ItemHat() {
        super(MATERIAL_HAT, 0, EntityEquipmentSlot.HEAD);
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            for (int i = 0; i < NAMES.length; i++) items.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int meta = Math.max(0, Math.min(stack.getMetadata(), NAMES.length - 1));
        return "create.hat_" + NAMES[meta];
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        // Placeholder: vanilla leather helm overlay until hat art lands (NEEDS-LEAD).
        return "textures/models/armor/leather_layer_1.png";
    }
}
