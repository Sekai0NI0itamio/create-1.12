package nl.melonstudios.create.block.generator;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticRotatedPillarBase;
import nl.melonstudios.create.tileentity.generator.TileEntityWaterWheelTemp;

import javax.annotation.Nullable;

public class BlockWaterWheelTemp extends BlockKineticRotatedPillarBase implements ITileEntityProvider {
    public BlockWaterWheelTemp(MapColor color, SoundType soundType) {
        super(Material.ROCK, color);
        this.blockSoundType = soundType;

        this.setRegistryName("water_wheel_temp");
        this.setUnlocalizedName("create.water_wheel_temp");

        this.setCreativeTab(null);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityWaterWheelTemp();
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return state.getValue(AXIS) == side.getAxis();
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityWaterWheelTemp) {
            ((TileEntityWaterWheelTemp)te).determineAndApplyFlowSource();
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityWaterWheelTemp) {
            ((TileEntityWaterWheelTemp)te).determineAndApplyFlowSource();
        }
    }
}
