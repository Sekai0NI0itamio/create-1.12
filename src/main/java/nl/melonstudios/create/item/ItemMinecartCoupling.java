package nl.melonstudios.create.item;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Couples two minecarts so they travel as one train.
 * Reference: MinecartCouplingItem (right-click two carts in turn; the
 * second click creates the coupling). This stores the first cart's id in
 * NBT and reports both clicks; the actual coupling edge belongs to the
 * minecart-anchor/train system, which is the lead's build.
 */
public class ItemMinecartCoupling extends Item {
    public ItemMinecartCoupling() {
        super();
        this.setRegistryName("minecart_coupling");
        this.setUnlocalizedName("create.minecart_coupling");
        this.setMaxStackSize(16);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (hasFirstCart(stack)) {
            tooltip.add(TextFormatting.GRAY + "First cart marked. Right-click a second cart to couple.");
        } else {
            tooltip.add(TextFormatting.GRAY + "Right-click two minecarts in turn to couple them.");
        }
    }

    public static boolean hasFirstCart(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasUniqueId("FirstCart");
    }

    public boolean interactWithCart(ItemStack stack, EntityPlayer player, EntityMinecart target, EnumHand hand) {
        if (player.world.isRemote) {
            return true;
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        UUID cartId = target.getUniqueID();
        if (!hasFirstCart(stack)) {
            tag.setUniqueId("FirstCart", cartId);
            stack.setTagCompound(tag);
            player.sendStatusMessage(new TextComponentString("First cart marked. Right-click a second cart to couple."), true);
        } else if (cartId.equals(tag.getUniqueId("FirstCart"))) {
            player.sendStatusMessage(new TextComponentString("That is the same cart. Pick a different one."), true);
        } else {
            // The train/coupling edge is the lead's build; record the pair for it.
            tag.setUniqueId("SecondCart", cartId);
            stack.setTagCompound(tag);
            player.sendStatusMessage(new TextComponentString("Carts paired."), true);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return true;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, net.minecraft.util.math.BlockPos pos,
            EnumHand hand, net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ) {
        return EnumActionResult.PASS;
    }
}
