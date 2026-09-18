package nl.melonstudios.create.block.actor;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import nl.melonstudios.create.block.state.EnumBeltPart;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.tileentity.actor.TileEntityBeltBase;
import nl.melonstudios.create.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Belt end interactions, translated from the reference mod's
 * BeltSlicer / BeltConnectorItem / BeltBlock trio
 * (content/kinetics/belt/). Logic rewritten for 1.12.2 Forge;
 * no reference code is pasted verbatim.
 *
 * Reference rules ported here:
 * - BeltConnectorItem.maxLength() = CKinetics.maxBeltLength, default 20.
 * - BeltSlicer.useConnector: connector on a belt START/END extends the belt
 *   by one segment when the target spot is free, or merges two touching
 *   belts when the target spot holds a compatible belt (same slope/axis).
 * - BeltSlicer.hoveringEnd: MIDDLE/PULLEY never react; the click must land
 *   on the outer half of the end segment.
 * - BeltBlock.use isShaft branch: shaft item on a MIDDLE segment makes it
 *   a PULLEY. BeltBlock.onWrenched PULLEY branch: wrench reverts PULLEY
 *   to MIDDLE and refunds the shaft.
 */
public final class BeltSlicer {
    private BeltSlicer() {
    }

    /** Reference default: CKinetics.maxBeltLength = 20 (config range 5+). */
    public static final int MAX_BELT_LENGTH = 20;

    public static boolean isStraightBelt(IBlockState state) {
        return state.getBlock() instanceof BlockBeltStraight;
    }

    /** Direction from START towards END for a straight belt. */
    public static EnumFacing beltVector(IBlockState state) {
        if (state.getValue(BlockBeltStraight.VERTICAL)) return EnumFacing.UP;
        return state.getValue(BlockBeltStraight.AXIS) == EnumFacing.Axis.X ? EnumFacing.EAST : EnumFacing.SOUTH;
    }

    /** Reference beltStatesCompatible, reduced to what the backport models (axis + vertical flag). */
    public static boolean sameLine(IBlockState a, IBlockState b) {
        if (!isStraightBelt(a) || !isStraightBelt(b)) return false;
        return a.getValue(BlockBeltStraight.AXIS) == b.getValue(BlockBeltStraight.AXIS)
                && a.getValue(BlockBeltStraight.VERTICAL) == b.getValue(BlockBeltStraight.VERTICAL);
    }

    /**
     * Reference BeltSlicer.hoveringEnd: only START/END react, and the click
     * must be on the outer half of the segment along the belt direction.
     */
    public static boolean hoveringEnd(IBlockState state, float hitX, float hitY, float hitZ) {
        if (!isStraightBelt(state)) return false;
        EnumBeltPart part = state.getValue(BlockBeltBase.PART);
        if (part != EnumBeltPart.START && part != EnumBeltPart.END) return false;
        EnumFacing vec = beltVector(state);
        Vec3d center = new Vec3d(0.5, 0.5, 0.5);
        Vec3d hit = new Vec3d(hitX, hitY, hitZ);
        double dot = hit.subtract(center).dotProduct(new Vec3d(vec.getDirectionVec()));
        return (dot > 0.0) == (part == EnumBeltPart.END);
    }

    /** Ordered chain of contiguous same-line belt segments containing pos. */
    public static List<BlockPos> getChain(World world, BlockPos pos) {
        List<BlockPos> chain = new ArrayList<>();
        IBlockState state = world.getBlockState(pos);
        if (!isStraightBelt(state)) return chain;
        EnumFacing vec = beltVector(state);
        BlockPos start = pos;
        for (int i = 0; i < 64; i++) {
            BlockPos prev = start.offset(vec.getOpposite());
            if (!world.isBlockLoaded(prev)) break;
            if (!sameLine(state, world.getBlockState(prev))) break;
            start = prev;
        }
        BlockPos cur = start;
        for (int i = 0; i < 64; i++) {
            if (!world.isBlockLoaded(cur)) break;
            if (!sameLine(state, world.getBlockState(cur))) break;
            chain.add(cur.toImmutable());
            cur = cur.offset(vec);
        }
        return chain;
    }

    private static void status(EntityPlayer player, String key, Object... args) {
        player.sendStatusMessage(new TextComponentTranslation(key, args), true);
    }

