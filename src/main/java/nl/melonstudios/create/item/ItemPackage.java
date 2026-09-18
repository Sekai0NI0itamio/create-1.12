package nl.melonstudios.create.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import nl.melonstudios.create.init.SoundInit;

import javax.annotation.Nullable;
import java.util.List;

public class ItemPackage extends Item {
    public ItemPackage() {
        super();
        this.setMaxStackSize(1);
        this.setMaxDamage(0);
        // Single box item: the reference uses one registered item per style,
        // the backport registers one item, so no subtypes or meta variants.
        this.setHasSubtypes(false);
    }

    /** Contents: up to 9 stacks under "Items" list. */
    public static void setContents(ItemStack box, ItemStack gathered) {
        NBTTagCompound tag = box.hasTagCompound() ? box.getTagCompound() : new NBTTagCompound();
        NBTTagList list = tag.hasKey("Items", 9) ? tag.getTagList("Items", 10) : new NBTTagList();
        if (list.tagCount() >= 9) return;
        list.appendTag(gathered.writeToNBT(new NBTTagCompound()));
        tag.setTag("Items", list);
        box.setTagCompound(tag);
    }

    public static NonNullList<ItemStack> getContents(ItemStack box) {
        NonNullList<ItemStack> out = NonNullList.create();
        if (!box.hasTagCompound()) return out;
        NBTTagList list = box.getTagCompound().getTagList("Items", 10);
        for (int i = 0; i < list.tagCount() && i < 9; i++) {
            out.add(new ItemStack(list.getCompoundTagAt(i)));
        }
        return out;
    }

    public static void setAddress(ItemStack box, String address) {
        NBTTagCompound tag = box.hasTagCompound() ? box.getTagCompound() : new NBTTagCompound();
        tag.setString("Address", address);
        box.setTagCompound(tag);
    }

    public static void clearAddress(ItemStack box) {
        if (box.hasTagCompound()) {
            box.getTagCompound().removeTag("Address");
        }
    }

    public static String getAddress(ItemStack box) {
        if (!box.hasTagCompound()) return "";
        return box.getTagCompound().getString("Address");
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        String address = getAddress(stack);
        if (!address.trim().isEmpty()) {
            tooltip.add(TextFormatting.GOLD + "\u2192 " + address);
        }
        if (!stack.hasTagCompound()) return;
        NonNullList<ItemStack> contents = getContents(stack);
        int visible = 0;
        int skipped = 0;
        for (ItemStack s : contents) {
            if (s.isEmpty()) continue;
            if (visible > 2) {
                skipped++;
                continue;
            }
            visible++;
            tooltip.add(TextFormatting.GRAY + s.getDisplayName() + " x" + s.getCount());
        }
        if (skipped > 0) {
            tooltip.add(TextFormatting.ITALIC + "" + I18n.format("container.shulkerBox.more", skipped));
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack box = player.getHeldItem(hand);
        if (world.isRemote || box.isEmpty()) return new ActionResult<>(EnumActionResult.PASS, box);
        if (!player.isSneaking()) return new ActionResult<>(EnumActionResult.PASS, box);
        // Sneak-right-click unpacks the package into the player inventory.
        NonNullList<ItemStack> contents = getContents(box);
        if (contents.isEmpty()) return new ActionResult<>(EnumActionResult.PASS, box);
        for (ItemStack s : contents) {
            if (!player.inventory.addItemStackToInventory(s.copy())) {
                player.dropItem(s.copy(), false);
            }
        }
        player.sendStatusMessage(new TextComponentString("Unpacked " + contents.size() + " stack(s)."), true);
        world.playSound(null, player.getPosition(), SoundInit.package_pop,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        player.setHeldItem(hand, ItemStack.EMPTY);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    // Address stays in the tooltip (gold arrow line); the display name itself is
    // the plain lang name, matching the reference getDescriptionId behaviour.
}
