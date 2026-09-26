package nl.melonstudios.create.item;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Filter that matches by block/entity attribute instead of by item.
 * Reference: AttributeFilterItem (a FilterItem whose summary shows the
 * chosen attribute id). The reference GUI is out of scope for this port,
 * so the attribute is carried as the "Attribute" NBT string; sneak-use
 * clears it back to an empty filter.
 */
public class ItemAttributeFilter extends Item {
    public ItemAttributeFilter() {
        super();
        this.setRegistryName("attribute_filter");
        this.setUnlocalizedName("create.attribute_filter");
        this.setMaxStackSize(64);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        String attribute = getAttribute(stack);
        if (attribute.isEmpty()) {
            tooltip.add(TextFormatting.GRAY + "No attribute selected");
        } else {
            tooltip.add(TextFormatting.GRAY + "Attribute: " + TextFormatting.AQUA + attribute);
        }
    }

    public static String getAttribute(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? tag.getString("Attribute") : "";
    }

    public static void setAttribute(ItemStack stack, String attribute) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setString("Attribute", attribute);
        stack.setTagCompound(tag);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && player.isSneaking() && stack.hasTagCompound()) {
            stack.setTagCompound(null);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }
}
