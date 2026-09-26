package nl.melonstudios.create.item;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

/**
 * Drinkable food (1 hunger, Haste for 3 minutes, always edible)
 * that hands back a glass bottle. Matches reference BuildersTeaItem;
 * the tea-fluid filling recipe needs the lead (no filling system yet).
 */
public class ItemBuildersTea extends ItemFood {
    public ItemBuildersTea() {
        super(1, 0.6F, false);
        this.setRegistryName("builders_tea");
        this.setUnlocalizedName("create.builders_tea");
        this.setMaxStackSize(16);
        this.setAlwaysEdible();
        this.setPotionEffect(new PotionEffect(MobEffects.HASTE, 3 * 60 * 20, 0), 1.0F);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 42;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.DRINK;
    }

    @Override
    protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
        super.onFoodEaten(stack, world, player);
        if (!world.isRemote && !player.capabilities.isCreativeMode) {
            player.inventory.addItemStackToInventory(new ItemStack(Items.GLASS_BOTTLE));
        }
    }
}
