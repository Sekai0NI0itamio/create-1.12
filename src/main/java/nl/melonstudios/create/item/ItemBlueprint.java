package nl.melonstudios.create.item;

import nl.melonstudios.create.init.CreateTabs;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Crafting blueprint card. Reference: BlueprintItem (hanging recipe entity;
 * assignCompleteRecipe converts a 3x3 recipe into filter stacks).
 * 1.12 simplification: the hanging-entity display needs entity registration
 * (NEEDS-LEAD), so the item is a recipe snapshot card — sneak-use records the
 * offhand result plus the 9 hotbar ingredient stacks; the schematicannon can
 * read them back via getIngredients().
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemBlueprint extends Item {
    public ItemBlueprint() {
        this.setMaxStackSize(1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    public static boolean isWritten(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("Ingredients", 9);
    }

    public static NBTTagList ingredientsOf(ItemStack stack) {
        if (!isWritten(stack)) return new NBTTagList();
        return stack.getTagCompound().getTagList("Ingredients", 10);
    }

    @Nullable
    public static ItemStack resultOf(ItemStack stack) {
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("Result", 10)) return null;
        ItemStack result = new ItemStack(stack.getTagCompound().getCompoundTag("Result"));
        return result.isEmpty() ? null : result;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) return EnumActionResult.PASS;
        ItemStack stack = player.getHeldItem(hand);
        if (worldIn.isRemote) return EnumActionResult.SUCCESS;
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagList grid = new NBTTagList();
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.inventory.getStackInSlot(i);
            if (s.isEmpty() || s == stack) continue;
            ItemStack single = s.copy();
            single.setCount(1);
            NBTTagCompound entry = single.writeToNBT(new NBTTagCompound());
            NBTTagCompound wrapped = new NBTTagCompound();
            wrapped.setInteger("Slot", i);
            wrapped.setTag("Stack", entry);
            grid.appendTag(wrapped);
        }
        tag.setTag("Ingredients", grid);
        ItemStack result = player.getHeldItemOffhand();
        if (!result.isEmpty() && result != stack) {
            tag.setTag("Result", result.copy().writeToNBT(new NBTTagCompound()));
        }
        stack.setTagCompound(tag);
        player.sendStatusMessage(new TextComponentString("Blueprint recorded (" + grid.tagCount() + " ingredients)."), true);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (!isWritten(stack)) {
            tooltip.add(TextFormatting.GRAY + "Empty. Sneak-use to snapshot hotbar + offhand result.");
            return;
        }
        ItemStack result = resultOf(stack);
        tooltip.add(TextFormatting.GOLD + "Result: " + (result == null ? "?" : result.getDisplayName()));
        tooltip.add(TextFormatting.GRAY + "" + ingredientsOf(stack).tagCount() + " ingredient stacks");
    }
}
