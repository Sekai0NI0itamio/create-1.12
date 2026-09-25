package nl.melonstudios.create.block.logistics;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.BlockStateProperties;
import com.melonstudios.melonlib.misc.StackUtil;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.actor.BlockBeltBase;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.tileentity.logistics.TileEntityBrassTunnel;
import nl.melonstudios.create.util.filter.IItemFilter;
import nl.melonstudios.create.util.filter.ItemFilterExact;
import nl.melonstudios.create.util.interfaces.IGoggleInfo;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Brass tunnel: a belt-cover splitter/sorter placed on top of a belt segment.
 * The row axis follows the belt below at placement time. Right-click with an
 * item sets an exact filter on the clicked side, sneak right-click with an
 * empty hand clears it, empty-hand use takes back held stacks, and the wrench
 * cycles the selection mode. Breaks off when the belt below is removed.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class BlockBrassTunnel extends Block implements ITileEntityProvider, IWrenchable, IGoggleInfo {
    public static final PropertyEnum<EnumFacing.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public static final AxisAlignedBB BOX = AABB.create(0, 0, 0, 16, 12, 16);

    public BlockBrassTunnel() {
        super(Material.IRON, MapColor.gold);
        this.blockSoundType = SoundType.METAL;

        this.setHardness(3.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 1);

        this.setCreativeTab(ItemInit.TAB_CREATE);
        this.setDefaultState(this.getDefaultState().withProperty(AXIS, EnumFacing.Axis.X));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityBrassTunnel();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AXIS) == EnumFacing.Axis.Z ? 1 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(AXIS, (meta & 1) != 0 ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                           float hitX, float hitY, float hitZ, int meta,
                                           EntityLivingBase placer, EnumHand hand) {
        IBlockState below = world.getBlockState(pos.down());
        if (below.getBlock() instanceof BlockBeltBase) {
            return this.getDefaultState().withProperty(AXIS,
                    ((BlockBeltBase) below.getBlock()).getTransportAxis(below));
        }
        return this.getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return super.canPlaceBlockAt(worldIn, pos)
                && worldIn.getBlockState(pos.down()).getBlock() instanceof BlockBeltBase;
    }

    private static void refresh(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityBrassTunnel) ((TileEntityBrassTunnel) te).refreshConnections();
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        refresh(worldIn, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!(worldIn.getBlockState(pos.down()).getBlock() instanceof BlockBeltBase)) {
            this.dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
            return;
        }
        refresh(worldIn, pos);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOptimizedBase) ((TileEntityOptimizedBase) te).destroy();
        if (this.hasTileEntity(state)) worldIn.removeTileEntity(pos);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                   EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityBrassTunnel)) return false;
        TileEntityBrassTunnel tunnel = (TileEntityBrassTunnel) te;
        ItemStack heldItem = player.getHeldItem(hand);
        if (heldItem.isEmpty() && !player.isSneaking()) {
            List<ItemStack> taken = tunnel.grabRow(false);
            if (taken.isEmpty()) return false;
            boolean moved = false;
            for (ItemStack stack : taken) {
                if (!player.addItemStackToInventory(stack.copy())) {
                    StackUtil.spawnItemWithVelocity(world, player.posX, player.posY + 0.5, player.posZ,
                            stack.copy(), 0.0, 0.2, 0.0);
                }
                moved = true;
            }
            if (moved) {
                world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS,
                        0.2F, 1.0F + world.rand.nextFloat());
            }
            return true;
        }
        if (facing.getAxis() != EnumFacing.Axis.Y && hand == EnumHand.MAIN_HAND) {
            if (heldItem.isEmpty() && player.isSneaking()) {
                tunnel.setFilter(facing, null);
                player.sendStatusMessage(new TextComponentString(
                        "Brass tunnel filter on " + facing.getName() + " cleared."), true);
                return true;
            }
            if (!heldItem.isEmpty()) {
                IItemFilter filter = new ItemFilterExact(heldItem);
                tunnel.setFilter(facing, filter);
                player.sendStatusMessage(new TextComponentString(
                        "Brass tunnel filter on " + facing.getName() + " set to "
                                + heldItem.getDisplayName() + "."), true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side,
                             float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityBrassTunnel)) return false;
        if (!world.isRemote) {
            TileEntityBrassTunnel tunnel = (TileEntityBrassTunnel) te;
            tunnel.cycleMode();
            if (world.getBlockState(pos).getBlock() == this) {
                for (TileEntityBrassTunnel member : tunnelRow(world, pos, state.getValue(AXIS))) {
                    if (member != tunnel && member.mode != tunnel.mode) {
                        member.mode = tunnel.mode;
                        member.markDirty();
                        member.sync();
                    }
                }
            }
        }
        return true;
    }

    private static List<TileEntityBrassTunnel> tunnelRow(World world, BlockPos pos, EnumFacing.Axis axis) {
        List<TileEntityBrassTunnel> members = new ArrayList<>();
        TileEntity center = world.getTileEntity(pos);
        if (center instanceof TileEntityBrassTunnel) members.add((TileEntityBrassTunnel) center);
        EnumFacing[] dirs = axis == EnumFacing.Axis.X
                ? new EnumFacing[]{EnumFacing.WEST, EnumFacing.EAST}
                : new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH};
        for (EnumFacing dir : dirs) {
            BlockPos p = pos.offset(dir);
            for (int i = 0; i < 16; i++) {
                TileEntity te = world.getTileEntity(p);
                if (!(te instanceof TileEntityBrassTunnel)) break;
                IBlockState memberState = world.getBlockState(p);
                if (!(memberState.getBlock() instanceof BlockBrassTunnel)) break;
                if (memberState.getValue(AXIS) != axis) break;
                members.add((TileEntityBrassTunnel) te);
                p = p.offset(dir);
            }
        }
        return members;
    }

    @Override
    public List<String> getGoggleInfo(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityBrassTunnel)) return Collections.emptyList();
        TileEntityBrassTunnel tunnel = (TileEntityBrassTunnel) te;
        List<String> lines = new ArrayList<>();
        lines.add("Brass Tunnel");
        lines.add("Mode: " + tunnel.mode.label);
        if (!tunnel.held.isEmpty()) {
            lines.add(tunnel.held.getCount() + "x " + tunnel.held.getDisplayName());
        }
        int filters = tunnel.filterCount();
        if (filters > 0) lines.add(filters + (filters == 1 ? " filtered side" : " filtered sides"));
        return lines;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOX;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityBrassTunnel && !((TileEntityBrassTunnel) te).held.isEmpty()) return 15;
        return 0;
    }

    //region this is not a full block
    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isTranslucent(IBlockState state) {
        return true;
    }

    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
    //endregion
}
