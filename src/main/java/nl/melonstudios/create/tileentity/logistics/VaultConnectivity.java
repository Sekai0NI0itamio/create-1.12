package nl.melonstudios.create.tileentity.logistics;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.block.logistics.BlockVault;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Multiblock formation for item vaults, mirroring the 1.20.1 vault rules:
 * square cross-section up to 3x3, length up to 3x the width, single axis.
 * Controller is the minimum corner; LARGE renders when width is 3.
 */
public final class VaultConnectivity {
    static final int MAX_WIDTH = 3;

    private VaultConnectivity() {
    }

    static boolean isVault(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof BlockVault;
    }

    static EnumFacing.Axis vaultAxis(World world, BlockPos pos) {
        return world.getBlockState(pos).getValue(BlockVault.AXIS);
    }

    static void requestFormAround(World world, BlockPos pos) {
        for (EnumFacing side : EnumFacing.VALUES) {
            TileEntity te = world.getTileEntity(pos.offset(side));
            if (te instanceof TileEntityVault) {
                ((TileEntityVault) te).requestForm();
            }
        }
    }

    static void form(TileEntityVault root) {
        World world = root.getWorld();
        BlockPos rootPos = root.getPos();
        root.clearNeedsForm();
        if (world == null || world.isRemote) {
            return;
        }
        if (!isVault(world, rootPos)) {
            return;
        }
        EnumFacing.Axis axis = vaultAxis(world, rootPos);

        Set<BlockPos> members = new LinkedHashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        open.add(rootPos);
        while (!open.isEmpty()) {
            BlockPos p = open.poll();
            if (!members.add(p)) {
                continue;
            }
            if (!isVault(world, p) || vaultAxis(world, p) != axis) {
                members.remove(p);
                continue;
            }
            for (EnumFacing side : EnumFacing.VALUES) {
                BlockPos q = p.offset(side);
                if (!members.contains(q) && isVault(world, q) && vaultAxis(world, q) == axis) {
                    open.add(q);
                }
            }
        }
        if (members.isEmpty()) {
            return;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : members) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getZ() < minZ) minZ = p.getZ();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() > maxY) maxY = p.getY();
            if (p.getZ() > maxZ) maxZ = p.getZ();
        }
        int sx = maxX - minX + 1;
        int sy = maxY - minY + 1;
        int sz = maxZ - minZ + 1;

        int width, length;
        if (axis == EnumFacing.Axis.X) {
            width = Math.max(sy, sz);
            length = sx;
            if (sy != sz) {
                dissolve(world, members);
                return;
            }
        } else {
            width = Math.max(sx, sz);
            length = sz;
            if (sx != sz) {
                dissolve(world, members);
                return;
            }
        }
        if (width < 1 || width > MAX_WIDTH || length < 1 || length > width * MAX_WIDTH
                || members.size() != width * width * length) {
            dissolve(world, members);
            return;
        }

        BlockPos controller = new BlockPos(minX, minY, minZ);
        List<TileEntityVault> tes = new ArrayList<>();
        for (BlockPos p : members) {
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileEntityVault) {
                tes.add((TileEntityVault) te);
            }
        }
        if (tes.size() != members.size()) {
            dissolve(world, members);
            return;
        }
        for (TileEntityVault te : tes) {
            te.setMultiblock(controller, width, length);
        }
    }

    private static void dissolve(World world, Set<BlockPos> members) {
        for (BlockPos p : members) {
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileEntityVault) {
                ((TileEntityVault) te).setMultiblock(null, 1, 1);
            }
        }
    }
}
