package nl.melonstudios.create.item;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.block.BlockShaft;
import nl.melonstudios.create.block.actor.BeltSlicer;
import nl.melonstudios.create.block.actor.BlockBeltStraight;
import nl.melonstudios.create.block.state.EnumBeltPart;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.Utils;

public class ItemBeltConnector extends Item {
    public ItemBeltConnector() {
        super();

        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty() || stack.getItem() != this) return EnumActionResult.PASS;
        // Reference BeltConnectorItem: sneaking clears the stored first pulley.
        if (player.isSneaking()) {
            NBTTagCompound held = stack.getTagCompound();
            if (held != null) {
                held.removeTag("LastPos");
                if (held.getSize() == 0) stack.setTagCompound(null);
            }
            return EnumActionResult.SUCCESS;
        }
        IBlockState clicked = worldIn.getBlockState(pos);
        if (clicked.getBlock() instanceof BlockBeltStraight) {
            // Belt-end extension/merging fallback (see BeltSlicer). Normally the
            // belt block already handled this in onBlockActivated before this
            // item use runs; this branch only fires when that was skipped.
            // It must never touch the stored first-pulley tag.
            if (BeltSlicer.useConnector(worldIn, pos, clicked, player, hand, hitX, hitY, hitZ)) {
                return EnumActionResult.SUCCESS;
            }
            return EnumActionResult.FAIL;
        }
        if (clicked.getBlock() != BlockInit.SHAFT) return EnumActionResult.FAIL;
        player.getCooldownTracker().setCooldown(this, 10);
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey("LastPos", 10)) {
            BlockPos last = NBTUtil.getPosFromTag(nbt.getCompoundTag("LastPos"));
            nbt.removeTag("LastPos");
            if (nbt.getSize() == 0) stack.setTagCompound(null);

            IBlockState shaftFrom = worldIn.getBlockState(last);
            IBlockState shaftTo = worldIn.getBlockState(pos);

            if (shaftFrom.getBlock() != BlockInit.SHAFT || shaftTo.getBlock() != BlockInit.SHAFT) {
                return this.failed(player, "Not a shaft", !worldIn.isRemote);
            }
            if (shaftFrom != shaftTo) return this.failed(player, "Invalid shafts", !worldIn.isRemote);
            int mismatching = 0;
            if (last.getX() != pos.getX()) mismatching++;
            if (last.getY() != pos.getY()) mismatching++;
            if (last.getZ() != pos.getZ()) mismatching++;
            if (mismatching != 1) return this.failed(player, "Cannot place diagonal belt", !worldIn.isRemote);
            for (BlockPos between : BlockPos.getAllInBox(last, pos)) {
                IBlockState state = worldIn.getBlockState(between);
                if (state != shaftFrom && !state.getBlock().isReplaceable(worldIn, between)) {
                    return this.failed(player, "Something occludes the belt", !worldIn.isRemote);
                }
            }
            if (last.getX() != pos.getX()) {
                if (shaftFrom.getValue(BlockShaft.AXIS) != EnumFacing.Axis.Z) {
                    return this.failed(player, "Invalid shaft orientation", !worldIn.isRemote);
                }
                if (!worldIn.isRemote) {
                    final BlockPos from = last.getX() < pos.getX() ? last : pos;
                    final BlockPos to = pos.getX() < last.getX() ? last : pos;
                    IBlockState state = BlockInit.BELT_STRAIGHT.getDefaultState()
                            .withProperty(BlockBeltStraight.AXIS, EnumFacing.Axis.X)
                            .withProperty(BlockBeltStraight.VERTICAL, false)
                            .withProperty(BlockBeltStraight.PART, EnumBeltPart.MIDDLE);
                    for (BlockPos bp : BlockPos.getAllInBox(from, to)) {
                        final IBlockState placed;
                        if (from.equals(bp)) placed = state.withProperty(BlockBeltStraight.PART, EnumBeltPart.START);
                        else if (to.equals(bp)) placed = state.withProperty(BlockBeltStraight.PART, EnumBeltPart.END);
                        else if (worldIn.getBlockState(bp) == shaftFrom)
                            placed = state.withProperty(BlockBeltStraight.PART, EnumBeltPart.PULLEY);
                        else placed = state;

                        worldIn.setBlockState(bp, placed);
                    }
                    if (!player.isCreative()) stack.shrink(1);
                    worldIn.playSound(null, pos,
                            SoundEvents.BLOCK_CLOTH_PLACE, SoundCategory.BLOCKS,
                            1.0F, 0.9F + worldIn.rand.nextFloat() * 0.2F
                    );
                }
                return EnumActionResult.SUCCESS;
            }
            if (last.getZ() != pos.getZ()) {
                if (shaftFrom.getValue(BlockShaft.AXIS) != EnumFacing.Axis.X) {
                    return this.failed(player, "Invalid shaft orientation", !worldIn.isRemote);
                }
                if (!worldIn.isRemote) {
                    final BlockPos from = last.getZ() < pos.getZ() ? last : pos;
                    final BlockPos to = pos.getZ() < last.getZ() ? last : pos;
                    IBlockState state = BlockInit.BELT_STRAIGHT.getDefaultState()
                            .withProperty(BlockBeltStraight.AXIS, EnumFacing.Axis.Z)
                            .withProperty(BlockBeltStraight.VERTICAL, false)
                            .withProperty(BlockBeltStraight.PART, EnumBeltPart.MIDDLE);
                    for (BlockPos bp : BlockPos.getAllInBox(from, to)) {
                        final IBlockState placed;
                        if (from.equals(bp)) placed = state.withProperty(BlockBeltStraight.PART, EnumBeltPart.START);
                        else if (to.equals(bp)) placed = state.withProperty(BlockBeltStraight.PART, EnumBeltPart.END);
                        else if (worldIn.getBlockState(bp) == shaftFrom)
                            placed = state.withProperty(BlockBeltStraight.PART, EnumBeltPart.PULLEY);
                        else placed = state;

                        worldIn.setBlockState(bp, placed);
                    }
                    if (!player.isCreative()) stack.shrink(1);
                    worldIn.playSound(null, pos,
                            SoundEvents.BLOCK_CLOTH_PLACE, SoundCategory.BLOCKS,
                            1.0F, 0.9F + worldIn.rand.nextFloat() * 0.2F
                    );
                }
                return EnumActionResult.SUCCESS;
            }
            if (last.getY() != pos.getY()) {
                if (shaftFrom.getValue(BlockShaft.AXIS) == EnumFacing.Axis.Y) {
                    return this.failed(player, "Invalid shaft orientation", !worldIn.isRemote);
                }
                if (!worldIn.isRemote) {
                    final BlockPos from = last.getY() < pos.getY() ? last : pos;
                    final BlockPos to = pos.getY() < last.getY() ? last : pos;
                    IBlockState state = BlockInit.BELT_STRAIGHT.getDefaultState()
                            .withProperty(BlockBeltStraight.AXIS, shaftFrom.getValue(BlockShaft.AXIS) == EnumFacing.Axis.X ? EnumFacing.Axis.Z : EnumFacing.Axis.X)
                            .withProperty(BlockBeltStraight.VERTICAL, true)
                            .withProperty(BlockBeltStraight.PART, EnumBeltPart.MIDDLE);
                    for (BlockPos bp : BlockPos.getAllInBox(from, to)) {
                        final IBlockState placed;
                        if (from.equals(bp)) placed = state.withProperty(BlockBeltStraight.PART, EnumBeltPart.START);
                        else if (to.equals(bp)) placed = state.withProperty(BlockBeltStraight.PART, EnumBeltPart.END);
                        else if (worldIn.getBlockState(bp) == shaftFrom)
                            placed = state.withProperty(BlockBeltStraight.PART, EnumBeltPart.PULLEY);
                        else placed = state;

                        worldIn.setBlockState(bp, placed);
                    }
                    if (!player.isCreative()) stack.shrink(1);
                    worldIn.playSound(null, pos,
                            SoundEvents.BLOCK_CLOTH_PLACE, SoundCategory.BLOCKS,
                            1.0F, 0.9F + worldIn.rand.nextFloat() * 0.2F
                    );
                }
                return EnumActionResult.SUCCESS;
            }
            return this.failed(player, "Unknown problem :(", !worldIn.isRemote);
        } else {
            stack.setTagCompound(nbt != null ? nbt : new NBTTagCompound());
            stack.getTagCompound().setTag("LastPos", NBTUtil.createPosTag(pos));
            if (!worldIn.isRemote) {
                worldIn.playSound(null, pos,
                        SoundEvents.BLOCK_CLOTH_PLACE, SoundCategory.BLOCKS,
                        1.0F, 0.9F + worldIn.rand.nextFloat() * 0.2F
                );
            }
        }
        return EnumActionResult.SUCCESS;
    }

    private EnumActionResult failed(EntityPlayer player, String message, boolean client) {
        if (client) player.sendStatusMessage(new TextComponentTranslation(message), true);
        return EnumActionResult.SUCCESS;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if (isSelected && worldIn.isRemote && (worldIn.getTotalWorldTime() & 3) == 0) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt != null && nbt.hasKey("LastPos", 10)) {
                RayTraceResult select = Minecraft.getMinecraft().objectMouseOver;
                if (select != null && select.typeOfHit == RayTraceResult.Type.BLOCK) {
                    BlockPos last = NBTUtil.getPosFromTag(nbt.getCompoundTag("LastPos"));
                    BlockPos pos = select.getBlockPos();
                    boolean flag = worldIn.getBlockState(last).getBlock() != BlockInit.SHAFT || worldIn.getBlockState(pos).getBlock() != BlockInit.SHAFT;
                    int c = 0;
                    if (last.getX() != pos.getX()) c++;
                    if (last.getY() != pos.getY()) c++;
                    if (last.getZ() != pos.getZ()) c++;
                    if (c == 1) {
                        for (BlockPos itr : BlockPos.getAllInBox(last, pos)) {
                            this.spawnParticlesInBox(itr.getX(), itr.getY(), itr.getZ(), flag && Utils.dist_manh(last, pos) > 16);
                        }
                    }
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void spawnParticlesInBox(double x, double y, double z, boolean flag) {
        float r = flag ? 1.0F : 0.2F;
        float g = flag ? 0.0F : 1.0F;
        float b = flag ? 0.0F : 0.2F;
        CreateLegacy.proxy.spawnRedstoneFX(x+0.25, y+0.25, z+0.25, 0, 0, 0, 1.0F, r, g, b);
        CreateLegacy.proxy.spawnRedstoneFX(x+0.75, y+0.25, z+0.25, 0, 0, 0, 1.0F, r, g, b);
        CreateLegacy.proxy.spawnRedstoneFX(x+0.25, y+0.25, z+0.75, 0, 0, 0, 1.0F, r, g, b);
        CreateLegacy.proxy.spawnRedstoneFX(x+0.75, y+0.25, z+0.75, 0, 0, 0, 1.0F, r, g, b);
        CreateLegacy.proxy.spawnRedstoneFX(x+0.25, y+0.75, z+0.25, 0, 0, 0, 1.0F, r, g, b);
        CreateLegacy.proxy.spawnRedstoneFX(x+0.75, y+0.75, z+0.25, 0, 0, 0, 1.0F, r, g, b);
        CreateLegacy.proxy.spawnRedstoneFX(x+0.25, y+0.75, z+0.75, 0, 0, 0, 1.0F, r, g, b);
        CreateLegacy.proxy.spawnRedstoneFX(x+0.75, y+0.75, z+0.75, 0, 0, 0, 1.0F, r, g, b);
    }
}
