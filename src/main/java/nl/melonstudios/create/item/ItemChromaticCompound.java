package nl.melonstudios.create.item;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

/**
 * Uncommon ingredient that collects light while lying on the ground.
 * Falls into the void -> Shadow Steel; charges on enough light
 * (or inside an active beacon beam) -> Refined Radiance.
 * Simplified backport of the reference ChromaticCompoundItem: the
 * belt/depot light-eating path is omitted; any placed light-emitting
 * block is eaten instead (line-of-sight check omitted).
 */
public class ItemChromaticCompound extends ItemNoGravity {
    public static final int LIGHT_NEEDED = 10;

    public ItemChromaticCompound() {
        this.setRegistryName("chromatic_compound");
        this.setUnlocalizedName("create.chromatic_compound");
        this.setMaxStackSize(16);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    public static int getLight(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : tag.getInteger("CollectingLight");
    }

    public static void setLight(ItemStack stack, int light) {
        if (light <= 0) {
            if (stack.hasTagCompound()) stack.getTagCompound().removeTag("CollectingLight");
            return;
        }
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("CollectingLight", light);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getLight(stack) > 0;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - (double) getLight(stack) / LIGHT_NEEDED;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0xFFFFFF;
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return getLight(stack) > 0 ? 1 : 16;
    }

    @Override
    public boolean onEntityItemUpdate(EntityItem entity) {
        World world = entity.world;
        entity.setNoGravity(true);
        if (world.isRemote) return false;

        ItemStack stack = entity.getItem();

        // Fell into the void: becomes Shadow Steel.
        if (entity.posY < 0) {
            ItemStack steel = new ItemStack(ItemInit.SHADOW_STEEL, stack.getCount());
            entity.setItem(steel);
            entity.getEntityData().setBoolean("JustCreated", true);
            return false;
        }

        // Fully charged: one compound converts to Refined Radiance.
        if (getLight(stack) >= LIGHT_NEEDED) {
            ItemStack radiance = new ItemStack(ItemInit.REFINED_RADIANCE);
            EntityItem radianceEntity = new EntityItem(world, entity.posX, entity.posY, entity.posZ, radiance);
            radianceEntity.setDefaultPickupDelay();
            radianceEntity.getEntityData().setBoolean("JustCreated", true);
            world.spawnEntity(radianceEntity);
            setLight(stack, 0);
            stack.shrink(1);
            if (stack.isEmpty()) entity.setDead();
            else entity.lifespan = 6000;
            return false;
        }

        // Inside an active beacon beam: the whole stack converts.
        if (isOverActiveBeacon(world, entity.getPosition())) {
            ItemStack radiance = new ItemStack(ItemInit.REFINED_RADIANCE, stack.getCount());
            entity.setItem(radiance);
            entity.getEntityData().setBoolean("JustCreated", true);
            return false;
        }

        // Eat a nearby placed light source (glowstone, torches in bulk, ...).
        if (entity.ticksExisted % 40 == 0 && stack.getCount() > 0) {
            BlockPos p = new BlockPos(
                    entity.posX + world.rand.nextInt(7) - 3,
                    entity.posY + world.rand.nextInt(5) - 2,
                    entity.posZ + world.rand.nextInt(7) - 3);
            IBlockState state = world.getBlockState(p);
            if (state.getLightValue(world, p) > 0
                    && state.getBlock() != Blocks.BEACON
                    && state.getBlock() != Blocks.BEDROCK
                    && state.getBlockHardness(world, p) >= 0
                    && world.getTileEntity(p) == null) {
                world.destroyBlock(p, false);
                ItemStack charged = stack.splitStack(1);
                setLight(charged, getLight(charged) + 1);
                EntityItem split = new EntityItem(world, entity.posX, entity.posY, entity.posZ, charged);
                split.setDefaultPickupDelay();
                world.spawnEntity(split);
                entity.lifespan = 6000;
                if (stack.isEmpty()) entity.setDead();
            }
        }
        return false;
    }

    private static boolean isOverActiveBeacon(World world, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        while (cursor.getY() > 0) {
            cursor.move(EnumFacing.DOWN);
            if (world.getBlockState(cursor).getBlock() == Blocks.BEDROCK) break;
            if (world.getBlockState(cursor).getBlock() == Blocks.BEACON) {
                TileEntity te = world.getTileEntity(cursor);
                // Field 0 of the beacon tile is the pyramid level count.
                return te instanceof TileEntityBeacon && ((TileEntityBeacon) te).getField(0) > 0;
            }
            if (world.getBlockState(cursor).isOpaqueCube()) break;
        }
        return false;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }
}
