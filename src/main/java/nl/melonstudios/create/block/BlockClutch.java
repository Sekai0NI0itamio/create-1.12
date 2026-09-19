package nl.melonstudios.create.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.tileentity.TileEntityClutch;

import javax.annotation.Nullable;

public class BlockClutch extends BlockGearshift {
    public BlockClutch(MapColor mapColor, SoundType soundType) {
        super(mapColor, soundType);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (worldIn.isRemote) return;

        boolean lastPowered = state.getValue(POWERED);
        if (lastPowered != isPosPowered(worldIn, pos)) {
            worldIn.setBlockState(pos, state.cycleProperty(POWERED), 18);
            this.detachKinetics(worldIn, pos, lastPowered);
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityClutch();
    }
}
