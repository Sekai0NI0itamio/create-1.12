package nl.melonstudios.create.block.redstone;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Display board: wide text panel rendering up to 3 display-link lines
 * (links behind it, stacked). Simplified backport: shows the first link
 * line found behind, plus label.
 */
public class BlockDisplayBoard extends Block {
    public BlockDisplayBoard() {
        super(Material.WOOD, MapColor.WOOD);
        this.blockSoundType = SoundType.WOOD;
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
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0, 0, 0.375, 1, 1, 0.625);
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "axe".equals(type);
    }
}
