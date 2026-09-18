package nl.melonstudios.create.worldgen;

import com.google.common.base.Predicate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

/**
 * Minable-style ore vein confined to the chunk being populated.
 * Vanilla WorldGenMinable lets veins wander into neighboring chunks, forcing
 * them to load mid-populate (cascading worldgen lag). This skips any
 * placement outside the target chunk instead.
 */
public class BoundedVeinGenerator extends WorldGenerator {
    private final IBlockState state;
    private final int count;
    private final Predicate<IBlockState> replaceable;
    private final int minChunkX;
    private final int minChunkZ;

    public BoundedVeinGenerator(IBlockState state, int count, Predicate<IBlockState> replaceable, int chunkX, int chunkZ) {
        this.state = state;
        this.count = count;
        this.replaceable = replaceable;
        this.minChunkX = chunkX * 16;
        this.minChunkZ = chunkZ * 16;
    }

    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        float angle = rand.nextFloat() * (float) Math.PI;
        double x0 = (double) ((float) (pos.getX() + 8) + MathHelper.sin(angle) * (float) this.count / 8.0F);
        double x1 = (double) ((float) (pos.getX() + 8) - MathHelper.sin(angle) * (float) this.count / 8.0F);
        double z0 = (double) ((float) (pos.getZ() + 8) + MathHelper.cos(angle) * (float) this.count / 8.0F);
        double z1 = (double) ((float) (pos.getZ() + 8) - MathHelper.cos(angle) * (float) this.count / 8.0F);
        double y0 = (double) (pos.getY() + rand.nextInt(3) - 2);
        double y1 = (double) (pos.getY() + rand.nextInt(3) - 2);

        for (int i = 0; i <= this.count; ++i) {
            double t = (double) i / (double) this.count;
            double cx = x0 + (x1 - x0) * t;
            double cy = y0 + (y1 - y0) * t;
            double cz = z0 + (z1 - z0) * t;
            double spread = rand.nextDouble() * (double) this.count / 16.0D;
            double rx = (MathHelper.sin((float) Math.PI * (float) t) + 1.0F) * spread + 1.0D;
            double ry = (MathHelper.sin((float) Math.PI * (float) t) + 1.0F) * spread + 1.0D;

            int xStart = MathHelper.floor(cx - rx / 2.0D);
            int yStart = MathHelper.floor(cy - ry / 2.0D);
            int zStart = MathHelper.floor(cz - rx / 2.0D);
            int xEnd = MathHelper.floor(cx + rx / 2.0D);
            int yEnd = MathHelper.floor(cy + ry / 2.0D);
            int zEnd = MathHelper.floor(cz + rx / 2.0D);

            for (int x = xStart; x <= xEnd; ++x) {
                if (x < this.minChunkX || x > this.minChunkX + 15) {
                    continue;
                }
                double dx = ((double) x + 0.5D - cx) / (rx / 2.0D);
                if (dx * dx >= 1.0D) {
                    continue;
                }
                for (int y = yStart; y <= yEnd; ++y) {
                    if (y < 0 || y > 255) {
                        continue;
                    }
                    double dy = ((double) y + 0.5D - cy) / (ry / 2.0D);
                    if (dx * dx + dy * dy >= 1.0D) {
                        continue;
                    }
                    for (int z = zStart; z <= zEnd; ++z) {
                        if (z < this.minChunkZ || z > this.minChunkZ + 15) {
                            continue;
                        }
                        double dz = ((double) z + 0.5D - cz) / (rx / 2.0D);
                        if (dx * dx + dy * dy + dz * dz < 1.0D) {
                            BlockPos p = new BlockPos(x, y, z);
                            IBlockState current = world.getBlockState(p);
                            if (world.isBlockLoaded(p) && this.replaceable.apply(current)) {
                                world.setBlockState(p, this.state, 2);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
