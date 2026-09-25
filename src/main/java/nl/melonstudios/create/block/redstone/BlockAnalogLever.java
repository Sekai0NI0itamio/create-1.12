package nl.melonstudios.create.block.redstone;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.redstone.TileEntityAnalogLever;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Analog lever: click steps the output through 0-15, sneak-click steps down.
 * The level lives in the tile entity (4-bit meta cannot hold it); the block
 * only stores its mounting face. Output applies after a short delay,
 * translated from the reference behaviour.
 */
@SuppressWarnings("deprecation")
public class BlockAnalogLever extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.3125, 0.0, 0.25, 0.6875, 0.1875, 0.75);

    public BlockAnalogLever() {
        super(Material.CIRCUITS, MapColor.WOOD);
        this.blockSoundType = SoundType.WOOD;
        this.setHardness(0.5F);
        this.setResistance(2.0F);
        this.setCreativeTab(ItemInit.TAB_CREATE);
        this.setRegistryName("analog_lever");
        this.setUnlocalizedName("create.analog_lever");
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getFront(meta & 7));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, facing);
    }

    private boolean canStay(World world, BlockPos pos, EnumFacing facing) {
        BlockPos support = pos.offset(facing.getOpposite());
        return world.getBlockState(support).getBlockFaceShape(world, support, facing) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return this.canStay(world, pos, side);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!this.canStay(world, pos, state.getValue(FACING))) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                   EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntityAnalogLever lever = Utils.cast(world.getTileEntity(pos), TileEntityAnalogLever.class);
        if (lever == null) return false;
        lever.changeState(player.isSneaking());
        float pitch = 0.25F + ((lever.getLevel() + 5) / 15.0F) * 0.5F;
        world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, pitch);
        return true;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side,
                             float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        EnumFacing next = EnumFacing.getFront((state.getValue(FACING).getIndex() + 1) % 6);
        if (this.canStay(world, pos, next)) {
            world.setBlockState(pos, state.withProperty(FACING, next), 3);
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityAnalogLever();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntityAnalogLever lever = Utils.cast(world.getTileEntity(pos), TileEntityAnalogLever.class);
        int level = lever != null ? lever.getLevel() : 0;
        world.removeTileEntity(pos);
        if (level != 0) {
            this.notifyNeighbors(world, pos, state);
        }
    }

    public void notifyNeighbors(World world, BlockPos pos, IBlockState state) {
        world.notifyNeighborsOfStateChange(pos, this, false);
        world.notifyNeighborsOfStateChange(pos.offset(state.getValue(FACING).getOpposite()), this, false);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        TileEntityAnalogLever lever = Utils.cast(access.getTileEntity(pos), TileEntityAnalogLever.class);
        return lever != null ? lever.getLevel() : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        return state.getValue(FACING) == side ? this.getWeakPower(state, access, pos, side) : 0;
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos,
                                      @Nullable EnumFacing side) {
        return true;
    }

    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        TileEntityAnalogLever lever = Utils.cast(world.getTileEntity(pos), TileEntityAnalogLever.class);
        if (lever != null && lever.getLevel() != 0 && rand.nextFloat() < 0.25F) {
            EnumFacing facing = state.getValue(FACING);
            double x = pos.getX() + 0.5 + facing.getFrontOffsetX() * 0.15;
            double y = pos.getY() + 0.5 + facing.getFrontOffsetY() * 0.15;
            double z = pos.getZ() + 0.5 + facing.getFrontOffsetZ() * 0.15;
            world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "axe".equals(type);
    }
}
