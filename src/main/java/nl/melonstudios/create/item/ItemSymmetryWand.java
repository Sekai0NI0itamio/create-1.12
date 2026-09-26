package nl.melonstudios.create.item;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Symmetry wand. Reference: SymmetryWandItem (mirror stored as NBT, sneak-use
 * opens the config screen, plain use sets/moves the mirror plane, 5-tick
 * cooldown). 1.12 simplification: no config screen; sneak-use cycles the
 * mirror plane orientation (XY/YZ/XZ), plain use sets the mirror position.
 * Actual placement mirroring hooks into block-place events via NEEDS-LEAD
 * wiring (see getMirror).
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemSymmetryWand extends Item {
    public static final String TAG_SYMMETRY = "symmetry";
    /** 0 = XY plane, 1 = YZ plane, 2 = XZ plane. */
    public static final String[] PLANES = {"XY", "YZ", "XZ"};

    public ItemSymmetryWand() {
        this.setMaxStackSize(1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    public static NBTTagCompound mirrorTag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound root = stack.getTagCompound();
        if (!root.hasKey(TAG_SYMMETRY, 10)) root.setTag(TAG_SYMMETRY, new NBTTagCompound());
        return root.getCompoundTag(TAG_SYMMETRY);
    }

    public static boolean isEnabled(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getCompoundTag(TAG_SYMMETRY).getBoolean("Enabled");
    }

    /** Mirror query for the future place-event handler: null when disabled. */
    @Nullable
    public static Mirror getMirror(ItemStack stack) {
        if (!isEnabled(stack)) return null;
        NBTTagCompound tag = stack.getTagCompound().getCompoundTag(TAG_SYMMETRY);
        if (!tag.hasKey("MirrorPos")) return null;
        return new Mirror(BlockPos.fromLong(tag.getLong("MirrorPos")), tag.getInteger("Plane") % 3);
    }

    public static class Mirror {
        public final BlockPos pos;
        public final int plane;

        Mirror(BlockPos pos, int plane) {
            this.pos = pos;
            this.plane = plane;
        }

        /** Reflect a placement position across the mirror plane. */
        public BlockPos reflect(BlockPos at) {
            switch (this.plane) {
                case 0:
                    return new BlockPos(2 * this.pos.getX() - at.getX(), at.getY(), at.getZ());
                case 1:
                    return new BlockPos(at.getX(), at.getY(), 2 * this.pos.getZ() - at.getZ());
                default:
                    return new BlockPos(at.getX(), 2 * this.pos.getY() - at.getY(), at.getZ());
            }
        }
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (hand != EnumHand.MAIN_HAND) return EnumActionResult.PASS;
        if (!worldIn.isRemote) {
            NBTTagCompound tag = mirrorTag(stack);
            if (player.isSneaking()) {
                int plane = (tag.getInteger("Plane") + 1) % 3;
                tag.setInteger("Plane", plane);
                tag.setBoolean("Enabled", true);
                player.sendStatusMessage(new TextComponentString("Symmetry plane: " + PLANES[plane]), true);
            } else {
                BlockPos at = pos.offset(facing);
                tag.setLong("MirrorPos", at.toLong());
                tag.setBoolean("Enabled", true);
                player.sendStatusMessage(new TextComponentString("Mirror set at "
                        + at.getX() + ", " + at.getY() + ", " + at.getZ()
                        + " (" + PLANES[tag.getInteger("Plane") % 3] + ")"), true);
            }
        }
        player.getCooldownTracker().setCooldown(this, 5);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        Mirror mirror = getMirror(stack);
        if (mirror == null) {
            tooltip.add(TextFormatting.GRAY + "No mirror set. Use on a block to place one.");
        } else {
            tooltip.add(TextFormatting.GOLD + "Mirror (" + PLANES[mirror.plane] + "): "
                    + mirror.pos.getX() + ", " + mirror.pos.getY() + ", " + mirror.pos.getZ());
            tooltip.add(TextFormatting.GRAY + "Sneak-use to rotate the plane.");
        }
    }
}
