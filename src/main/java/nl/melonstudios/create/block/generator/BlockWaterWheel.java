package nl.melonstudios.create.block.generator;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticDirectionalBase;
import nl.melonstudios.create.tileentity.generator.TileEntityWaterWheel;

import javax.annotation.Nullable;
import java.util.Random;

@SuppressWarnings("deprecation")
public class BlockWaterWheel extends BlockKineticDirectionalBase implements ITileEntityProvider {
    public BlockWaterWheel(MapColor color, SoundType soundType) {
        super(Material.ROCK, color);
        this.blockSoundType = soundType;

        this.setRegistryName("water_wheel");
        this.setUnlocalizedName("create.water_wheel");
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        worldIn.scheduleUpdate(pos, this, 1);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        worldIn.scheduleUpdate(pos, this, 1);
        // Reference WaterWheelBlock.canSurvive: wheels may only touch along a
        // shared axis with matching axes. Pop invalid placements.
        EnumFacing.Axis axis = state.getValue(FACING).getAxis();
        for (EnumFacing side : EnumFacing.VALUES) {
            IBlockState neighbour = worldIn.getBlockState(pos.offset(side));
            if (!(neighbour.getBlock() instanceof BlockWaterWheel)) {
                continue;
            }
            if (neighbour.getValue(FACING).getAxis() != axis || axis != side.getAxis()) {
                worldIn.destroyBlock(pos, true);
                return;
            }
        }
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityWaterWheel) {
            TileEntityWaterWheel waterWheel = (TileEntityWaterWheel) te;
            waterWheel.determineAndApplyFlowSource();
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand);
        return state.withProperty(FACING, EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, state.getValue(FACING).getAxis()));
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityWaterWheel();
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return state.getValue(FACING).getAxis() == side.getAxis();
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || "axe".equals(type);
    }
}
