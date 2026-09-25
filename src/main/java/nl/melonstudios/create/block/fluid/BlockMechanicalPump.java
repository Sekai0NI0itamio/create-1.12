package nl.melonstudios.create.block.fluid;

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
import nl.melonstudios.create.block.BlockKineticDirectionalBase;
import nl.melonstudios.create.tileentity.TileEntityMechanicalPump;

import javax.annotation.Nullable;

/**
 * Mechanical pump: kinetic pressure source for fluid pipes.
 * Translated from the reference PumpBlock (never pasted):
 * flow axis is the FACING axis, open on both ends of that axis,
 * rotation axis is the same axis, and neighbour pipe changes are
 * propagated by scheduling a re-scan on the pump TE.
 */
@SuppressWarnings("deprecation")
public class BlockMechanicalPump extends BlockKineticDirectionalBase implements ITileEntityProvider {
    public BlockMechanicalPump() {
        super(Material.ROCK, MapColor.STONE);
        this.setRegistryName("mechanical_pump");
        this.setUnlocalizedName("create.mechanical_pump");
        this.blockSoundType = SoundType.STONE;
        this.setHardness(3.0F);
        this.setResistance(6.0F);
    }

    /** Pipe/flow ends of the pump: both faces along the FACING axis. */
    public static boolean isOpenAt(IBlockState state, EnumFacing side) {
        return side.getAxis() == state.getValue(FACING).getAxis();
    }

    public static boolean isPump(IBlockState state) {
        return state.getBlock() instanceof BlockMechanicalPump;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityMechanicalPump();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityMechanicalPump)
                ((TileEntityMechanicalPump) te).clearPumpPressures();
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, net.minecraft.block.Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
        if (worldIn.isRemote)
            return;
        EnumFacing changed = null;
        for (EnumFacing side : EnumFacing.VALUES) {
            if (pos.offset(side).equals(fromPos)) {
                changed = side;
                break;
            }
        }
        if (changed == null || !isOpenAt(state, changed))
            return;
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityMechanicalPump)
            ((TileEntityMechanicalPump) te).onPipeChanged();
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
