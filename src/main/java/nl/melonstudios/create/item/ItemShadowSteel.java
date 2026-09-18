package nl.melonstudios.create.item;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.EnumRarity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

/**
 * Created when Chromatic Compound falls into the void.
 * Floats (no gravity) with a small upward pop on creation.
 */
public class ItemShadowSteel extends ItemNoGravity {
    public ItemShadowSteel() {
        this.setRegistryName("shadow_steel");
        this.setUnlocalizedName("create.shadow_steel");
        this.setRarity(EnumRarity.UNCOMMON);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    protected void onCreated(EntityItem item, NBTTagCompound data) {
        super.onCreated(item, data);
        float yMotion = (item.fallDistance + 3) / 50.0F;
        item.motionX = 0;
        item.motionY = yMotion;
        item.motionZ = 0;
    }

    @Override
    public boolean onEntityItemUpdate(EntityItem entity) {
        super.onEntityItemUpdate(entity);
        World world = entity.world;
        if (world.isRemote && world.rand.nextFloat() < getIdleParticleChance(entity)) {
            world.spawnParticle(EnumParticleTypes.END_ROD,
                    entity.posX + (world.rand.nextFloat() - 0.5),
                    entity.posY,
                    entity.posZ + (world.rand.nextFloat() - 0.5),
                    0, -0.1, 0);
        }
        return false;
    }

    private float getIdleParticleChance(EntityItem entity) {
        int count = entity.getItem().getCount();
        float min = MathHelper.clamp((float) (entity.motionY * 20), 5.0F, 20.0F);
        return MathHelper.clamp((float) (count - 10), min, 100.0F) / 64.0F;
    }
}
