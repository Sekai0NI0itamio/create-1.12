package nl.melonstudios.create.block.train;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.tileentity.train.TileEntityTrainSignal;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Track signal (foundation): shows red/green from whether the next track
 * section is occupied by a train entity, with yellow for a train approaching
 * further out. Mirrors the reference signal states (red/yellow/green driven
 * by block occupancy) without its graph edge points: occupancy here is a
 * cheap proximity scan around the signal, which the tile entity refreshes on
 * its lazy tick.
 *
 * Red signals emit full redstone power so existing redstone can interlock.
 */
@SuppressWarnings("deprecation")
public class BlockTrainSignal extends Block implements ITileEntityProvider {
    public enum SignalAspect implements IStringSerializable {
        RED,
        YELLOW,
        GREEN;

        private final String name = this.toString().toLowerCase(Locale.ENGLISH);

        @Override
        public String getName() {
            return this.name;
        }
    }

    public static final PropertyEnum<SignalAspect> SIGNAL = PropertyEnum.create("signal", SignalAspect.class);

    public BlockTrainSignal() {
        super(Material.IRON, MapColor.IRON);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(2.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 1);
        this.setRegistryName("train_signal");
        this.setUnlocalizedName("create.train_signal");
        this.setDefaultState(this.blockState.getBaseState().withProperty(SIGNAL, SignalAspect.GREEN));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, SIGNAL);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(SIGNAL).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        SignalAspect[] values = SignalAspect.values();
        return this.getDefaultState().withProperty(SIGNAL, values[meta < 0 || meta >= values.length ? 2 : meta]);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityTrainSignal();
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.util.EnumFacing side) {
        return state.getValue(SIGNAL) == SignalAspect.RED ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.util.EnumFacing side) {
        return state.getValue(SIGNAL) == SignalAspect.RED ? 15 : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 0.75D);
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
