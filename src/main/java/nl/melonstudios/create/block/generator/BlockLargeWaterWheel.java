package nl.melonstudios.create.block.generator;

import nl.melonstudios.create.init.CreateTabs;
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
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.generator.TileEntityLargeWaterWheel;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Large water wheel (translated from the reference LargeWaterWheelBlock, MIT).
 * A slower, bigger wheel: the shared flow-scoring checks the 8 rim offsets
 * two blocks out (LARGE_OFFSETS) and the generated speed halves to
 * clamp(flow, -1, 1) * 8 / 2 = 4 RPM max (see TileEntityLargeWaterWheel).
 * Needs a clear 3x3 plane perpendicular to its axis; obstructed placements
 * pop with a drop, mirroring the reference survival check. Structural rim
 * blocks from the reference have no backport equivalent and are skipped.
 */
@SuppressWarnings("deprecation")
public class BlockLargeWaterWheel extends BlockKineticDirectionalBase implements ITileEntityProvider {
    public BlockLargeWaterWheel(MapColor color, SoundType soundType) {
        super(Material.ROCK, color);
        this.blockSoundType = soundType;

        this.setRegistryName("large_water_wheel");
        this.setUnlocalizedName("create.large_water_wheel");

        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);

        this.setHarvestLevel("axe", 0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        worldIn.scheduleUpdate(pos, this, 1);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        EnumFacing.Axis axis = state.getValue(FACING).getAxis();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos offset = planeOffset(axis, a, b);
                if (!worldIn.getBlockState(pos.add(offset)).getBlock().isReplaceable(worldIn, pos.add(offset))) {
                    worldIn.destroyBlock(pos, true);
                    return;
                }
            }
        }
        worldIn.scheduleUpdate(pos, this, 1);
    }

    private static BlockPos planeOffset(EnumFacing.Axis axis, int a, int b) {
        switch (axis) {
            case X: return new BlockPos(0, a, b);
            case Z: return new BlockPos(a, b, 0);
            case Y:
            default: return new BlockPos(a, 0, b);
        }
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityLargeWaterWheel) {
            ((TileEntityLargeWaterWheel) te).determineAndApplyFlowSource();
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
        return new TileEntityLargeWaterWheel();
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
        return "axe".equals(type);
    }
}
