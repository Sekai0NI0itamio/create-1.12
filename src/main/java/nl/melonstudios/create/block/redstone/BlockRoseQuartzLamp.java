package nl.melonstudios.create.block.redstone;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Rose quartz lamp: latches onto an incoming signal and rebroadcasts it to
 * every connected lamp in its cluster (16-block Manhattan flood fill),
 * translated from the reference cluster logic. Fully static, no tile entity.
 * Comparators read the distance to the nearest powering lamp along the column.
 */
@SuppressWarnings("deprecation")
public class BlockRoseQuartzLamp extends Block implements IWrenchable {
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyBool POWERING = PropertyBool.create("powering");
    public static final PropertyBool ACTIVATE = PropertyBool.create("activate");

    public BlockRoseQuartzLamp() {
        super(Material.REDSTONE_LIGHT, MapColor.QUARTZ);
        this.blockSoundType = SoundType.GLASS;
        this.setHardness(1.5F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("rose_quartz_lamp");
        this.setUnlocalizedName("create.rose_quartz_lamp");
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(POWERED, false)
                .withProperty(POWERING, false)
                .withProperty(ACTIVATE, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWERED, POWERING, ACTIVATE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(POWERED, (meta & 1) != 0)
                .withProperty(POWERING, (meta & 2) != 0)
                .withProperty(ACTIVATE, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(POWERED) ? 1 : 0)
                | (state.getValue(POWERING) ? 2 : 0)
                | (state.getValue(ACTIVATE) ? 4 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(POWERED, world.isBlockPowered(pos));
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (world.isRemote) return;
        boolean lit = state.getValue(POWERED);
        if (lit == world.isBlockPowered(pos)) return;
        if (lit) {
            world.setBlockState(pos, state.withProperty(POWERED, false), 2);
            return;
        }
        this.forEachInCluster(world, pos, (lampPos, lampState) -> {
            world.setBlockState(lampPos, lampState.withProperty(POWERING, false), 2);
            this.scheduleActivation(world, lampPos);
        });
        world.setBlockState(pos, state.withProperty(POWERED, true)
                .withProperty(POWERING, true)
                .withProperty(ACTIVATE, true), 2);
        world.notifyNeighborsOfStateChange(pos, this, false);
        this.scheduleActivation(world, pos);
    }

    private void scheduleActivation(World world, BlockPos pos) {
        if (!world.isBlockTickPending(pos, this)) {
            world.scheduleUpdate(pos, this, 1);
        }
    }

    private void forEachInCluster(World world, BlockPos origin, BiConsumer<BlockPos, IBlockState> action) {
        Queue<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(origin);
        visited.add(origin);
        while (!frontier.isEmpty()) {
            BlockPos current = frontier.remove();
            for (EnumFacing side : EnumFacing.VALUES) {
                BlockPos next = current.offset(side);
                if (Math.abs(next.getX() - origin.getX()) + Math.abs(next.getY() - origin.getY()) + Math.abs(next.getZ() - origin.getZ()) > 16) continue;
                if (!visited.add(next)) continue;
                IBlockState nextState = world.getBlockState(next);
                if (nextState.getBlock() != this) continue;
                action.accept(next, nextState);
                frontier.add(next);
            }
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        boolean wasPowering = state.getValue(POWERING);
        boolean shouldPower = state.getValue(ACTIVATE);
        if (wasPowering || shouldPower) {
            world.setBlockState(pos, state.withProperty(ACTIVATE, false)
                    .withProperty(POWERING, shouldPower), 2);
        }
        world.notifyNeighborsOfStateChange(pos, this, false);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        if (side == null) return 0;
        IBlockState target = access.getBlockState(pos.offset(side.getOpposite()));
        if (target.getBlock() == this) return 0;
        if (target.getBlock() == Blocks.UNPOWERED_COMPARATOR
                || target.getBlock() == Blocks.POWERED_COMPARATOR) {
            return this.distanceToPowering(access, pos, side);
        }
        return state.getValue(POWERING) ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        return this.getWeakPower(state, access, pos, side);
    }

    private int distanceToPowering(IBlockAccess access, BlockPos pos, EnumFacing column) {
        BlockPos cursor = new BlockPos(pos);
        for (int power = 15; power > 0; power--) {
            IBlockState state = access.getBlockState(cursor);
            if (state.getBlock() != this) return 0;
            if (state.getValue(POWERING)) return power;
            cursor = cursor.offset(column);
        }
        return 0;
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos,
                                      @Nullable EnumFacing side) {
        return true;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(POWERING) ? 15 : 0;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                   EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return false;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side,
                             float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        world.setBlockState(pos, state.withProperty(POWERING, !state.getValue(POWERING)), 2);
        this.forEachInCluster(world, pos, (lampPos, lampState) ->
                world.notifyNeighborsOfStateChange(lampPos, this, false));
        return true;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
