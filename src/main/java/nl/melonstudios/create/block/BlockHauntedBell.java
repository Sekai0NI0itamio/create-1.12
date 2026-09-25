package nl.melonstudios.create.block;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Haunted bell. Reference: HauntedBellBlock (the converted form; lower ring
 * pitch, soul particles, pulses contraptions). 1.12 keeps the lower ring and
 * smoke-ember particles; the contraption pulse hook is NEEDS-LEAD.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockHauntedBell extends BlockPeculiarBell {
    /** Last constructed instance; set once BlockInit wiring lands (NEEDS-LEAD). */
    public static BlockHauntedBell instance;

    public BlockHauntedBell() {
        super();
        instance = this;
    }

    @Override
    public float ringPitch() {
        return 0.6F;
    }

    @Override
    protected void tryConvert(World worldIn, BlockPos pos, IBlockState state) {
        // Already converted; nothing to do.
    }

    /** Swaps a peculiar bell for the haunted variant when wiring exists. */
    public static void convert(World worldIn, BlockPos pos) {
        if (instance != null) worldIn.setBlockState(pos, instance.getDefaultState(), 3);
    }
}
