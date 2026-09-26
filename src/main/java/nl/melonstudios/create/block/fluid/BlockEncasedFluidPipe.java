package nl.melonstudios.create.block.fluid;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.fluid.IFluidPipeConnectable;
import nl.melonstudios.create.tileentity.fluid.TileEntityFluidPipe;
import nl.melonstudios.create.util.BlockProperties;

/**
 * Encased fluid pipe: the same fluid connection on every side, hidden
 * inside a full copper-casing cube.
 *
 * <p>Ports the reference {@code EncasedPipeBlock} in its cheapest honest
 * form. The modern version reuses the six-way open/closed properties of
 * the standard pipe; the backport renders the casing as a plain full
 * cube (the existing {@code encased_pipe} art), so no per-side state is
 * needed at all — one blockstate, one model. Connection-wise it accepts
 * every side whose neighbour accepts it back, exactly like the standard
 * pipe with all six arms open.</p>
 */
public class BlockEncasedFluidPipe extends Block implements IFluidPipeConnectable {
    public BlockEncasedFluidPipe() {
        super(Material.IRON);
        this.setHardness(3.0F);
        this.setResistance(BlockProperties.IRON_RESISTANCE);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("encased_fluid_pipe");
        this.setUnlocalizedName("create.encased_fluid_pipe");
    }

    @Override
    public boolean canConnectPipe(World world, BlockPos pos, EnumFacing side) {
        return BlockFluidPipe.canConnectTo(world, pos, side);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityFluidPipe();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        worldIn.removeTileEntity(pos);
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos,
                                Block blockIn, BlockPos fromPos) {
        worldIn.notifyBlockUpdate(pos, state, state, 3);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }
}
