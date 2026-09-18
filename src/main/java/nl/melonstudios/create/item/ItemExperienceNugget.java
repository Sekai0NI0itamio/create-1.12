package nl.melonstudios.create.item;

import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

/**
 * Right-click to redeem the stored experience (3 XP per nugget;
 * sneak redeems a single nugget). Matches reference ExperienceNuggetItem.
 */
public class ItemExperienceNugget extends Item {
    public ItemExperienceNugget() {
        this.setRegistryName("experience_nugget");
        this.setUnlocalizedName("create.experience_nugget");
        this.setRarity(EnumRarity.UNCOMMON);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (world.isRemote) {
            world.playSound(player, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5F, 1.0F);
            return ActionResult.newResult(EnumActionResult.SUCCESS, held);
        }
        int amount = player.isSneaking() ? 1 : held.getCount();
        int total = 3 * amount;
        int orbs = amount == 1 ? 1 : 5;
        int perOrb = Math.max(1, 1 + total / orbs);
        Vec3d look = player.getLookVec();
        for (int i = 0; i < orbs; i++) {
            int value = Math.min(perOrb, total - i * perOrb);
            if (value <= 0) continue;
            EntityXPOrb orb = new EntityXPOrb(world,
                    player.posX + look.x * 0.5, player.posY + 1.2, player.posZ + look.z * 0.5, value);
            orb.motionX = look.x * 0.2 + (world.rand.nextFloat() - 0.5) * 0.1;
            orb.motionY = 0.2;
            orb.motionZ = look.z * 0.2 + (world.rand.nextFloat() - 0.5) * 0.1;
            world.spawnEntity(orb);
        }
        held.shrink(amount);
        return ActionResult.newResult(EnumActionResult.SUCCESS, held);
    }
}
