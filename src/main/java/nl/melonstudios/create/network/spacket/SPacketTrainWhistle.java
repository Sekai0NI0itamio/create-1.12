package nl.melonstudios.create.network.spacket;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import nl.melonstudios.create.entity.train.EntityTrain;
import nl.melonstudios.create.network.CreateLegacyPacketManager;
import nl.melonstudios.create.network.SPacketBase;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serverbound train-whistle honk request.
 *
 * <p>Flow: client (train controls GUI / whistle key) sends the ridden train's
 * entity id plus a whistle pitch; the server re-resolves the train in the
 * sender's world, rate-limits per player, and plays the whistle sound at the
 * train position so every nearby player hears it. Sound choice mirrors
 * {@code TileEntitySteamWhistle#hoot} (vanilla harp stand-in until the
 * whistle oggs are wired).</p>
 *
 * <p>NEEDS-LEAD (registration, exact content):</p>
 * <pre>
 * // CreateLegacySPackets.java
 * public static final SPacketTrainWhistle TRAIN_WHISTLE = new SPacketTrainWhistle();
 * // CreateLegacyPacketManager constructor, next free id (after SCROLL_INTERACTION=0):
 * register(CreateLegacySPackets.TRAIN_WHISTLE, 1);
 * </pre>
 */
public class SPacketTrainWhistle extends SPacketBase {
    /** Minimum milliseconds between honks from one player. */
    public static final long HONK_COOLDOWN_MS = 1000L;
    /** Audible radius hint for future targeted sends; playSound handles range. */
    public static final double AUDIBLE_RADIUS = 64.0;

    private static final Map<UUID, Long> LAST_HONK = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public FMLProxyPacket handle(PacketBuffer data, EntityPlayerMP player) {
        int trainId = data.readInt();
        float pitch = data.readFloat();
        if (pitch < 0.5F) pitch = 0.5F;
        if (pitch > 2.0F) pitch = 2.0F;
        if (player == null || player.world == null || player.world.isRemote) return null;

        long now = System.currentTimeMillis();
        Long last = LAST_HONK.get(player.getUniqueID());
        if (last != null && now - last < HONK_COOLDOWN_MS) return null;
        LAST_HONK.put(player.getUniqueID(), now);

        Entity entity = player.world.getEntityByID(trainId);
        if (!(entity instanceof EntityTrain)) return null;
        EntityTrain train = (EntityTrain) entity;
        if (!train.isEntityAlive()) return null;

        train.world.playSound(null, train.posX, train.posY, train.posZ,
                SoundEvents.BLOCK_NOTE_HARP, SoundCategory.BLOCKS, 1.5F, pitch);
        return null;
    }

    /** Build a serverbound honk request for the given train entity id. */
    public FMLProxyPacket create(int trainEntityId, float pitch) {
        float clamped = Math.max(0.5F, Math.min(2.0F, pitch));
        PacketBuffer data = this.buf();
        data.writeInt(trainEntityId);
        data.writeFloat(clamped);
        return new FMLProxyPacket(data, CreateLegacyPacketManager.CHANNEL);
    }

    /** Pitch matching the whistle-stack formula (extensions 0-6, size drop 0/2/4 semitones). */
    public static float pitchFor(int extensions, int sizeDropSemitones) {
        int ext = Math.max(0, Math.min(6, extensions));
        return (float) Math.pow(2.0, -(ext * 2 + sizeDropSemitones) / 12.0);
    }
}
