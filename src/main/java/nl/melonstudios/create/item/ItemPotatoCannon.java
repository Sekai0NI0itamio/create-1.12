package nl.melonstudios.create.item;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.entity.EntityPotatoProjectile;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Handheld spud launcher.
 * Reference: PotatoCannonItem (durability 100, ammo table in
 * AllPotatoProjectileTypes, Power +20%/lvl, Punch +0.5/lvl, per-type reload
 * ticks as cooldown). Ammo resolved by scanning the player inventory for the
 * first stack matching the table; velocity is look * 2.0 * type multiplier.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemPotatoCannon extends Item {
    public ItemPotatoCannon() {
        // Reference MAX_DAMAGE = 100.
        this.setMaxDamage(100);
        this.setMaxStackSize(1);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    /** damage, reloadTicks, velocityMult, knockback, effectId (see EntityPotatoProjectile). */

    public static EntityPotatoProjectile.AmmoStats statsFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() == Items.POTATO) return new EntityPotatoProjectile.AmmoStats(5.0F, 15, 1.25F, 1.5F, 0, stack);
        if (stack.getItem() == Items.BAKED_POTATO) return new EntityPotatoProjectile.AmmoStats(5.0F, 15, 1.25F, 0.5F, 1, stack);
        if (stack.getItem() == Items.CARROT) return new EntityPotatoProjectile.AmmoStats(4.0F, 12, 1.45F, 0.3F, 0, stack);
        if (stack.getItem() == Items.GOLDEN_CARROT) return new EntityPotatoProjectile.AmmoStats(12.0F, 15, 1.45F, 0.5F, 0, stack);
        if (stack.getItem() == Items.POISONOUS_POTATO) return new EntityPotatoProjectile.AmmoStats(5.0F, 15, 1.25F, 0.05F, 2, stack);
        if (stack.getItem() == Items.CHORUS_FRUIT) return new EntityPotatoProjectile.AmmoStats(3.0F, 15, 1.20F, 0.05F, 3, stack);
        if (stack.getItem() == Items.APPLE) return new EntityPotatoProjectile.AmmoStats(5.0F, 10, 1.45F, 0.5F, 0, stack);
        if (stack.getItem() == Items.GOLDEN_APPLE) return new EntityPotatoProjectile.AmmoStats(1.0F, 100, 1.45F, 0.05F, 4, stack);
        if (stack.getItem() == Items.BEETROOT) return new EntityPotatoProjectile.AmmoStats(2.0F, 5, 1.6F, 0.1F, 0, stack);
        if (stack.getItem() == Items.MELON) return new EntityPotatoProjectile.AmmoStats(3.0F, 8, 1.45F, 0.1F, 0, stack);
        return null;
    }

    /** First inventory stack (incl. hands) the cannon accepts, or EMPTY. */
    public static ItemStack findAmmo(EntityPlayer player) {
        NonNullList<ItemStack> inv = player.inventory.mainInventory;
        for (int pass = 0; pass < 2; pass++) {
            for (ItemStack stack : inv) {
                if (pass == 0 && stack.isEmpty()) continue;
                if (statsFor(stack) != null) return stack;
            }
            inv = player.inventory.offHandInventory;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack held = playerIn.getHeldItem(handIn);
        ItemStack ammo = findAmmo(playerIn);
        if (ammo.isEmpty()) return new ActionResult<>(EnumActionResult.PASS, held);

        EntityPotatoProjectile.AmmoStats stats = statsFor(ammo);
        if (stats == null) return new ActionResult<>(EnumActionResult.PASS, held);

        if (!worldIn.isRemote) {
            int power = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, held);
            int punch = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, held);
            float damage = stats.damage * (1.0F + power * 0.2F);
            float knockback = stats.knockback + punch * 0.5F;

            EntityPotatoProjectile shot = new EntityPotatoProjectile(worldIn, playerIn,
                    damage, stats.reloadTicks, stats.velocity, knockback, stats.effect, ammo.copy());
            shot.shoot(playerIn, playerIn.rotationPitch, playerIn.rotationYaw, 0.0F, 2.0F * stats.velocity, 1.0F);
            worldIn.spawnEntity(shot);
            worldIn.playSound(null, playerIn.posX, playerIn.posY, playerIn.posZ,
                    SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.8F, 0.7F + worldIn.rand.nextFloat() * 0.4F);

            if (!playerIn.capabilities.isCreativeMode) {
                ammo.shrink(1);
                held.damageItem(1, playerIn);
            }
        }
        playerIn.getCooldownTracker().setCooldown(this, stats.reloadTicks);
        return new ActionResult<>(EnumActionResult.SUCCESS, held);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "Launches potatoes and produce.");
        tooltip.add(TextFormatting.DARK_GREEN + " Power: +20% dmg/lvl, Punch: +0.5 kb/lvl");
    }
}
