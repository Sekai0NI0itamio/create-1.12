package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.actor.BlockEncasedFan;
import nl.melonstudios.create.recipe.PulverizationRecipe;
import nl.melonstudios.create.recipe.server.BlastingRecipes;
import nl.melonstudios.create.recipe.server.HauntingRecipes;
import nl.melonstudios.create.recipe.server.SmokingRecipes;
import nl.melonstudios.create.recipe.server.SplashingRecipes;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Encased fan: blows (positive speed) or sucks (negative) an air current up
 * to 20 blocks (range grows with RPM per the official table). Entities in
 * the stream are pushed; items are processed by whatever the stream passes
 * through: water (washing), fire/campfire/lit burner (smoking), lava/blazing
 * (blasting), soul fire/soul campfire (haunting).
 */
public class TileEntityEncasedFan extends TileEntityKinetic {
    /** Per-itemEntity processing progress (block pos independent). */
    private final Map<UUID, Integer> progress = new HashMap<>();

    public int airDistance;

    public static int rangeForSpeed(float rpm) {
        float a = Math.abs(rpm);
        if (a < 1) return 0;
        if (a <= 12) return 4;
        if (a <= 28) return 5;
        if (a < 32) return 5;
        if (a < 48) return 6;
        if (a < 64) return 7;
        if (a < 80) return 8;
        if (a < 96) return 9;
        if (a < 112) return 10;
        if (a < 128) return 11;
        if (a < 192) return 12;
        if (a < 256) return 16;
        return 20;
    }

    public EnumFacing getAirFacing() {
        try {
            return this.getState().getValue(BlockEncasedFan.FACING);
        } catch (Exception e) {
            return EnumFacing.UP;
        }
    }

    public boolean isPushing() {
        return this.getSpeed() > 0;
    }

    @Override
    public void tick() {
        super.tick();
        float speed = this.getSpeed();
        if (speed == 0) {
            this.airDistance = 0;
            return;
        }
        EnumFacing facing = this.getAirFacing();
        int range = rangeForSpeed(speed);
        this.airDistance = range;

        // Find how far air actually travels (blocked by solid cubes).
        int travel = range;
        for (int i = 1; i <= range; i++) {
            BlockPos p = this.pos.offset(facing, i);
            if (!this.world.isBlockLoaded(p)) {
                travel = i - 1;
                break;
            }
            IBlockState s = this.world.getBlockState(p);
            if (s.isOpaqueCube() && !isFanTransparent(s)) {
                travel = i - 1;
                break;
            }
        }

        EnumFanProcess process = detectProcess(facing, travel);

        // Push entities + process items in the stream.
        AxisAlignedBB stream = streamBox(facing, travel);
        List<Entity> entities = this.world.getEntitiesWithinAABB(Entity.class, stream, e -> e != null && e.isEntityAlive());
        boolean pushing = this.isPushing();
        for (Entity e : entities) {
            if (e instanceof EntityPlayer) {
                EntityPlayer pl = (EntityPlayer) e;
                if (pl.capabilities.isCreativeMode && pl.capabilities.isFlying) continue;
                if (Math.abs(speed) < 4) continue;
            }
            double dist = distanceAlong(e, facing);
            double accel = Math.abs(speed) / 512.0 / Math.max(1.0, dist / Math.max(1, travel));
            double mx = 0, my = 0, mz = 0;
            EnumFacing push = pushing ? facing : facing.getOpposite();
            mx = push.getFrontOffsetX() * accel;
            my = push.getFrontOffsetY() * accel;
            mz = push.getFrontOffsetZ() * accel;
            e.motionX += mx / 8.0;
            e.motionY += my / 8.0;
            e.motionZ += mz / 8.0;
            e.fallDistance = 0;
            if (e instanceof EntityItem && process != EnumFanProcess.NONE) {
                this.processItem((EntityItem) e, process);
            }
            if (e instanceof EntityLivingBase && process == EnumFanProcess.BLASTING) {
                e.setFire(2);
            }
        }

        if (this.world.isRemote && this.world.getTotalWorldTime() % 4 == 0) {
            this.spawnAirParticles(facing, travel, process);
        }
    }

    private static boolean isFanTransparent(IBlockState s) {
        Material m = s.getMaterial();
        return !m.isSolid() || m == Material.WATER || m == Material.LAVA || m == Material.FIRE
                || m == Material.GLASS || m == Material.ICE || m == Material.LEAVES || m == Material.PLANTS;
    }

    private AxisAlignedBB streamBox(EnumFacing facing, int travel) {
        BlockPos from = this.pos.offset(facing, 1);
        BlockPos to = this.pos.offset(facing, Math.max(1, travel));
        double x1 = Math.min(from.getX(), to.getX());
        double y1 = Math.min(from.getY(), to.getY());
        double z1 = Math.min(from.getZ(), to.getZ());
        double x2 = Math.max(from.getX(), to.getX()) + 1;
        double y2 = Math.max(from.getY(), to.getY()) + 1;
        double z2 = Math.max(from.getZ(), to.getZ()) + 1;
        return new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
    }

    private double distanceAlong(Entity e, EnumFacing facing) {
        double dx = e.posX - (this.pos.getX() + 0.5);
        double dy = e.posY - (this.pos.getY() + 0.5);
        double dz = e.posZ - (this.pos.getZ() + 0.5);
        return Math.abs(dx * facing.getFrontOffsetX() + dy * facing.getFrontOffsetY() + dz * facing.getFrontOffsetZ());
    }

