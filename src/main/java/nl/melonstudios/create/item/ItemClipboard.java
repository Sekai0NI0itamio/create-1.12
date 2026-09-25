package nl.melonstudios.create.item;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Portable clipboard data item. Reference: ClipboardBlockItem/ClipboardEntry
 * (an ordered entry list stored on the item; the block mirrors it with
 * null-safety). Entries are plain strings here; the in-world screen is out
 * of scope (no GUI precedent in this port) so entries are managed by
 * sneak-interacting with the clipboard block.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemClipboard extends ItemBlock {
    public ItemClipboard() {
        super(BlockInit.CLIPBOARD);
        this.setRegistryName("clipboard");
        this.setMaxStackSize(1);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    public static NBTTagList entriesOf(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("Entries", 9)) return new NBTTagList();
        return tag.getTagList("Entries", 10);
    }

    public static void setEntries(ItemStack stack, NBTTagList entries) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setTag("Entries", entries);
    }

    public static boolean isWritten(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("Entries", 9)
                && !stack.getTagCompound().getTagList("Entries", 10).hasNoTags();
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // Placement of the block form is handled by the ItemBlock wiring
        // (NEEDS-LEAD registration); this item only carries data.
        return EnumActionResult.PASS;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        int n = entriesOf(stack).tagCount();
        if (n == 0) {
            tooltip.add(TextFormatting.GRAY + "Empty. Sneak-use a clipboard block to sync.");
        } else {
            tooltip.add(TextFormatting.GOLD + "" + n + " entries");
            NBTTagList list = entriesOf(stack);
            for (int i = 0; i < Math.min(4, list.tagCount()); i++) {
                tooltip.add(TextFormatting.GRAY + "- " + list.getCompoundTagAt(i).getString("Text"));
            }
        }
    }
}
