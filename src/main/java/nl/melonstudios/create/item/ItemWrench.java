package nl.melonstudios.create.item;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.util.CreateTagHelper;

public class ItemWrench extends Item {
    public ItemWrench() {
        this.setMaxStackSize(1);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
                                      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return EnumActionResult.PASS;
        if (!player.canPlayerEdit(pos, facing, player.getHeldItem(hand))) return EnumActionResult.PASS;
        IBlockState state = worldIn.getBlockState(pos);
        if (player.isSneaking()) {
            if (CreateTagHelper.isWrenchPickup(state)) {
                NonNullList<ItemStack> drops = NonNullList.create();
                state.getBlock().getDrops(drops, worldIn, pos, state, 0);
                for (ItemStack stack : drops) player.inventory.addItemStackToInventory(stack);
                worldIn.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                worldIn.playSound(null, pos, SoundInit.item_wrench_used_rotate, SoundCategory.PLAYERS,
                        1.0F, 0.9F + worldIn.rand.nextFloat() * 0.2F);
                worldIn.playSound(null, pos, SoundInit.item_wrench_used_dismantle, SoundCategory.PLAYERS,
                        1.0F, 0.9F + worldIn.rand.nextFloat() * 0.2F);
                return EnumActionResult.SUCCESS;
            }
        }
        if (state.getBlock() instanceof IWrenchable) {
            if (((IWrenchable)state.getBlock()).onWrenched(worldIn, pos, state, facing, hitX, hitY, hitZ)) {
                worldIn.playSound(null, pos, SoundInit.item_wrench_used_rotate, SoundCategory.PLAYERS,
                        1.0F, 0.9F + worldIn.rand.nextFloat() * 0.2F);
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.PASS;
    }
}
