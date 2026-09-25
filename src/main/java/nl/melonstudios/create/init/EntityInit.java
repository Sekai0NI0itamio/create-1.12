package nl.melonstudios.create.init;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.entity.train.EntityTrain;
import nl.melonstudios.create.entity.EntityPotatoProjectile;

/**
 * Custom entity registrations (vanilla registry path; contraption entities
 * spawn directly and don't need this).
 */
public final class EntityInit {
    private EntityInit() {
    }

    public static void init(Object modInstance) {
        int id = 0;
        EntityRegistry.registerModEntity(new ResourceLocation("create", "train"),
                EntityTrain.class, "create.train", id++, modInstance, 128, 1, true);
        EntityRegistry.registerModEntity(new ResourceLocation("create", "potato_projectile"),
                EntityPotatoProjectile.class, "create.potato_projectile", id++, modInstance, 64, 10, true);
    }
}
