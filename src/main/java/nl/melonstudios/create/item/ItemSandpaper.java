package nl.melonstudios.create.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
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

        // Reference also picks a polishable item off the ground at the look target.
        RayTraceResult ray = this.rayTrace(worldIn, playerIn, false);
        if (ray != null && ray.hitVec != null) {
            Vec3d hit = ray.hitVec;
            AxisAlignedBB box = new AxisAlignedBB(hit.x - 1, hit.y - 1, hit.z - 1,
                    hit.x + 1, hit.y + 1, hit.z + 1);
            for (EntityItem entity : worldIn.getEntitiesWithinAABB(EntityItem.class, box)) {
                if (entity.isDead) {
                    continue;
                }
                if (entity.getDistanceToEntity(playerIn) > 3) {
                    continue;
                }
                ItemStack stack = entity.getItem();
                if (stack.isEmpty() || SandingRecipes.instance.getResult(stack).isEmpty()) {
                    continue;
                }
                ItemStack toPolish = stack.splitStack(1);
                if (stack.isEmpty()) {
                    entity.setDead();
                } else {
                    entity.setItem(stack);
                }
                if (paper.getTagCompound() == null) {
                    paper.setTagCompound(new NBTTagCompound());
                }
                paper.getTagCompound().setTag("Polishing", toPolish.serializeNBT());
                playerIn.setActiveHand(handIn);
                return ActionResult.newResult(EnumActionResult.SUCCESS, paper);
            }
        }
        return ActionResult.newResult(EnumActionResult.PASS, paper);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
        if (entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entityLiving;
            ItemStack toPolish = ItemStack.EMPTY;
            boolean fromTag = false;
            if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("Polishing")) {
                toPolish = new ItemStack(stack.getTagCompound().getCompoundTag("Polishing"));
                stack.getTagCompound().removeTag("Polishing");
                fromTag = true;
            }
            EnumHand paperHand = player.getHeldItem(EnumHand.MAIN_HAND).getItem() == this
                    ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
            EnumHand otherHand = paperHand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
            ItemStack target = fromTag ? toPolish : player.getHeldItem(otherHand);

            ItemStack result = SandingRecipes.instance.getResult(target);
            if (!result.isEmpty() && !target.isEmpty()) {
                if (worldIn.isRemote) {
                    Vec3d eye = entityLiving.getPositionEyes(1.0F);
                    for (int i = 0; i < 8; i++) {
                        worldIn.spawnParticle(EnumParticleTypes.ITEM_CRACK,
                                eye.x, eye.y, eye.z,
                                (worldIn.rand.nextDouble() - 0.5D) * 0.2D,
                                worldIn.rand.nextDouble() * 0.2D,
                                (worldIn.rand.nextDouble() - 0.5D) * 0.2D,
                                Item.getIdFromItem(target.getItem()), target.getMetadata());
                    }
                } else {
                    player.inventory.placeItemBackInInventory(worldIn, result);
                    if (target.getItem().hasContainerItem(target)) {
                        player.inventory.placeItemBackInInventory(worldIn,
                                target.getItem().getContainerItem(target));
                    }
                    if (!fromTag && !player.isCreative()) {
                        target.shrink(1);
                    }
                    if (!player.isCreative()) {
                        stack.damageItem(1, player);
                    }
                    worldIn.playSound(null, player.posX, player.posY, player.posZ,
                            SoundInit.item_sandpaper_used, SoundCategory.PLAYERS, 1.0F, 0.9F + worldIn.rand.nextFloat() * 0.2F);
                }
            } else if (fromTag && !worldIn.isRemote) {
                player.inventory.placeItemBackInInventory(worldIn, target);
            }
        }
        return stack;
    }
}
