package nl.melonstudios.create.block.deco;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.tileentity.deco.TileEntityCopycat;

/**
 * Shared right-click / break handling for the copycat family, translated
 * from the reference CopycatBlock use/onWrenched flow into 1.12 idioms:
 *
 * - Right-click a bare copycat with a solid full-cube block item to store it
 *   as the mimic (one item consumed outside creative, like the reference).
 * - Sneak-right-click with an empty hand to clear the mimic.
 * - Breaking the block drops the stored mimic item back, mirroring the
 *   reference wrench-strip refund.
 */
public final class CopycatMimicHelper {
    private CopycatMimicHelper() {}

    public static boolean onActivated(World world, BlockPos pos, EntityPlayer player, EnumHand hand) {
        if (world.isRemote || player == null) return true;
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityCopycat)) return false;
        TileEntityCopycat copycat = (TileEntityCopycat) te;

        ItemStack held = player.getHeldItem(hand);
        if (copycat.hasMimic()) {
            if (player.isSneaking() && held.isEmpty()) {
                if (!player.isCreative()) {
                    player.addItemStackToInventory(copycat.getConsumed().copy());
                }
                copycat.clearMimic();
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
                return true;
            }
            return false;
        }

        if (!held.isEmpty() && held.getItem() instanceof ItemBlock && !player.isSneaking()) {
            Block block = ((ItemBlock) held.getItem()).getBlock();
            IBlockState candidate;
            try {
                candidate = block.getStateFromMeta(held.getMetadata());
            } catch (Exception e) {
                return false;
            }
            if (!candidate.isFullCube() || !candidate.isOpaqueCube()) return false;
            copycat.setMimic(held, held.getMetadata());
            if (!player.isCreative()) held.shrink(1);
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            return true;
        }
        return false;
    }

    public static void dropMimic(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCopycat) {
            TileEntityCopycat copycat = (TileEntityCopycat) te;
            if (copycat.hasMimic()) {
                Block.spawnAsEntity(world, pos, copycat.getConsumed().copy());
                copycat.clearMimic();
            }
        }
        world.removeTileEntity(pos);
    }

    public static TileEntityCopycat getCopycat(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileEntityCopycat ? (TileEntityCopycat) te : null;
    }

    public static boolean isValidMimicTarget(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) return false;
        Block block = ((ItemBlock) stack.getItem()).getBlock();
        IBlockState candidate;
        try {
            candidate = block.getStateFromMeta(stack.getMetadata());
        } catch (Exception e) {
            return false;
        }
        return candidate.isFullCube() && candidate.isOpaqueCube();
    }

    public static EnumFacing oppositeOrNull(EnumFacing facing) {
        return facing == null ? null : facing.getOpposite();
    }
}
