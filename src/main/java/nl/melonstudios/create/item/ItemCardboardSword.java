package nl.melonstudios.create.item;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityEndermite;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import nl.melonstudios.create.init.ItemInit;

/**
 * Mostly harmless cardboard sword: burnable (1000 ticks), Knockback-only
 * enchantments, and hits deal no damage to non-arthropods — victims are
 * knocked back (and slowed if passive) with a wooden bonk instead.
 * Simplified backport of the reference CardboardSwordItem.
 */
@Mod.EventBusSubscriber(modid = "create")
public class ItemCardboardSword extends ItemSword {
    public static final ToolMaterial CARDBOARD =
            EnumHelper.addToolMaterial("CARDBOARD", 0, 0, 1.0F, 2.0F, 1);

    public ItemCardboardSword() {
        super(CARDBOARD);
        this.setRegistryName("cardboard_sword");
        this.setUnlocalizedName("create.cardboard_sword");
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public int getItemBurnTime(ItemStack stack) {
        return 1000;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == Enchantments.KNOCKBACK;
    }

    private static boolean isArthropod(EntityLivingBase target) {
        return target instanceof EntitySpider || target instanceof EntityCaveSpider
                || target instanceof EntitySilverfish || target instanceof EntityEndermite;
    }

    @SubscribeEvent
    public static void cardboardSwordsMakeNoiseOnClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof ItemCardboardSword)) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (event.getWorld().isRemote) return;
        event.getWorld().playSound(null, event.getPos(),
                SoundEvents.BLOCK_WOOD_HIT, SoundCategory.PLAYERS, 0.5F, 1.85F);
    }

    @SubscribeEvent
    public static void cardboardSwordsCannotHurtYou(LivingAttackEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityLivingBase)) return;
        EntityLivingBase attacker = (EntityLivingBase) event.getSource().getTrueSource();
        if (!(attacker.getHeldItemMainhand().getItem() instanceof ItemCardboardSword)) return;
        EntityLivingBase target = (EntityLivingBase) event.getEntityLiving();
        if (isArthropod(target)) return;

        event.setCanceled(true);
        target.world.playSound(null, target.posX, target.posY, target.posZ,
                SoundEvents.BLOCK_WOOD_HIT, SoundCategory.PLAYERS, 0.75F, 1.85F);

        double strength = 2.0 + EnchantmentHelper.getKnockbackModifier(attacker);
        if (attacker.isSprinting()) strength += 1.0;
        float yaw = attacker.rotationYaw;
        target.dismountRidingEntity();
        target.knockBack(attacker, (float) strength,
                MathHelper.sin(yaw * (float) Math.PI / 180.0F),
                -MathHelper.cos(yaw * (float) Math.PI / 180.0F));
        if (target.isNonBoss()
                && !(target instanceof net.minecraft.entity.player.EntityPlayer)
                && !(target instanceof net.minecraft.entity.monster.EntityMob)) {
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 9));
        }
        attacker.motionX *= 0.6;
        attacker.motionZ *= 0.6;
        attacker.setSprinting(false);
    }
}
