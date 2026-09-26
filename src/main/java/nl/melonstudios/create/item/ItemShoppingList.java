package nl.melonstudios.create.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Shopping list for the table-cloth shop network.
 * Reference: ShoppingListItem (single-stack; carries a "Purchases" list of
 * cloth positions plus amounts, with shop owner/network ids; sneak-use on
 * the cloth adds a purchase). Purchase capture against the cloth block
 * entity waits on the shop GUI, so this stores and summarizes the NBT and
 * sneak-use clears it.
 */
public class ItemShoppingList extends Item {
    public ItemShoppingList() {
        super();
        this.setRegistryName("shopping_list");
        this.setUnlocalizedName("create.shopping_list");
        this.setMaxStackSize(1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        int purchases = getPurchaseCount(stack);
        if (purchases == 0) {
            tooltip.add(TextFormatting.GRAY + "Empty. Sneak-use a stocked table cloth to add a purchase.");
        } else {
            tooltip.add(TextFormatting.GOLD + "" + purchases + (purchases == 1 ? " purchase" : " purchases"));
        }
    }

    public static int getPurchaseCount(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return 0;
        }
        NBTTagList purchases = tag.getTagList("Purchases", 10);
        int total = 0;
        for (int i = 0; i < purchases.tagCount(); i++) {
            total += purchases.getCompoundTagAt(i).getInteger("Amount");
        }
        return total;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && player.isSneaking() && stack.hasTagCompound()) {
            stack.setTagCompound(null);
            player.sendStatusMessage(new TextComponentString("Shopping list cleared."), true);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }
}
