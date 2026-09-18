package nl.melonstudios.create.item;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

/**
 * Created when Chromatic Compound collects enough light.
 * Floats (no gravity), glints like an enchanted item.
 */
public class ItemRefinedRadiance extends ItemNoGravity {
    public ItemRefinedRadiance() {
        this.setRegistryName("refined_radiance");
        this.setUnlocalizedName("create.refined_radiance");
        this.setRarity(EnumRarity.UNCOMMON);
        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    protected void onCreated(EntityItem item, net.minecraft.nbt.NBTTagCompound data) {
        super.onCreated(item, data);
        item.motionY += 0.25;
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
