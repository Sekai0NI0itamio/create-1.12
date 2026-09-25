package nl.melonstudios.create.block.train;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.train.TileEntitySteamWhistle;

import javax.annotation.Nullable;

/**
 * Steam whistle: wall- or floor-mounted horn. Redstone edge triggers a hoot
 * whose pitch falls with SIZE (small/medium/large, wrench-cycled) and with
 * each extension block stacked above (up to 6, reference rule). Extension
 * grows by right-clicking the whistle or stack with a whistle block.
 */
@SuppressWarnings("deprecation")
public class BlockSteamWhistle extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool WALL = PropertyBool.create("wall");
    public static final PropertyEnum<WhistleSize> SIZE = PropertyEnum.create("size", WhistleSize.class);

    public enum WhistleSize implements IStringSerializable {
        SMALL, MEDIUM, LARGE;

        @Override
        public String getName() {
            return this.name().toLowerCase();
        }
    }

    public BlockSteamWhistle() {
        super(Material.IRON, MapColor.GOLD);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(1.5F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(ItemInit.TAB_CREATE);
        this.setRegistryName("steam_whistle");
        this.setUnlocalizedName("create.steam_whistle");
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(WALL, false)
                .withProperty(SIZE, WhistleSize.MEDIUM));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, WALL, SIZE);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        boolean wall = facing.getAxis() != EnumFacing.Axis.Y;
        EnumFacing horn = wall ? facing : placer.getHorizontalFacing().getOpposite();
        return this.getDefaultState().withProperty(FACING, horn).withProperty(WALL, wall);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        // 4-bit meta keeps facing + wall only (2 + 1 bits). SIZE lives in the
        // TE and is overlaid via getActualState, so it survives reloads.
        return state.getValue(FACING).getHorizontalIndex()
                | (state.getValue(WALL) ? 4 : 0);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
                .withProperty(WALL, (meta & 4) != 0);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world instanceof World ? ((World) world).getTileEntity(pos) : null;
        if (te instanceof TileEntitySteamWhistle) {
            return state.withProperty(SIZE, ((TileEntitySteamWhistle) te).getWhistleSize());
        }
        return state;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySteamWhistle();
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (world.isRemote) return;
        boolean powered = world.isBlockPowered(pos);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntitySteamWhistle) {
            ((TileEntitySteamWhistle) te).onRedstone(powered);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                   EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!player.getHeldItem(hand).isEmpty()
                && player.getHeldItem(hand).getItem() == net.minecraft.item.Item.getItemFromBlock(this)) {
            if (!world.isRemote) TileEntitySteamWhistle.grow(world, pos);
            return true;
        }
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntitySteamWhistle) ((TileEntitySteamWhistle) te).hoot();
        }
        return true;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY,
                              float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        WhistleSize current = te instanceof TileEntitySteamWhistle
                ? ((TileEntitySteamWhistle) te).getWhistleSize()
                : state.getValue(SIZE);
        WhistleSize next = current == WhistleSize.SMALL ? WhistleSize.MEDIUM
                : current == WhistleSize.MEDIUM ? WhistleSize.LARGE : WhistleSize.SMALL;
        if (te instanceof TileEntitySteamWhistle) ((TileEntitySteamWhistle) te).setWhistleSize(next);
        world.setBlockState(pos, state.withProperty(SIZE, next), 3);
        TileEntitySteamWhistle.queuePitchUpdate(world, pos);
        return true;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        if (!state.getValue(WALL)) {
            return state.getValue(SIZE) == WhistleSize.SMALL
                    ? new AxisAlignedBB(0.375, 0, 0.375, 0.625, 0.5, 0.625)
                    : state.getValue(SIZE) == WhistleSize.MEDIUM
                    ? new AxisAlignedBB(0.3125, 0, 0.3125, 0.6875, 0.75, 0.6875)
                    : new AxisAlignedBB(0.25, 0, 0.25, 0.75, 1.0, 0.75);
        }
        return new AxisAlignedBB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
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
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
