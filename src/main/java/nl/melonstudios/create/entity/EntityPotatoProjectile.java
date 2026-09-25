package nl.melonstudios.create.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.entity.projectile.EntityThrowable;
import nl.melonstudios.create.init.SoundInit;

/**
 * Flying spud. Reference: PotatoProjectileEntity (damage/knockback per type,
 * Power/Punch scaling applied by the cannon, fire/poison/teleport/heal
 * on-entity effects, crop planting on farmland, ammo recovery drops,
 * short-reload targets lose i-frames).
 */
public class EntityPotatoProjectile extends EntityThrowable {
    /** Carried stats for one ammo kind. */
    public static class AmmoStats {
        public final float damage;
        public final int reloadTicks;
        public final float velocity;
        public final float knockback;
        /** 0 none, 1 ignite 3s, 2 poison, 3 chorus teleport, 4 heal. */
        public final int effect;
        public final ItemStack ammo;

        public AmmoStats(float damage, int reloadTicks, float velocity, float knockback, int effect, ItemStack ammo) {
            this.damage = damage;
            this.reloadTicks = reloadTicks;
            this.velocity = velocity;
            this.knockback = knockback;
            this.effect = effect;
            this.ammo = ammo;
        }
    }

    private float damage = 5.0F;
    private int reloadTicks = 15;
    private float knockback = 1.0F;
    private int effectId;
    private ItemStack ammo = ItemStack.EMPTY;
    private float recoveryChance;

    public EntityPotatoProjectile(World worldIn) {
        super(worldIn);
    }

    public EntityPotatoProjectile(World worldIn, EntityLivingBase thrower, float damage,
            int reloadTicks, float velocity, float knockback, int effectId, ItemStack ammo) {
        super(worldIn, thrower);
        this.damage = damage;
        this.reloadTicks = reloadTicks;
        this.knockback = knockback;
        this.effectId = effectId;
        this.ammo = ammo.isEmpty() ? ItemStack.EMPTY : ammo.copy();
        this.ammo.setCount(1);
        // Reference PotatoRecovery: 12.5% + 12.5%/lvl; no enchantment ported,
        // baseline recovery roll kept at zero unless set from NBT.
        this.recoveryChance = 0.0F;
        if (effectId == 1) this.setFire(3);
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (this.world.isRemote) {
            this.setDead();
            return;
        }
        if (result.entityHit instanceof EntityLivingBase) {
            EntityLivingBase target = (EntityLivingBase) result.entityHit;
            DamageSource src = DamageSource.causeThrownDamage(this, this.getThrower());
            if (target.attackEntityFrom(src, this.damage)) {
                if (this.knockback > 0.0F) {
                    double dx = target.posX - this.posX;
                    double dz = target.posZ - this.posZ;
                    double len = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
                    target.addVelocity(dx / len * this.knockback * 0.6D, 0.25D, dz / len * this.knockback * 0.6D);
                }
                // Reference drops target i-frames for fast ammo (reload < 10).
                if (this.reloadTicks < 10) target.hurtResistantTime = this.reloadTicks + 10;
                switch (this.effectId) {
                    case 1:
                        target.setFire(3);
                        break;
                    case 2:
                        target.addPotionEffect(new PotionEffect(MobEffects.POISON, 160, 1));
                        break;
                    case 3:
                        if (target instanceof EntityPlayer) {
                            EntityPlayer p = (EntityPlayer) target;
                            double nx = this.posX + (this.rand.nextDouble() - 0.5D) * 16.0D;
                            double nz = this.posZ + (this.rand.nextDouble() - 0.5D) * 16.0D;
                            p.attemptTeleport(nx, this.posY + 2.0D, nz);
                        }
                        break;
                    case 4:
                        // Reference cures zombie villagers; 1.12 equivalent: heal + regen.
                        target.heal(4.0F);
                        target.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 100, 1));
                        break;
                    default:
                        break;
                }
                this.world.playSound(null, this.posX, this.posY, this.posZ,
                        SoundInit.potato_hit, this.getSoundCategory(), 1.0F, 1.0F);
            }
            if (!this.ammo.isEmpty() && this.rand.nextFloat() <= this.recoveryChance) {
                target.entityDropItem(this.ammo.copy(), 0.5F);
            }
        } else if (result.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos pos = result.getBlockPos();
            if (!tryPlantCrop(pos) && !this.ammo.isEmpty() && this.rand.nextFloat() <= this.recoveryChance) {
                this.entityDropItem(this.ammo.copy(), 0.5F);
            }
            this.world.playSound(null, this.posX, this.posY, this.posZ,
                    SoundInit.potato_hit, this.getSoundCategory(), 0.7F, 1.2F);
            this.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, this.posX, this.posY, this.posZ,
                    0.0D, 0.0D, 0.0D, Item.getIdFromItem(this.ammo.isEmpty() ? Items.POTATO : this.ammo.getItem()));
        }
        this.setDead();
    }

    /** Potato/carrot ammo plants its crop on farmland (reference PlantCrop). */
    private boolean tryPlantCrop(BlockPos pos) {
        if (this.ammo.isEmpty()) return false;
        IBlockState state = this.world.getBlockState(pos);
        if (state.getBlock() != Blocks.FARMLAND) return false;
        BlockPos above = pos.up();
        if (!this.world.isAirBlock(above)) return false;
        String id = this.ammo.getItem().getRegistryName().toString();
        if (id.equals("minecraft:potato")) {
            this.world.setBlockState(above, Blocks.POTATOES.getDefaultState(), 3);
            return true;
        }
        if (id.equals("minecraft:carrot")) {
            this.world.setBlockState(above, Blocks.CARROTS.getDefaultState(), 3);
            return true;
        }
        return false;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setFloat("Damage", this.damage);
        compound.setInteger("Reload", this.reloadTicks);
        compound.setFloat("Knockback", this.knockback);
        compound.setInteger("Effect", this.effectId);
        compound.setFloat("Recovery", this.recoveryChance);
        if (!this.ammo.isEmpty()) compound.setTag("Ammo", this.ammo.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.damage = compound.getFloat("Damage");
        this.reloadTicks = compound.getInteger("Reload");
        this.knockback = compound.getFloat("Knockback");
        this.effectId = compound.getInteger("Effect");
        this.recoveryChance = compound.getFloat("Recovery");
        this.ammo = compound.hasKey("Ammo", 10) ? new ItemStack(compound.getCompoundTag("Ammo")) : ItemStack.EMPTY;
    }
}
