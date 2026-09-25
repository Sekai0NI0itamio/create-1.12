package nl.melonstudios.create.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
 * Handheld remote for the redstone-link network.
 * Reference: LinkedControllerItem (single-stack; sneak-use opens the
 * frequency screen, plain use toggles the bound link). Screens are out
 * of scope here, so the bound frequency pair lives in NBT
 * ("FrequencyFirst"/"FrequencySecond"); sneak-use clears the binding
 * until the lectern-controller flow lands.
 */
public class ItemLinkedController extends Item {
    public ItemLinkedController() {
        super();
        this.setRegistryName("linked_controller");
        this.setUnlocalizedName("create.linked_controller");
        this.setMaxStackSize(1);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        String first = getFrequency(stack, "FrequencyFirst");
        String second = getFrequency(stack, "FrequencySecond");
        if (first.isEmpty() && second.isEmpty()) {
            tooltip.add(TextFormatting.GRAY + "Unbound. Sneak-use a Redstone Link to bind it.");
        } else {
            tooltip.add(TextFormatting.GRAY + "Bound to "
                    + TextFormatting.AQUA + (first.isEmpty() ? "<empty>" : first)
                    + TextFormatting.GRAY + " / "
                    + TextFormatting.AQUA + (second.isEmpty() ? "<empty>" : second));
        }
    }

    public static String getFrequency(ItemStack stack, String key) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? tag.getString(key) : "";
    }

    public static void setFrequency(ItemStack stack, String first, String second) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setString("FrequencyFirst", first);
        tag.setString("FrequencySecond", second);
        stack.setTagCompound(tag);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && player.isSneaking() && hand == EnumHand.MAIN_HAND && stack.hasTagCompound()) {
            stack.setTagCompound(null);
            player.sendStatusMessage(new TextComponentString("Link binding cleared."), true);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }
}
