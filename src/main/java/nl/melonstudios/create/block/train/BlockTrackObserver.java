package nl.melonstudios.create.block.train;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.train.TileEntityTrackObserver;

import javax.annotation.Nullable;

/**
 * Track observer: redstone source that emits 15 on every side while a train
 * (EntityTrain) or minecart passes within range above it. The TE holds the
 * pulse for 8 ticks after the last sighting (reference: powered output while
 * observing). Wrench does nothing but is accepted for consistency.
 */
@SuppressWarnings("deprecation")
public class BlockTrackObserver extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockTrackObserver() {
        super(Material.IRON, MapColor.BROWN);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(2.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(ItemInit.TAB_CREATE);
        this.setRegistryName("track_observer");
        this.setUnlocalizedName("create.track_observer");
        this.setDefaultState(this.blockState.getBaseState().withProperty(POWERED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWERED);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(POWERED) ? 1 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(POWERED, (meta & 1) != 0);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityTrackObserver();
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY,
                             float hitZ) {
        return true;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.8125, 1.0);
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
