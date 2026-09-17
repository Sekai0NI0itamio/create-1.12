package nl.melonstudios.create.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry + spawn helpers for the 13 Create particle types (1.20.1 parity).
 * Sprites are original recreations registered into the terrain atlas;
 * behavior presets (lifetime, size, tint, fade) mirror AllParticleTypes.
 */
@SideOnly(Side.CLIENT)
public final class CreateParticles {
    private static final Map<String, TextureAtlasSprite[]> CACHE = new HashMap<>();

    private CreateParticles() {
    }

    public static void registerSprites(TextureMap map) {
        register(map, "particle/air", 4);
        register(map, "particle/steam_jet", 23);
        register(map, "particle/wifi", 16);
        register(map, "particle/soul_sprite_anim", 1, 16);
        register(map, "particle/soul_base", 8);
        register(map, "particle/drop", 1);
        register(map, "particle/square", 1);
        register(map, "particle/soft", 1);
        register(map, "particle/arrow", 1);
    }

    private static void register(TextureMap map, String base, int frames) {
        register(map, base, 0, frames);
    }

    private static void register(TextureMap map, String base, int first, int count) {
        for (int i = 0; i < count; i++) {
            map.registerSprite(new ResourceLocation("create", base + "_" + (first + i)));
        }
    }

    private static TextureAtlasSprite[] frames(String base, int count) {
        return frames(base, 0, count);
    }

    private static TextureAtlasSprite[] frames(String base, int first, int count) {
        String key = base + "@" + first;
        TextureAtlasSprite[] out = CACHE.get(key);
        if (out == null) {
            TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
            out = new TextureAtlasSprite[count];
            for (int i = 0; i < count; i++) {
                out[i] = map.getAtlasSprite("create:" + base + "_" + (first + i));
            }
            CACHE.put(key, out);
        }
        return out;
    }

    private static void add(World world, double x, double y, double z,
                            double mx, double my, double mz,
                            TextureAtlasSprite[] frames,
                            int lifetime, float scale, float gravity,
                            float damping, float grow, int tint,
                            float alphaStart, float alphaEnd, boolean fullBright) {
        if (world == null || world.isRemote == false) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.effectRenderer == null) {
            return;
        }
        mc.effectRenderer.addEffect(new ParticleCreate(world, x, y, z, mx, my, mz,
                frames, lifetime, scale, gravity,
                damping, grow, tint, alphaStart, alphaEnd, fullBright));
    }

    /** Fan air current streak. distFrac 0..1 along the stream, tint by processing. */
    public static void airFlow(World world, double x, double y, double z,
                               double mx, double my, double mz, int tint) {
        add(world, x, y, z, mx, my, mz, frames("particle/air", 4),
                40, 0.5F, 0.0F, 1.0F, 0.0F, tint, 0.25F, 0.1F, false);
    }

    /** Ambient drifting mote. */
    public static void air(World world, double x, double y, double z,
                           double mx, double my, double mz) {
        add(world, x, y, z, mx, my, mz, frames("particle/soft", 1),
                60, 0.35F, 0.0F, 0.98F, 0.0F, 0xEEEEEE, 0.2F, 0.0F, false);
    }

    /** Steam engine jet puff, oriented travel via velocity. */
    public static void steamJet(World world, double x, double y, double z,
                                double mx, double my, double mz) {
        add(world, x, y, z, mx, my, mz, frames("particle/steam_jet", 23),
                21, 0.375F, 0.0F, 0.96F, 0.03F, 0xFFFFFF, 0.9F, 0.0F, false);
    }

    /** Small tinted cube (train sparks, generic debris). */
    public static void cube(World world, double x, double y, double z,
                            double mx, double my, double mz, int tint) {
        add(world, x, y, z, mx, my, mz, frames("particle/square", 1),
                30, 0.25F, 0.6F, 0.98F, -0.002F, tint, 1.0F, 0.0F, false);
    }

    /** Fluid-tinted mote (spout splashes, pipe leaks). */
    public static void fluid(World world, double x, double y, double z,
                             double mx, double my, double mz, int tint) {
        add(world, x, y, z, mx, my, mz, frames("particle/square", 1),
                20, 0.3F, 0.4F, 0.98F, 0.0F, tint, 1.0F, 0.0F, false);
    }

    /** Fluid surface bob in basins. */
    public static void basinFluid(World world, double x, double y, double z, int tint) {
        add(world, x, y, z, 0.0D, 0.05D, 0.0D, frames("particle/soft", 1),
                30, 0.5F, -0.05F, 0.96F, 0.0F, tint, 0.8F, 0.0F, false);
    }

    /** Falling fluid drip. */
    public static void fluidDrip(World world, double x, double y, double z, int tint) {
        add(world, x, y, z, 0.0D, -0.1D, 0.0D, frames("particle/drop", 1),
                40, 0.4F, 1.0F, 1.0F, 0.0F, tint, 1.0F, 1.0F, false);
    }

    /** Redstone-link wireless pulse. */
    public static void wifi(World world, double x, double y, double z, int tint) {
        add(world, x, y, z, 0.0D, 0.25D, 0.0D, frames("particle/wifi", 16),
                20, 0.5F, 0.0F, 1.0F, 0.01F, tint, 1.0F, 0.0F, true);
    }

    /** Haunted soul wisp, rising. */
    public static void soul(World world, double x, double y, double z, int tint) {
        add(world, x, y, z, 0.0D, 0.15D, 0.0D, frames("particle/soul_sprite_anim", 1, 16),
                30, 0.5F, -0.02F, 0.98F, 0.0F, tint, 0.9F, 0.0F, true);
    }

    /** Soul fire base flame tint. */
    public static void soulBase(World world, double x, double y, double z) {
        add(world, x, y, z, 0.0D, 0.15D, 0.0D, frames("particle/soul_base", 8),
                30, 0.5F, -0.02F, 0.98F, 0.0F, 0xFF9A3C, 0.9F, 0.0F, true);
    }

    /** Static soul marker (perimeter). */
    public static void soulPerimeter(World world, double x, double y, double z) {
        add(world, x, y, z, 0.0D, 0.02D, 0.0D, frames("particle/soul_sprite_anim", 1, 16),
                40, 0.4F, 0.0F, 1.0F, 0.0F, 0xB3E5FF, 0.8F, 0.0F, true);
    }

    /** Expanding soul ring marker. */
    public static void soulExpanding(World world, double x, double y, double z) {
        add(world, x, y, z, 0.0D, 0.02D, 0.0D, frames("particle/soft", 1),
                20, 0.4F, 0.0F, 1.0F, 0.06F, 0xB3E5FF, 0.8F, 0.0F, true);
    }

    /** Rotation direction indicator. */
    public static void rotationIndicator(World world, double x, double y, double z, int tint) {
        add(world, x, y, z, 0.0D, 0.0D, 0.0D, frames("particle/arrow", 1),
                30, 0.5F, 0.0F, 1.0F, 0.0F, tint, 0.9F, 0.0F, true);
    }
}
