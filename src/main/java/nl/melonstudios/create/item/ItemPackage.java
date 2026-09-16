package nl.melonstudios.create.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class ItemPackage extends Item {
    public ItemPackage() {
        super();
        this.setMaxStackSize(1);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            items.add(new ItemStack(this, 1, 0));
            items.add(new ItemStack(this, 1, 1));
            items.add(new ItemStack(this, 1, 2));
            items.add(new ItemStack(this, 1, 3));
        }
    }

    /** Contents: up to 9 stacks under "Items" list. */
    public static void setContents(ItemStack box, ItemStack gathered) {
        NBTTagCompound tag = box.hasTagCompound() ? box.getTagCompound() : new NBTTagCompound();
        NBTTagList list = tag.hasKey("Items", 9) ? tag.getTagList("Items", 10) : new NBTTagList();
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

    public static String getAddress(ItemStack box) {
        if (!box.hasTagCompound()) return "";
        return box.getTagCompound().getString("Address");
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
        player.setHeldItem(hand, ItemStack.EMPTY);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String addr = getAddress(stack);
        String base = super.getItemStackDisplayName(stack);
        return addr.isEmpty() ? base : base + " [" + addr + "]";
    }
}
