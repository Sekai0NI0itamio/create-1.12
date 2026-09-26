package nl.melonstudios.create.block.logistics;

import nl.melonstudios.create.init.CreateTabs;
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
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.actor.BlockBeltBase;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.tileentity.logistics.TileEntityAndesiteTunnel;
import nl.melonstudios.create.util.interfaces.IGoggleInfo;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Andesite tunnel: the unfiltered belt-cover distributor.
 * Ports the reference BeltTunnelBlock behaviour: sits on a belt segment, the
 * row axis follows the belt below, pulls items off the moving belt and shares
 * them across the connected row (even split, no filters, no selection modes).
 * Breaks off when the belt below is removed.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class BlockAndesiteTunnel extends Block implements ITileEntityProvider, IGoggleInfo {
    public static final PropertyEnum<EnumFacing.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public static final AxisAlignedBB BOX = AABB.create(0, 0, 0, 16, 12, 16);

    public BlockAndesiteTunnel() {
        super(Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;

        this.setHardness(2.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setDefaultState(this.getDefaultState().withProperty(AXIS, EnumFacing.Axis.X));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityAndesiteTunnel();
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
        if (te instanceof TileEntityAndesiteTunnel) ((TileEntityAndesiteTunnel) te).refreshConnections();
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
        if (!(te instanceof TileEntityAndesiteTunnel)) return false;
        TileEntityAndesiteTunnel tunnel = (TileEntityAndesiteTunnel) te;
        if (!player.getHeldItem(hand).isEmpty()) return false;
        List<ItemStack> taken = tunnel.grabRow(false);
        if (taken.isEmpty()) return false;
        for (ItemStack stack : taken) {
            if (!player.addItemStackToInventory(stack.copy())) {
                StackUtil.spawnItemWithVelocity(world, player.posX, player.posY + 0.5, player.posZ,
                        stack.copy(), 0.0, 0.2, 0.0);
            }
        }
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS,
                0.2F, 1.0F + world.rand.nextFloat());
        return true;
    }

    @Override
    public List<String> getGoggleInfo(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityAndesiteTunnel)) return Collections.emptyList();
        TileEntityAndesiteTunnel tunnel = (TileEntityAndesiteTunnel) te;
        List<String> lines = new ArrayList<>();
        lines.add("Andesite Tunnel");
        if (!tunnel.held.isEmpty()) {
            lines.add(tunnel.held.getCount() + "x " + tunnel.held.getDisplayName());
        }
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
        if (te instanceof TileEntityAndesiteTunnel && !((TileEntityAndesiteTunnel) te).held.isEmpty()) return 15;
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