    /**
     * Reference BeltSlicer.useConnector: extend at a free end, or merge two
     * touching compatible belts. Returns true when the click was handled
     * (even when the extension itself was refused with a message).
     * Never touches the connector's stored first-pulley NBT.
     */
    public static boolean useConnector(World world, BlockPos pos, IBlockState state,
                                       EntityPlayer player, EnumHand hand,
                                       float hitX, float hitY, float hitZ) {
        if (!hoveringEnd(state, hitX, hitY, hitZ)) return false;
        EnumBeltPart part = state.getValue(BlockBeltBase.PART);
        EnumFacing vec = beltVector(state);
        BlockPos next = part == EnumBeltPart.END ? pos.offset(vec) : pos.offset(vec.getOpposite());
        if (!world.isBlockLoaded(next)) return false;

        // getChain walks through contiguous belts, so when two belts already
        // touch, chain spans the union and chain.size() is the merged length.
        List<BlockPos> chain = getChain(world, pos);
        IBlockState nextState = world.getBlockState(next);

        if (isStraightBelt(nextState) && sameLine(state, nextState)) {
            if (chain.size() > MAX_BELT_LENGTH) {
                if (!world.isRemote) status(player, "message.create.belt_too_long", MAX_BELT_LENGTH);
                return true;
            }
            if (!world.isRemote) {
                relabel(chain, world);
                TileEntity te = world.getTileEntity(pos);
                if (te instanceof TileEntityBeltBase) {
                    TileEntityBeltBase belt = (TileEntityBeltBase) te;
                    belt.applyColor(belt.color);
                }
                world.playSound(null, pos, SoundEvents.BLOCK_CLOTH_HIT, SoundCategory.BLOCKS,
                        0.5F, 1.3F);
            }
            return true;
        }

        if (!nextState.getBlock().isReplaceable(world, next)) {
            if (!world.isRemote) status(player, "message.create.belt_no_space");
            return true;
        }
        if (chain.size() + 1 > MAX_BELT_LENGTH) {
            if (!world.isRemote) status(player, "message.create.belt_max", MAX_BELT_LENGTH);
            return true;
        }
        if (!world.isRemote) {
            EnumDyeColor color = null;
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityBeltBase) color = ((TileEntityBeltBase) te).color;

            Utils.setBlockKineticTESafe(world, pos,
                    state.withProperty(BlockBeltBase.PART, EnumBeltPart.MIDDLE), 3);

            IBlockState placed = BlockInit.BELT_STRAIGHT.getDefaultState()
                    .withProperty(BlockBeltStraight.AXIS, state.getValue(BlockBeltStraight.AXIS))
                    .withProperty(BlockBeltStraight.VERTICAL, state.getValue(BlockBeltStraight.VERTICAL))
                    .withProperty(BlockBeltBase.PART, part);
            world.setBlockState(next, placed, 3);

            TileEntity te2 = world.getTileEntity(next);
            if (te2 instanceof TileEntityBeltBase) {
                ((TileEntityBeltBase) te2).color = color;
                ((TileEntityBeltBase) te2).sync();
            }
            world.playSound(null, pos, SoundEvents.BLOCK_CLOTH_PLACE, SoundCategory.BLOCKS,
                    1.0F, 0.9F + world.rand.nextFloat() * 0.2F);
        }
        return true;
    }

    private static void relabel(List<BlockPos> chain, World world) {
        for (int i = 0; i < chain.size(); i++) {
            BlockPos p = chain.get(i);
            IBlockState s = world.getBlockState(p);
            if (!isStraightBelt(s)) continue;
            EnumBeltPart want;
            if (i == 0) want = EnumBeltPart.START;
            else if (i == chain.size() - 1) want = EnumBeltPart.END;
            else want = s.getValue(BlockBeltBase.PART) == EnumBeltPart.PULLEY
                    ? EnumBeltPart.PULLEY : EnumBeltPart.MIDDLE;
            if (s.getValue(BlockBeltBase.PART) != want) {
                Utils.setBlockKineticTESafe(world, p, s.withProperty(BlockBeltBase.PART, want), 3);
            }
        }
    }

    /**
     * Reference BeltBlock.use isShaft branch: shaft item on a MIDDLE
     * segment turns it into a PULLEY (shaft consumed unless creative).
     */
    public static boolean useShaft(World world, BlockPos pos, IBlockState state,
                                   EntityPlayer player, EnumHand hand) {
        if (!isStraightBelt(state)) return false;
        if (state.getValue(BlockBeltBase.PART) != EnumBeltPart.MIDDLE) return false;
        if (!world.isRemote) {
            Utils.setBlockKineticTESafe(world, pos,
                    state.withProperty(BlockBeltBase.PART, EnumBeltPart.PULLEY), 3);
            if (!player.isCreative()) player.getHeldItem(hand).shrink(1);
        }
        return true;
    }

    /**
     * Reference BeltBlock.onWrenched PULLEY branch: wrenching a pulley
     * (from top/bottom) reverts it to MIDDLE and refunds the shaft.
     */
    public static boolean useWrench(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        if (!isStraightBelt(state)) return false;
        if (state.getValue(BlockBeltBase.PART) != EnumBeltPart.PULLEY) return false;
        if (side.getAxis() != EnumFacing.Axis.Y) return false;
        if (!world.isRemote) {
            Utils.setBlockKineticTESafe(world, pos,
                    state.withProperty(BlockBeltBase.PART, EnumBeltPart.MIDDLE), 3);
            Block.spawnAsEntity(world, pos, new ItemStack(BlockInit.SHAFT));
        }
        return true;
    }
}
