package nl.melonstudios.create.item;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.block.funnel.BlockFunnelBase;
import nl.melonstudios.create.block.funnel.BlockFunnelDown;
import nl.melonstudios.create.block.funnel.BlockFunnelWall;
import nl.melonstudios.create.block.state.EnumFunnelState;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.FunnelSet;
import nl.melonstudios.create.util.FunnelSets;
import nl.melonstudios.create.util.interfaces.IBypassBlockUse;

public class ItemBlockFunnel extends Item implements IBypassBlockUse {
    private final String set;
    public ItemBlockFunnel(String set) {
        this.set = set;

        this.setRegistryName("funnel_" + set);
        String modid = Loader.instance().activeModContainer().getModId();
        this.setUnlocalizedName(modid + ".funnel_" + set);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState oldState = worldIn.getBlockState(pos);
        Block oldBlock = oldState.getBlock();
        if (!oldBlock.isReplaceable(worldIn, pos)) {
            pos = pos.offset(facing);
        }

        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && player.canPlayerEdit(pos, facing, held)) {
            FunnelSet set = FunnelSets.get(this.set);
            if (facing.getAxis().isHorizontal()) {
                IBlockState placed = set.getWall().getDefaultState()
                        .withProperty(BlockFunnelWall.FACING, facing)
                        .withProperty(BlockFunnelWall.POWERED, BlockKineticBase.isPosPowered(worldIn, pos))
                        .withProperty(BlockFunnelWall.FUNNEL_STATE, player.isSneaking() ? EnumFunnelState.INSERTING : EnumFunnelState.EXTRACTING);
                if (worldIn.mayPlace(placed.getBlock(), pos, false, facing, player)) {
                    if (worldIn.setBlockState(pos, placed,
                            11
                    )) {
                        IBlockState state = worldIn.getBlockState(pos);
                        SoundType soundType = state.getBlock().getSoundType(state, worldIn, pos, player);
                        worldIn.playSound(player, pos, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) * 0.5F, soundType.getPitch() * 0.8F);
                        held.shrink(1);
                    }
                    return EnumActionResult.SUCCESS;
                }
            } else {
                IBlockState placed = set.getDown().getDefaultState()
                        .withProperty(BlockFunnelDown.FUNNEL_STATE, player.isSneaking() ? EnumFunnelState.INSERTING : EnumFunnelState.EXTRACTING)
                        .withProperty(BlockFunnelBase.POWERED, BlockKineticBase.isPosPowered(worldIn, pos));
                if (worldIn.mayPlace(placed.getBlock(), pos, false, facing, player)
                        && set.getDown().canPlaceBlockAt(worldIn, pos)) {
                    if (worldIn.setBlockState(pos, placed, 11)) {
                        IBlockState state = worldIn.getBlockState(pos);
                        SoundType soundType = state.getBlock().getSoundType(state, worldIn, pos, player);
                        worldIn.playSound(player, pos, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) * 0.5F, soundType.getPitch() * 0.8F);
                        held.shrink(1);
                    }
                    return EnumActionResult.SUCCESS;
                }
            }
        }
        return EnumActionResult.FAIL;
    }

    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        FunnelSet set = FunnelSets.get(this.set);
        if (side.getAxis().isHorizontal()) {
            return world.setBlockState(pos, set.getWall().getDefaultState()
                    .withProperty(BlockFunnelWall.FACING, side)
                    .withProperty(BlockFunnelWall.POWERED, BlockKineticBase.isPosPowered(world, pos))
                    .withProperty(BlockFunnelWall.FUNNEL_STATE, player.isSneaking() ? EnumFunnelState.INSERTING : EnumFunnelState.EXTRACTING),
                    11
            );
        }
        return world.setBlockState(pos, set.getDown().getDefaultState()
                .withProperty(BlockFunnelDown.FUNNEL_STATE, player.isSneaking() ? EnumFunnelState.INSERTING : EnumFunnelState.EXTRACTING)
                .withProperty(BlockFunnelBase.POWERED, BlockKineticBase.isPosPowered(world, pos)),
                11
        );
    }
}
