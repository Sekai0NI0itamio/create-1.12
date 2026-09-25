package nl.melonstudios.create.block;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.state.EnumBeltPart;
import nl.melonstudios.create.tileentity.TileEntityChainConveyor;

import javax.annotation.Nullable;

/**
 * Chain conveyor (vertical item lift, core loop only).
 *
 * Translated from the reference chain-conveyor system
 * (ChainConveyorBlock / ChainConveyorBlockEntity /
 * ChainConveyorConnectionHandler): two endpoint blocks joined by a chain
 * line carry items and entities between them at RPM-scaled speed.
 *
 * Backport simplifications (scope control):
 * - Vertical columns only. The reference forbids pure-vertical links
 *   (horizontal run required, slope <= 45 degrees); here the column IS the
 *   lift, mirroring the belt-vertical and elevator-pulley patterns.
 * - Lines form automatically: a lone endpoint scans up/down for a partner
 *   (3..32 blocks apart, clear air between) and fills the gap with MIDDLE
 *   chain-segment blocks. No chain-item linking step (1.12 has no chain
 *   item) and no multi-connection routing table.
 * - No rideable-player hanging and no package/frogport variants (future).
 */
@SuppressWarnings("deprecation")
public class BlockChainConveyor extends BlockKineticBase implements ITileEntityProvider {
    public static final PropertyEnum<EnumBeltPart> PART = PropertyEnum.create("part", EnumBeltPart.class);

    /** Reference CKinetics.maxChainConveyorLength default. */
    public static final int MAX_LENGTH = 32;
    /** Reference minimum link distance (2.5): endpoints at least 3 apart. */
    public static final int MIN_LENGTH = 3;

    private static final AxisAlignedBB SEGMENT_AABB =
            new AxisAlignedBB(5.0 / 16.0, 0.0, 5.0 / 16.0, 11.0 / 16.0, 1.0, 11.0 / 16.0);

