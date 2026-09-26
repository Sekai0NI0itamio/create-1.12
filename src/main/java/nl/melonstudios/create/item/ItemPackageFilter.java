package nl.melonstudios.create.item;

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
 * Filter that routes packages by their address string.
 * Reference: PackageFilterItem (summary line renders "-> address" in gold;
 * the filter holds no item list). Sneak-use clears the stored address.
 */
public class ItemPackageFilter extends Item {
    public ItemPackageFilter() {
        super();
        this.setRegistryName("package_filter");
        this.setUnlocalizedName("create.package_filter");
        this.setMaxStackSize(64);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        String address = getAddress(stack);
        if (!address.isEmpty()) {
            tooltip.add(TextFormatting.GRAY + "-> " + TextFormatting.GOLD + address);
        }
    }

    public static String getAddress(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? tag.getString("Address").trim() : "";
    }

    public static void setAddress(ItemStack stack, String address) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setString("Address", address);
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
