package nl.melonstudios.create.block.actor;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityPortableFluidInterface;

import javax.annotation.Nullable;

/**
 * Portable fluid interface: placed facing another one 1-2 blocks away (one
 * on a contraption), it bridges fluid between the contraption tank and pipes.
 * Simplified backport: exposes the neighbor contraption-side fluid handler
 * when a second interface faces it.
 */
@SuppressWarnings("deprecation")
public class BlockPortableFluidInterface extends BlockKineticBase implements ITileEntityProvider {
    public BlockPortableFluidInterface() {
        super(Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityPortableFluidInterface();
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
