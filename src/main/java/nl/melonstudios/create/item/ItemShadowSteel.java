package nl.melonstudios.create.item;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.EnumRarity;
import net.minecraft.util.EnumParticleTypes;
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
    protected void onCreated(EntityItem item, net.minecraft.nbt.NBTTagCompound data) {
        super.onCreated(item, data);
        float yMotion = (item.fallDistance + 3) / 50.0F;
        item.motionY = yMotion;
    }

    @Override
    public boolean onEntityItemUpdate(EntityItem entity) {
        super.onEntityItemUpdate(entity);
        World world = entity.world;
        if (world.isRemote && world.rand.nextFloat() < 0.15F) {
            world.spawnParticle(EnumParticleTypes.END_ROD,
                    entity.posX + (world.rand.nextFloat() - 0.5),
                    entity.posY + world.rand.nextFloat() * 0.5,
                    entity.posZ + (world.rand.nextFloat() - 0.5),
                    0, -0.1, 0);
        }
        return false;
    }
}
