package nl.melonstudios.create.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Empty schematic (sneak-right-click two corners to capture) / finished
 * schematic (NBT: corner1, corner2, dim, blocks list). Right-click deploys
 * the ghost at the targeted spot (stored in the schematic table / cannon).
 */
public class ItemSchematic extends Item {
    public ItemSchematic() {
        super();
        this.setMaxStackSize(1);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            items.add(new ItemStack(this, 1, 0));
            items.add(new ItemStack(this, 1, 1));
        }
    }

    public static boolean isFinished(ItemStack stack) {
        return stack.getMetadata() == 1 && stack.hasTagCompound() && stack.getTagCompound().hasKey("Blocks", 9);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
    }

    @Override
    public net.minecraft.util.EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote || stack.getMetadata() != 0 || !player.isSneaking()) {
            return net.minecraft.util.EnumActionResult.PASS;
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        if (!tag.hasKey("Corner1")) {
            tag.setLong("Corner1", pos.toLong());
            player.sendStatusMessage(new TextComponentString("Schematic corner 1 set. Sneak-click the opposite corner."), true);
        } else {
            BlockPos c1 = BlockPos.fromLong(tag.getLong("Corner1"));
            capture(world, c1, pos, tag);
            stack.setMetadata(1);
            player.sendStatusMessage(new TextComponentString("Schematic captured (" + tag.getTagList("Blocks", 10).tagCount() + " blocks)."), true);
        }
        stack.setTagCompound(tag);
        return net.minecraft.util.EnumActionResult.SUCCESS;
    }

    public static void capture(World world, BlockPos c1, BlockPos c2, NBTTagCompound tag) {
        int x1 = Math.min(c1.getX(), c2.getX());
        int y1 = Math.min(c1.getY(), c2.getY());
        int z1 = Math.min(c1.getZ(), c2.getZ());
        int x2 = Math.max(c1.getX(), c2.getX());
        int y2 = Math.max(c1.getY(), c2.getY());
        int z2 = Math.max(c1.getZ(), c2.getZ());
        int dx = Math.min(48, x2 - x1 + 1);
        int dy = Math.min(48, y2 - y1 + 1);
        int dz = Math.min(48, z2 - z1 + 1);
        tag.setIntArray("Size", new int[]{dx, dy, dz});
        tag.setLong("Origin", new BlockPos(x1, y1, z1).toLong());
        tag.setInteger("Dim", world.provider.getDimension());
        NBTTagList blocks = new NBTTagList();
        for (int x = 0; x < dx; x++) {
            for (int y = 0; y < dy; y++) {
                for (int z = 0; z < dz; z++) {
                    BlockPos p = new BlockPos(x1 + x, y1 + y, z1 + z);
                    net.minecraft.block.state.IBlockState s = world.getBlockState(p);
                    if (s.getBlock().isAir(s, world, p)) continue;
                    net.minecraft.tileentity.TileEntity te = world.getTileEntity(p);
                    NBTTagCompound e = new NBTTagCompound();
                    e.setIntArray("Pos", new int[]{x, y, z});
                    e.setString("Block", s.getBlock().getRegistryName().toString());
                    e.setInteger("Meta", s.getBlock().getMetaFromState(s));
                    if (te != null) {
                        NBTTagCompound tenbt = new NBTTagCompound();
                        te.writeToNBT(tenbt);
                        tenbt.removeTag("x");
                        tenbt.removeTag("y");
                        tenbt.removeTag("z");
                        e.setTag("TE", tenbt);
                    }
                    blocks.appendTag(e);
                }
            }
        }
        tag.setTag("Blocks", blocks);
    }
}
