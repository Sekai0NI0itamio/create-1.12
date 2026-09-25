package nl.melonstudios.create.block.logistics;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;
import nl.melonstudios.create.tileentity.logistics.TileEntityRepackager;

import javax.annotation.Nullable;

/**
 * Repackager: the packager variant that works packages back into stock.
 * Ports the reference RepackagerBlockEntity behaviour: pulls package items
 * out of the attached inventory below, holds each one through the 20-tick
 * iris cycle, and re-emits it with the configured address so fragmented
 * orders recombine downstream. Speed-gated like the packager.
 */
@SuppressWarnings("deprecation")
public class BlockRepackager extends BlockPackager {
    public BlockRepackager() {
        super();
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityRepackager();
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }
}