    public enum EnumFanProcess {
        NONE, SPLASHING, SMOKING, BLASTING, HAUNTING
    }

    /** What does the stream pass through? Checked from the fan outward. */
    private EnumFanProcess detectProcess(EnumFacing facing, int travel) {
        boolean water = false;
        boolean fire = false;
        boolean lava = false;
        boolean soulFire = false;
        for (int i = 1; i <= travel; i++) {
            BlockPos p = this.pos.offset(facing, i);
            if (!this.world.isBlockLoaded(p)) break;
            IBlockState s = this.world.getBlockState(p);
            Material m = s.getMaterial();
            if (m == Material.WATER) water = true;
            else if (m == Material.LAVA) lava = true;
            else if (s.getBlock() == Blocks.FIRE) fire = true;
            else if (s.getBlock() == Blocks.MAGMA) lava = true;
            else if (s.getBlock() == Blocks.SOUL_SAND) soulFire = true;
            // Blaze burner heat (backport block): heated = smoking, superheated = blasting.
            else if (s.getBlock() instanceof nl.melonstudios.create.util.interfaces.IHeatProvider) {
                int heat = ((nl.melonstudios.create.util.interfaces.IHeatProvider) s.getBlock()).getHeat(this.world, p, s);
                if (heat >= 2) lava = true;
                else if (heat >= 1) fire = true;
            }
        }
        // Soul fire detection via block below fire positions (1.12 has no soul fire block):
        // check checked blocks' support for soul sand/soil.
        if (!soulFire) {
            for (int i = 1; i <= travel; i++) {
                BlockPos p = this.pos.offset(facing, i);
                if (!this.world.isBlockLoaded(p)) break;
                IBlockState s = this.world.getBlockState(p);
                if (s.getBlock() == Blocks.FIRE) {
                    IBlockState below = this.world.getBlockState(p.down());
                    if (below.getBlock() == Blocks.SOUL_SAND) soulFire = true;
                }
            }
        }
        if (soulFire) return EnumFanProcess.HAUNTING;
        if (lava) return EnumFanProcess.BLASTING;
        if (fire) return EnumFanProcess.SMOKING;
        if (water) return EnumFanProcess.SPLASHING;
        return EnumFanProcess.NONE;
    }

    private void processItem(EntityItem ei, EnumFanProcess process) {
        ItemStack stack = ei.getItem();
        if (stack.isEmpty()) return;
        UUID id = ei.getUniqueID();
        int need = 160;
        PulverizationRecipe recipe = null;
        switch (process) {
            case SPLASHING:
                recipe = SplashingRecipes.instance.getRecipeForInput(stack);
                break;
            case HAUNTING:
                recipe = HauntingRecipes.instance.getRecipeForInput(stack);
                break;
            case SMOKING:
                recipe = SmokingRecipes.instance.getRecipeForInput(stack);
                break;
            case BLASTING:
                recipe = BlastingRecipes.instance.getRecipeForInput(stack);
                break;
            default:
                return;
        }
        if (recipe == null) {
            // Blasting falls back to furnace smelting.
            if (process == EnumFanProcess.BLASTING) {
                ItemStack smelt = FurnaceRecipes.instance().getSmeltingResult(stack);
                if (!smelt.isEmpty()) {
                    this.progress.put(id, this.progress.getOrDefault(id, 0) + this.getProcessingSpeed());
                    if (this.progress.getOrDefault(id, 0) >= need) {
                        this.progress.remove(id);
                        ItemStack out = smelt.copy();
                        stack.shrink(1);
                        if (stack.isEmpty()) ei.setDead();
                        else ei.setItem(stack);
                        if (!this.world.isRemote) {
                            StackUtil.spawnItemNoVelocity(this.world, ei.posX, ei.posY, ei.posZ, out);
                        }
                        this.world.playSound(null, ei.posX, ei.posY, ei.posZ,
                                SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.6F, 1.2F);
                    }
                }
            }
            return;
        }
        this.progress.put(id, this.progress.getOrDefault(id, 0) + this.getProcessingSpeed());
        if (this.progress.getOrDefault(id, 0) >= Math.max(20, recipe.processingTime)) {
            this.progress.remove(id);
            stack.shrink(1);
            if (stack.isEmpty()) ei.setDead();
            else ei.setItem(stack);
            for (ItemStack out : Utils.rollChancedResults(recipe.results)) {
                if (out.isEmpty()) continue;
                if (!this.world.isRemote) {
                    StackUtil.spawnItemNoVelocity(this.world, ei.posX, ei.posY, ei.posZ, out);
                }
            }
            this.world.playEvent(2001, this.pos, net.minecraft.block.Block.getStateId(
                    net.minecraft.init.Blocks.WATER.getDefaultState()));
        }
    }

    public int getProcessingSpeed() {
        return MathHelper.clamp((int) Math.abs(this.getSpeed() / 8.0F), 1, 512);
    }

    @SideOnly(Side.CLIENT)
    private void spawnAirParticles(EnumFacing facing, int travel, EnumFanProcess process) {
        // Particles handled by TESR airflow; keep TE lean.
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("airDistance", this.airDistance);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.airDistance = nbt.getInteger("airDistance");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeInt(this.airDistance);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        this.airDistance = buf.readInt();
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 4);
    }
}
