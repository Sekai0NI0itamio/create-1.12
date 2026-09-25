package nl.melonstudios.create.item;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.init.SoundInit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * Terrain zapper (Worldshaper equivalent).
 * Reference: ZapperItem + WorldshaperItem (range 128, cooldown 2, selected
 * block stored as NBT "BlockUsed", pattern NBT "Pattern", shift-use picks the
 * block / opens the GUI, firing places the block along the ray).
 * 1.12 simplification: no config screen; sneak-use on a block samples it,
 * sneak-use in air cycles the pattern, plain use fires.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemZapperWorldshaper extends Item {
    /** Reference WorldshaperItem values. */
    public static final int ZAP_RANGE = 128;
    public static final int COOLDOWN_TICKS = 2;

    public enum Pattern {
        SOLID, CHECKERED, INVERSE_CHECKERED, CHANCE_25, CHANCE_50, CHANCE_75;

        public static Pattern of(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null || !tag.hasKey("Pattern", 8)) return SOLID;
            try {
                return valueOf(tag.getString("Pattern"));
            } catch (IllegalArgumentException e) {
                return SOLID;
            }
        }
    }

    public ItemZapperWorldshaper() {
        this.setMaxStackSize(1);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    private static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }

    @Nullable
    public static IBlockState selectedBlock(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("BlockUsed", 8)) return null;
        Block block = Block.getBlockFromName(tag.getString("BlockUsed"));
        if (block == null || block == Blocks.AIR) return null;
        int meta = tag.getInteger("BlockMeta");
        try {
            return block.getStateFromMeta(meta);
        } catch (Exception e) {
            return block.getDefaultState();
        }
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.isSneaking()) return EnumActionResult.PASS;
        if (worldIn.isRemote) return EnumActionResult.SUCCESS;
        IBlockState state = worldIn.getBlockState(pos);
        if (state.getBlock() == Blocks.AIR) return EnumActionResult.PASS;
        NBTTagCompound tag = tag(stack);
        tag.setString("BlockUsed", state.getBlock().getRegistryName().toString());
        tag.setInteger("BlockMeta", state.getBlock().getMetaFromState(state));
        player.sendStatusMessage(new TextComponentString("Zapper block set: "
                + state.getBlock().getLocalizedName()), true);
        player.getCooldownTracker().setCooldown(this, 10);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (playerIn.isSneaking()) {
            if (!worldIn.isRemote) {
                Pattern next = Pattern.values()[(Pattern.of(stack).ordinal() + 1) % Pattern.values().length];
                tag(stack).setString("Pattern", next.name());
                playerIn.sendStatusMessage(new TextComponentString("Zapper pattern: " + next.name()), true);
                playerIn.getCooldownTracker().setCooldown(this, 10);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        IBlockState toPlace = selectedBlock(stack);
        if (toPlace == null) {
            if (!worldIn.isRemote) {
                playerIn.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "Sneak-use a block first to load the zapper."), true);
                playerIn.playSound(SoundInit.deny, 1.0F, 1.0F);
            }
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        RayTraceResult ray = playerIn.rayTrace(ZAP_RANGE, 1.0F);
        if (ray == null || ray.typeOfHit != RayTraceResult.Type.BLOCK) {
            playerIn.getCooldownTracker().setCooldown(this, COOLDOWN_TICKS);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (worldIn.isRemote) return new ActionResult<>(EnumActionResult.SUCCESS, stack);

        List<BlockPos> targets = patternTargets(ray.getBlockPos(), ray.sideHit, Pattern.of(stack));
        int placed = 0;
        for (BlockPos target : targets) {
            BlockPos at = worldIn.isAirBlock(target) ? target : target.offset(ray.sideHit);
            if (!worldIn.isAirBlock(at) && !worldIn.getBlockState(at).getBlock().isReplaceable(worldIn, at)) continue;
            if (!playerIn.canPlayerEdit(at, ray.sideHit, stack)) continue;
            if (!playerIn.capabilities.isCreativeMode && !consumeBlock(playerIn, toPlace)) break;
            worldIn.setBlockState(at, toPlace, 3);
            placed++;
            if (placed >= 9) break;
        }
        worldIn.playSound(null, ray.getBlockPos(), toPlace.getBlock().getSoundType().getPlaceSound(),
                SoundCategory.BLOCKS, 1.0F, 0.9F + worldIn.rand.nextFloat() * 0.2F);
        playerIn.getCooldownTracker().setCooldown(this, COOLDOWN_TICKS);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private static boolean consumeBlock(EntityPlayer player, IBlockState state) {
        ItemStack need = new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state));
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack s = player.inventory.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() == need.getItem() && s.getMetadata() == need.getMetadata()) {
                s.shrink(1);
                if (s.isEmpty()) player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    static List<BlockPos> patternTargets(BlockPos origin, EnumFacing face, Pattern pattern) {
        List<BlockPos> out = new ArrayList<>();
        out.add(origin);
        if (pattern == Pattern.SOLID) return out;
        EnumFacing u = face.getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : EnumFacing.UP;
        EnumFacing v = face.getAxis() == EnumFacing.Axis.Y ? EnumFacing.EAST : EnumFacing.EAST;
        if (face.getAxis() == EnumFacing.Axis.X) {
            u = EnumFacing.UP;
            v = EnumFacing.SOUTH;
        } else if (face.getAxis() == EnumFacing.Axis.Z) {
            u = EnumFacing.UP;
            v = EnumFacing.EAST;
        }
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos p = origin.offset(u, a).offset(v, b);
                int hash = p.getX() + p.getY() + p.getZ();
                switch (pattern) {
                    case CHECKERED:
                        if (hash % 2 == 0) out.add(p);
                        break;
                    case INVERSE_CHECKERED:
                        if (hash % 2 != 0) out.add(p);
                        break;
                    case CHANCE_25:
                        if (Math.abs(hash * 31 % 100) < 25) out.add(p);
                        break;
                    case CHANCE_50:
                        if (Math.abs(hash * 31 % 100) < 50) out.add(p);
                        break;
                    case CHANCE_75:
                        if (Math.abs(hash * 31 % 100) < 75) out.add(p);
                        break;
                    default:
                        break;
                }
            }
        }
        return out;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        IBlockState selected = selectedBlock(stack);
        tooltip.add(TextFormatting.GRAY + "Pattern: " + Pattern.of(stack).name());
        tooltip.add(TextFormatting.DARK_GRAY + "Using: "
                + (selected == null ? "nothing (sneak-use a block)" : selected.getBlock().getLocalizedName()));
    }
}