    public BlockChainConveyor() {
        super(Material.ROCK, MapColor.STONE);
        this.setRegistryName("chain_conveyor");
        this.setUnlocalizedName("create.chain_conveyor");
        this.blockSoundType = SoundType.STONE;
        this.setHardness(2.5F);
        this.setResistance(6.0F);
        this.setDefaultState(this.blockState.getBaseState().withProperty(PART, EnumBeltPart.START));
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityChainConveyor();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, PART);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(PART, EnumBeltPart.START);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(PART).getId() & 3;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumBeltPart part = EnumBeltPart.byId(meta);
        if (part == EnumBeltPart.PULLEY) part = EnumBeltPart.START;
        return this.getDefaultState().withProperty(PART, part);
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return EnumFacing.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        // Endpoints take power from above, like the elevator/rope pulleys.
        return state.getValue(PART) != EnumBeltPart.MIDDLE && side == EnumFacing.UP;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        if (!world.isRemote && state.getValue(PART) != EnumBeltPart.MIDDLE) {
            tryFormLine(world, pos);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block blockIn,
                                BlockPos fromPos) {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        if (world.isRemote) return;
        if (state.getValue(PART) != EnumBeltPart.MIDDLE) {
            tryFormLine(world, pos);
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityChainConveyor) {
                ((TileEntityChainConveyor) te).requestLineRefresh();
            }
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            dissolveLine(world, pos, state);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        if (state.getValue(PART) == EnumBeltPart.MIDDLE) return false;
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty()) return false;
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityChainConveyor)) return false;
        TileEntityChainConveyor lift = (TileEntityChainConveyor) te;
        ItemStack single = held.copy();
        single.setCount(1);
        ItemStack remainder = lift.tryInsert(single);
        if (remainder.isEmpty()) {
            held.shrink(1);
            return true;
        }
        return false;
    }

    /**
     * Scan up (then down) for a lone endpoint partner and fill the column
     * between with MIDDLE segments. Idempotent: re-running on an intact
     * line changes nothing.
     */
    public static void tryFormLine(World world, BlockPos pos) {
        if (!(world.getBlockState(pos).getBlock() instanceof BlockChainConveyor)) return;
        BlockPos top = findPartner(world, pos, 1);
        if (top != null) {
            buildLine(world, pos, top);
            return;
        }
        BlockPos bottom = findPartner(world, pos, -1);
        if (bottom != null) {
            buildLine(world, bottom, pos);
        }
    }

    @Nullable
    private static BlockPos findPartner(World world, BlockPos pos, int dir) {
        for (int i = MIN_LENGTH; i <= MAX_LENGTH; i++) {
            BlockPos p = pos.up(dir * i);
            if (!world.isBlockLoaded(p)) return null;
            IBlockState st = world.getBlockState(p);
            if (st.getBlock() instanceof BlockChainConveyor) {
                // Partner must be a lone endpoint, not a segment of another line.
                if (st.getValue(PART) == EnumBeltPart.MIDDLE) return null;
                // The whole run between must be air (or our own segments).
                for (int j = 1; j < i; j++) {
                    BlockPos mid = pos.up(dir * j);
                    IBlockState mst = world.getBlockState(mid);
                    if (mst.getBlock() instanceof BlockChainConveyor) {
                        if (mst.getValue(PART) != EnumBeltPart.MIDDLE) return null;
                    } else if (!world.isAirBlock(mid)) {
                        return null;
                    }
                }
                return p;
            } else if (!world.isAirBlock(p)) {
                return null;
            }
        }
        return null;
    }

    private static void buildLine(World world, BlockPos bottom, BlockPos top) {
        world.setBlockState(bottom, world.getBlockState(bottom).withProperty(PART, EnumBeltPart.START), 3);
        world.setBlockState(top, world.getBlockState(top).withProperty(PART, EnumBeltPart.END), 3);
        for (int y = bottom.getY() + 1; y < top.getY(); y++) {
            BlockPos p = new BlockPos(bottom.getX(), y, bottom.getZ());
            IBlockState st = world.getBlockState(p);
            if (world.isAirBlock(p)
                    || (st.getBlock() instanceof BlockChainConveyor
                    && st.getValue(PART) == EnumBeltPart.MIDDLE)) {
                world.setBlockState(p, world.getBlockState(bottom).getBlock()
                        .getDefaultState().withProperty(PART, EnumBeltPart.MIDDLE), 3);
            }
        }
        TileEntity te = world.getTileEntity(bottom);
        if (te instanceof TileEntityChainConveyor) ((TileEntityChainConveyor) te).requestLineRefresh();
        TileEntity te2 = world.getTileEntity(top);
        if (te2 instanceof TileEntityChainConveyor) ((TileEntityChainConveyor) te2).requestLineRefresh();
    }

    /**
     * Breaking any block of a line removes its MIDDLE segments and resets
     * surviving endpoints to lone START blocks.
     */
    public static void dissolveLine(World world, BlockPos pos, IBlockState state) {
        EnumBeltPart part = state.getValue(PART);
        // Clear segments above and below, stopping at the partner endpoints.
        for (int dir : new int[]{1, -1}) {
            for (int i = 1; i <= MAX_LENGTH + 1; i++) {
                BlockPos p = pos.up(dir * i);
                if (!world.isBlockLoaded(p)) break;
                IBlockState st = world.getBlockState(p);
                if (!(st.getBlock() instanceof BlockChainConveyor)) break;
                EnumBeltPart q = st.getValue(PART);
                if (q == EnumBeltPart.MIDDLE) {
                    world.setBlockToAir(p);
                } else {
                    // Surviving partner goes back to a lone endpoint.
                    if (!p.equals(pos)) {
                        world.setBlockState(p, st.withProperty(PART, EnumBeltPart.START), 3);
                        TileEntity te = world.getTileEntity(p);
                        if (te instanceof TileEntityChainConveyor) {
                            ((TileEntityChainConveyor) te).requestLineRefresh();
                        }
                    }
                    break;
                }
            }
        }
        if (part == EnumBeltPart.MIDDLE) {
            world.setBlockToAir(pos);
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(PART) == EnumBeltPart.MIDDLE ? SEGMENT_AABB : FULL_BLOCK_AABB;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        // Segments are pass-through so cargo and riders travel the column.
        return state.getValue(PART) == EnumBeltPart.MIDDLE ? NULL_AABB : FULL_BLOCK_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return state.getValue(PART) != EnumBeltPart.MIDDLE;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return state.getValue(PART) != EnumBeltPart.MIDDLE;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }

    @Override
    public int quantityDropped(IBlockState state, int fortune, java.util.Random random) {
        // Segments are free chain links: only endpoints drop the block.
        return state.getValue(PART) == EnumBeltPart.MIDDLE ? 0 : 1;
    }
}
