package nl.melonstudios.create.worldgen;

import com.google.common.base.Predicate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import nl.melonstudios.create.block.BlockOre;
import nl.melonstudios.create.init.BlockInit;

import java.util.Random;

@SuppressWarnings("unused")
public class CreateWorldGen implements IWorldGenerator {
    private static boolean generateOres = true;
    private static boolean generateStones = true;
    public static void setGenerationFlags(boolean ores, boolean stones) {
        generateOres = ores;
        generateStones = stones;
    }

    private static final class VeinSpec {
        final IBlockState state;
        final int count;
        final Predicate<IBlockState> replaceable;

        VeinSpec(IBlockState state, int count, Predicate<IBlockState> replaceable) {
            this.state = state;
            this.count = count;
            this.replaceable = replaceable;
        }
    }

    private final VeinSpec copper;
    private final VeinSpec zinc;
    private final VeinSpec asurine;
    private final VeinSpec crimsite;
    private final VeinSpec limestone;
    private final VeinSpec ochrum;
    private final VeinSpec scorchia;
    private final VeinSpec scoria;
    private final VeinSpec veridium;

    public CreateWorldGen() {
        // Copper follows the 1.20.1 distribution: blobs of up to 10, spread
        // from the bottom of the world up to Y=112 with the peak around 48.
        // (1.12 worlds bottom out at Y=0, so the range is clamped to 2-112.)
        Predicate<IBlockState> stone = BlockMatcher.forBlock(Blocks.STONE);
        Predicate<IBlockState> netherrack = BlockMatcher.forBlock(Blocks.NETHERRACK);
        this.copper = new VeinSpec(BlockOre.copper(), 10, stone);
        this.zinc = new VeinSpec(BlockOre.zinc(), 6, stone);

        this.asurine = new VeinSpec(BlockInit.ORESTONE.getStateFromMeta(0), 128, stone);
        this.crimsite = new VeinSpec(BlockInit.ORESTONE.getStateFromMeta(1), 128, stone);
        this.limestone = new VeinSpec(BlockInit.ORESTONE.getStateFromMeta(2), 128, stone);
        this.ochrum = new VeinSpec(BlockInit.ORESTONE.getStateFromMeta(3), 128, stone);
        this.scorchia = new VeinSpec(BlockInit.ORESTONE.getStateFromMeta(4), 128, netherrack);
        this.scoria = new VeinSpec(BlockInit.ORESTONE.getStateFromMeta(5), 128, netherrack);
        this.veridium = new VeinSpec(BlockInit.ORESTONE.getStateFromMeta(6), 128, stone);
    }
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.getDimension() == -1) {
            this.genNether(random, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
        } else if (world.provider.getDimension() == 1) {
            this.genEnd(random, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
        } else {
            this.genOverworld(random, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
        }
    }

    private void genNether(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (generateStones) {
            if (random.nextInt(64) == 0) {
                this.runGenerator(this.scorchia, world, random, chunkX, chunkZ, 1, 0, 64);
            }
            if (random.nextInt(64) == 0) {
                this.runGenerator(this.scoria, world, random, chunkX, chunkZ, 1, 64, 128);
            }
        }
    }
    private void genEnd(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        // NOOP
    }
    private void genOverworld(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (generateOres) {
            this.runGenerator(this.copper, world, random, chunkX, chunkZ, 16, 2, 112);
            this.runGenerator(this.zinc, world, random, chunkX, chunkZ, 8, 4, 64);
        }
        if (generateStones) {
            if (random.nextInt(64) == 0) {
                this.runGenerator(this.asurine, world, random, chunkX, chunkZ, 1, 0, 64);
            }
            if (random.nextInt(64) == 0) {
                this.runGenerator(this.crimsite, world, random, chunkX, chunkZ, 1, 0, 64);
            }
            if (random.nextInt(64) == 0) {
                this.runGenerator(this.limestone, world, random, chunkX, chunkZ, 1, 0, 64);
            }
            if (random.nextInt(64) == 0) {
                this.runGenerator(this.ochrum, world, random, chunkX, chunkZ, 1, 0, 64);
            }
            if (random.nextInt(64) == 0) {
                this.runGenerator(this.veridium, world, random, chunkX, chunkZ, 1, 0, 64);
            }
        }
    }

    private void runGenerator(VeinSpec spec, World world, Random random,
                              int chunkX, int chunkZ, int veins, int minHeight, int maxHeight) {
        int heightDiff = maxHeight - minHeight;

        for (int i = 0; i < veins; i++) {
            int x = chunkX * 16 + random.nextInt(16);
            int z = chunkZ * 16 + random.nextInt(16);
            int y = minHeight + random.nextInt(heightDiff);

            new BoundedVeinGenerator(spec.state, spec.count, spec.replaceable, chunkX, chunkZ)
                    .generate(world, random, new BlockPos(x, y, z));
        }
    }
}
