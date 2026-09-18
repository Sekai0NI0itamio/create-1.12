package nl.melonstudios.create.item;

import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

public class ItemTreeFertilizer extends Item {
    public ItemTreeFertilizer() {
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState state = worldIn.getBlockState(pos);
        if (state.getBlock() instanceof BlockSapling) {
            BlockSapling sapling = (BlockSapling) state.getBlock();

            if (worldIn.isRemote) {
                // Matches the reference client path (bonemeal growth particles).
                worldIn.playEvent(2005, pos, 0);
                return EnumActionResult.SUCCESS;
            }
            sapling.generateTree(worldIn, pos, state, worldIn.rand);
            if (!player.isCreative()) {
                player.getHeldItem(hand).shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }
}
