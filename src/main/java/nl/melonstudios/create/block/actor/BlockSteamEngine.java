package nl.melonstudios.create.block.actor;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntitySteamEngine;

import javax.annotation.Nullable;

/**
 * Steam engine: attaches to a boiler tank, generates rotation. Power =
 * 16 SU/RPM * efficiency, efficiency = min(1, heat/engines). Speed fixed
 * 16 RPM like official (configurable via gauge later).
 */
@SuppressWarnings("deprecation")
public class BlockSteamEngine extends BlockKineticBase implements ITileEntityProvider {
    public BlockSteamEngine() {
        super(Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySteamEngine();
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return EnumFacing.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side == EnumFacing.UP;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
