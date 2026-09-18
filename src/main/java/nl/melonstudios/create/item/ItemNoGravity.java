package nl.melonstudios.create.item;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

public class ItemNoGravity extends Item {
    @Override
    public boolean onEntityItemUpdate(EntityItem entityItem) {
        World world = entityItem.world;
        NBTTagCompound data = entityItem.getEntityData();

        if (world.isRemote) {
            if (entityItem.isSilent() && !data.getBoolean("PlayEffects")) {
                world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                        entityItem.posX, entityItem.posY, entityItem.posZ, 0, 0, 0);
                for (int i = 0; i < 20; i++) {
                    double motionX = (world.rand.nextFloat() - 0.5) * 2;
                    double motionY = 1 + (world.rand.nextFloat() - 0.5) * 2;
                    double motionZ = (world.rand.nextFloat() - 0.5) * 2;
                    world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                            entityItem.posX, entityItem.posY, entityItem.posZ,
                            motionX, motionY, motionZ);
                    world.spawnParticle(EnumParticleTypes.END_ROD,
                            entityItem.posX, entityItem.posY, entityItem.posZ,
                            motionX, motionY, motionZ);
                }
                data.setBoolean("PlayEffects", true);
            }
            return false;
        }

        entityItem.setNoGravity(true);
        if (!data.hasKey("JustCreated")) return false;
        this.onCreated(entityItem, data);
        return false;
    }

    protected void onCreated(EntityItem item, NBTTagCompound data) {
        item.lifespan = 6000;
        data.removeTag("JustCreated");
        item.setSilent(true);
    }
}
