package nl.melonstudios.create.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Blank schematic canvas.
 * Reference: EMPTY_SCHEMATIC (a plain single-stack item consumed by the
 * schematic table). Here it works standalone: sneak-click two corners to
 * capture, which converts it into a finished schematic (ItemSchematic
 * meta 1) using the shared capture routine.
 */
public class ItemEmptySchematic extends Item {
    public ItemEmptySchematic() {
        super();
        this.setRegistryName("empty_schematic");
        this.setUnlocalizedName("create.empty_schematic");
        this.setMaxStackSize(1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "Empty. Sneak-click two corners to capture.");
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote || !player.isSneaking()) {
            return EnumActionResult.PASS;
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        if (!tag.hasKey("Corner1")) {
            tag.setLong("Corner1", pos.toLong());
            stack.setTagCompound(tag);
            player.sendStatusMessage(new TextComponentString("Schematic corner 1 set. Sneak-click the opposite corner."), true);
        } else {
            BlockPos c1 = BlockPos.fromLong(tag.getLong("Corner1"));
            NBTTagCompound finished = new NBTTagCompound();
            ItemSchematic.capture(world, c1, pos, finished);
            ItemStack result = new ItemStack(ItemInit.SCHEMATIC, 1, 1);
            result.setTagCompound(finished);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            if (!player.inventory.addItemStackToInventory(result)) {
                player.dropItem(result, false);
            }
            player.sendStatusMessage(new TextComponentString("Schematic captured ("
                    + finished.getTagList("Blocks", 10).tagCount() + " blocks)."), true);
        }
        return EnumActionResult.SUCCESS;
    }
}
