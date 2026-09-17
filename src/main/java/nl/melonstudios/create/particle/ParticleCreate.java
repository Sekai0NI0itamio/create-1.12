package nl.melonstudios.create.particle;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Single flexible Create particle for 1.12.2.
 * Mirrors the look of the 1.20.1 reference particles (flipbook frames,
 * tint, fade, growth, damping) using the terrain atlas (FX layer 1).
 */
@SideOnly(Side.CLIENT)
public class ParticleCreate extends net.minecraft.client.particle.Particle {
    private final TextureAtlasSprite[] frames;
    private final int agePerFrame;
    private final float damping;
    private final float grow;
    private final float alphaStart;
    private final float alphaEnd;
    private final boolean fullBright;

    public ParticleCreate(World world, double x, double y, double z,
                          double mx, double my, double mz,
                          TextureAtlasSprite[] frames, int lifetime, float scale,
                          float gravity, float damping, float grow,
                          int tint, float alphaStart, float alphaEnd,
                          boolean fullBright) {
        super(world, x, y, z, mx, my, mz);
        this.frames = frames;
        this.agePerFrame = 2;
        this.damping = damping;
        this.grow = grow;
        this.alphaStart = alphaStart;
        this.alphaEnd = alphaEnd;
        this.fullBright = fullBright;
        this.particleMaxAge = Math.max(1, lifetime);
        this.particleScale = scale;
        this.particleGravity = gravity;
        this.setRBGColorF(((tint >> 16) & 0xFF) / 255.0F,
                ((tint >> 8) & 0xFF) / 255.0F,
                (tint & 0xFF) / 255.0F);
        this.particleAlpha = alphaStart;
        if (frames.length > 0 && frames[0] != null) {
            this.setParticleTexture(frames[0]);
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        if (this.fullBright) {
            return 15728880;
        }
        return super.getBrightnessForRender(partialTicks);
    }

    @Override
    public void onUpdate() {
        if (this.frames.length > 1) {
            TextureAtlasSprite frame = this.frames[(this.particleAge / this.agePerFrame) % this.frames.length];
            if (frame != null) {
                this.setParticleTexture(frame);
            }
        }
        this.motionX *= this.damping;
        this.motionY *= this.damping;
        this.motionZ *= this.damping;
        super.onUpdate();
        if (!this.isExpired) {
            float t = (float) this.particleAge / (float) this.particleMaxAge;
            if (t > 1.0F) {
                t = 1.0F;
            }
            this.particleAlpha = this.alphaStart + (this.alphaEnd - this.alphaStart) * t;
            this.particleScale += this.grow;
        }
    }
}
