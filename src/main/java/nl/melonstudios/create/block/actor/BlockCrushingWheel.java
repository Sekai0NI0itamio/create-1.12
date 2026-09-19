package nl.melonstudios.create.block.actor;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticHorizontalAxisBase;
import nl.melonstudios.create.tileentity.actor.TileEntityCrushingWheel;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;

/**
 * Crushing wheel: kinetic block with a horizontal axis. Two wheels sharing
 * one axis, two blocks apart along a horizontal direction perpendicular to
 * it and spinning opposite directions, form a crusher — the air gap between
 * them is the working spot (see TileEntity).
 */
@SuppressWarnings("deprecation")
public class BlockCrushingWheel extends BlockKineticHorizontalAxisBase implements ITileEntityProvider {
    public BlockCrushingWheel(MapColor color, SoundType soundType) {
        super(color, soundType);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityCrushingWheel();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing.Axis preferredAxis = getPreferredHorizontalAxis(world, pos);
        if (preferredAxis != null) return this.getDefaultState().withProperty(HORIZONTAL_AXIS, preferredAxis);
        return this.getDefaultState().withProperty(HORIZONTAL_AXIS, placer.getHorizontalFacing().rotateY().getAxis());
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
