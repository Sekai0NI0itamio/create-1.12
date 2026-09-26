package nl.melonstudios.create.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Schematic paired with a quill, ready to be authored/deployed.
 * Reference: SchematicAndQuillItem (single-stack; consumed by the
 * schematic table / cannon flow). Deployment-table integration is out
 * of scope here, so this carries the finished schematic NBT ("Blocks")
 * and reports its size in the tooltip until the table lands.
 */
public class ItemSchematicAndQuill extends Item {
    public ItemSchematicAndQuill() {
        super();
        this.setRegistryName("schematic_and_quill");
        this.setUnlocalizedName("create.schematic_and_quill");
        this.setMaxStackSize(1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Blocks", 9)) {
            int[] size = stack.getTagCompound().getIntArray("Size");
            if (size.length == 3) {
                tooltip.add(TextFormatting.GOLD + "" + size[0] + " x " + size[1] + " x " + size[2]);
            }
            tooltip.add(TextFormatting.GRAY + "" + stack.getTagCompound().getTagList("Blocks", 10).tagCount() + " blocks");
        } else {
            tooltip.add(TextFormatting.GRAY + "Pairs with a schematic in the schematic table.");
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
    }
}
