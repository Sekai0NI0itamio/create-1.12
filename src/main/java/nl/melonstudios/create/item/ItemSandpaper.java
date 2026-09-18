package nl.melonstudios.create.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.recipe.SandingRecipes;

public class ItemSandpaper extends Item {
    public ItemSandpaper() {
        super();
        this.setMaxStackSize(1);
        this.setRegistryName("sandpaper");
        this.setUnlocalizedName("create.sandpaper");
        this.setMaxDamage(8);

        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }

    @Override
    public int getItemStackLimit() {
        return 1;
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return 1;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        // Reference SandPaperItem#getUseDuration returns 32.
        return 32;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        // Reference uses UseAnim.EAT for the polishing action.
        return EnumAction.EAT;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack paper = playerIn.getHeldItem(handIn);
        EnumHand otherHand = handIn == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        ItemStack offhand = playerIn.getHeldItem(otherHand);

        if (!SandingRecipes.instance.getResult(offhand).isEmpty()) {
            // Hold-to-polish like the reference: the result is applied in onItemUseFinish.
            playerIn.setActiveHand(handIn);
            return ActionResult.newResult(EnumActionResult.SUCCESS, paper);
        }
        return ActionResult.newResult(EnumActionResult.PASS, paper);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
        if (entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entityLiving;
            EnumHand paperHand = player.getHeldItem(EnumHand.MAIN_HAND).getItem() == this
                    ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
            EnumHand otherHand = paperHand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
            ItemStack target = player.getHeldItem(otherHand);

            ItemStack result = SandingRecipes.instance.getResult(target);
            if (!result.isEmpty() && !target.isEmpty()) {
                player.inventory.placeItemBackInInventory(worldIn, result);
                if (!player.isCreative()) {
                    stack.damageItem(1, player);
                    target.shrink(1);
                }
                worldIn.playSound(null, player.posX, player.posY, player.posZ,
                        SoundInit.item_sandpaper_used, SoundCategory.PLAYERS, 1.0F, 0.9F + worldIn.rand.nextFloat() * 0.2F);
            }
        }
        return stack;
    }
}
