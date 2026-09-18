package nl.melonstudios.create.item;

import com.melonstudios.melonlib.misc.Localizer;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.entity.EntityGlue;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.GluedSurface;
import nl.melonstudios.create.util.interfaces.IBypassBlockUse;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemGlue extends Item implements IBypassBlockUse {
    public ItemGlue(int durability) {
        this.canRepair = false;
        this.setHasSubtypes(false);
        // Reference AllItems SUPER_GLUE uses durability(99); clamp any
        // registration value to that so behavior matches 1.20.1.
        this.setMaxDamage(Math.min(durability, 99));
        this.setMaxStackSize(1);

        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState state = worldIn.getBlockState(pos);
        if (state.getBlock().isReplaceable(worldIn, pos)) return EnumActionResult.PASS;
        GluedSurface surface = new GluedSurface(pos, facing);
        List<EntityGlue> entity = worldIn.getEntities(EntityGlue.class, (glue) -> surface.equals(glue.getSurface()));
        if (player.isSneaking()) {
            if (!entity.isEmpty()) {
                player.playSound(SoundEvents.BLOCK_SLIME_BREAK, 1.0F, 0.5F);
                if (worldIn.isRemote) {
                    entity.forEach(EntityGlue::spawnTheSlimes);
                }
                entity.forEach(worldIn::removeEntity);
                return EnumActionResult.SUCCESS;
            }
        } else {
            if (entity.isEmpty()) {
                player.playSound(SoundEvents.BLOCK_SLIME_PLACE, 1.0F, 1.0F);
                if (!player.isCreative()) {
                    player.getHeldItem(hand).damageItem(1, player);
                }
                worldIn.spawnEntity(new EntityGlue(worldIn, surface));
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.FAIL;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(Localizer.translate("item.create.superglue.desc1"));
        tooltip.add(Localizer.translate("item.create.superglue.desc2"));
    }
}
