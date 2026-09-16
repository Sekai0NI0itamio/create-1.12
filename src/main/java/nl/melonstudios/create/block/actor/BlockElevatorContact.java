package nl.melonstudios.create.block.actor;

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

import javax.annotation.Nullable;

/**
 * Elevator contact: floor marker the elevator stops at. Emits redstone while
 * the cabin is docked.
 */
public class BlockElevatorContact extends net.minecraft.block.Block implements ITileEntityProvider {
    public static final PropertyBool POWERING = PropertyBool.create("powering");

    public BlockElevatorContact() {
        super(Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;
        this.setDefaultState(this.blockState.getBaseState().withProperty(POWERING, false));
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new nl.melonstudios.create.tileentity.actor.TileEntityElevatorContact();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWERING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(POWERING) ? 1 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(POWERING, meta != 0);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return state.getValue(POWERING) ? 15 : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0, 0.625, 0, 1, 1, 1);
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
