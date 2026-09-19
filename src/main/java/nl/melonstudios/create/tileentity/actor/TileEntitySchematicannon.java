package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.item.ItemSchematic;
import nl.melonstudios.create.tileentity.TileEntityKinetic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schematicannon: loads a finished schematic from the player's hand
 * (right-click handled here via first-tick hand scan is skipped — instead
 * sneak-right-click the cannon with the schematic to load), consumes
 * gunpowder (SHOTS_PER_GUNPOWDER blocks per powder, matching the reference
 * default of 400) + materials from adjacent inventories, fires one block per
 * FIRE_DELAY_TICKS (matching the reference schematicannonDelay default of 10).
 */
public class TileEntitySchematicannon extends TileEntityKinetic {
    /** Matches reference CSchematics#schematicannonShotsPerGunpowder default (400). */
    public static final int SHOTS_PER_GUNPOWDER = 400;
    /** Matches reference CSchematics#schematicannonDelay default (10 ticks between shots). */
    public static final int FIRE_DELAY_TICKS = 10;
    /** Matches reference SchematicannonBlockEntity#MAX_ANCHOR_DISTANCE (256). */
    public static final int MAX_ANCHOR_DISTANCE = 256;

    public NBTTagCompound schematic;
    public BlockPos anchor;
    public int cursor;
    public boolean running;
    public int gunpowder;
    public String statusCache = "Idle";
    public int printerCooldown;
    /** Yaw the cannon rests at when it has no target (set on placement, like the reference). */
    public float defaultYaw;
    /** Next block about to print; synced for client-side aiming. Null when idle. */
    public BlockPos aimTarget;
    /** Last fired shot; synced for the client flight-tracer render. */
    public BlockPos lastShotTarget;
    public long lastShotTime;
    public int lastShotBlockId = -1;
    public int lastShotMeta;

    public void toggle() {
        if (this.schematic == null) {
            this.statusCache = "Invalid Blueprint";
            return;
        }
        this.running = !this.running;
        this.statusCache = this.running ? "Running" : "Paused";
    }

    public void stop() {
        this.running = false;
        this.cursor = 0;
        this.statusCache = "Stopped";
    }

    public String status() {
        return this.statusCache;
    }

    public boolean loadSchematic(ItemStack stack, BlockPos anchor) {
        if (!ItemSchematic.isFinished(stack) || !stack.hasTagCompound()) return false;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("Dim") && tag.getInteger("Dim") != this.world.provider.getDimension()) {
            this.statusCache = "Wrong Dimension";
            return false;
        }
        if (anchor.distanceSq(this.pos) > (long) MAX_ANCHOR_DISTANCE * MAX_ANCHOR_DISTANCE) {
            this.statusCache = "Target Outside Range";
            return false;
        }
        this.schematic = tag.copy();
        this.anchor = anchor;
        this.cursor = 0;
        this.printerCooldown = 0;
        this.aimTarget = null;
        this.statusCache = "Ready";
        this.sync();
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.world.isRemote) return;
        if (!this.running || this.schematic == null || this.getSpeed() == 0) return;
        if (this.printerCooldown > 0) {
            this.printerCooldown--;
            return;
        }
        if (this.gunpowder <= 0) {
            this.pullGunpowder();
            if (this.gunpowder <= 0) {
                this.statusCache = "Out of Gunpowder";
                return;
            }
        }
        NBTTagList blocks = this.schematic.getTagList("Blocks", 10);
        int skipped = 0;
        while (this.cursor < blocks.tagCount() && skipped < 1000) {
            NBTTagCompound e = blocks.getCompoundTagAt(this.cursor);
            int[] rel = e.getIntArray("Pos");
            BlockPos target = this.anchor.add(rel[0], rel[1], rel[2]);
            if (!this.world.isBlockLoaded(target)) {
                this.statusCache = "Block is not loaded";
                return;
            }
            Block block = Block.getBlockFromName(e.getString("Block"));
            if (block == null) {
                // Unknown/mod-missing block: skip like the reference "searching" pass.
                this.cursor++;
                skipped++;
                continue;
            }
            @SuppressWarnings("deprecation")
            IBlockState want = block.getStateFromMeta(e.getInteger("Meta"));
            if (shouldSkipPlacement(want)) {
                // Second halves of doors/beds, piston heads: never placed directly.
                this.cursor++;
                skipped++;
                continue;
            }
            if (target.distanceSq(this.pos) < 4) {
                // Reference never prints within 2 blocks of the cannon itself.
                this.cursor++;
                skipped++;
                continue;
            }
            IBlockState cur = this.world.getBlockState(target);
            if (cur.getBlock() == block && cur.getBlock().getMetaFromState(cur) == e.getInteger("Meta")) {
                this.cursor++;
                skipped++;
                continue;
            }
            ItemStack need = new ItemStack(block, 1, e.getInteger("Meta"));
            if (!this.consumeMaterial(need)) {
                this.statusCache = "Missing Block";
                return;
            }
            this.world.setBlockState(target, want, 2);
            TileEntity te = this.world.getTileEntity(target);
            if (te != null && e.hasKey("TE", 10)) {
                try {
                    te.readFromNBT(e.getCompoundTag("TE"));
                    te.setPos(target);
                } catch (Exception ignored) {
                }
            }
            this.cursor++;
            this.gunpowder--;
            this.printerCooldown = FIRE_DELAY_TICKS;
            this.aimTarget = target;
            this.lastShotTarget = target;
            this.lastShotTime = this.world.getTotalWorldTime();
            this.lastShotBlockId = Block.getIdFromBlock(block);
            this.lastShotMeta = e.getInteger("Meta");
            this.world.playSound(null, this.pos, SoundInit.schematicannon_launch_block, SoundCategory.BLOCKS, 1.0F, 1.0F);
            if (this.cursor >= blocks.tagCount()) {
                this.running = false;
                this.cursor = 0;
                this.aimTarget = null;
                this.statusCache = "Done";
                this.world.playSound(null, this.pos, SoundInit.schematicannon_finish, SoundCategory.BLOCKS, 1.0F, 1.0F);
                this.sync();
                return;
            }
            this.statusCache = "Running " + this.cursor + "/" + blocks.tagCount();
            this.sync();
            this.markDirty();
            return;
        }
        if (this.cursor >= blocks.tagCount()) {
            this.running = false;
            this.cursor = 0;
            this.aimTarget = null;
            this.statusCache = "Done";
            this.world.playSound(null, this.pos, SoundInit.schematicannon_finish, SoundCategory.BLOCKS, 1.0F, 1.0F);
            this.sync();
            return;
        }
        this.statusCache = "Running " + this.cursor + "/" + blocks.tagCount();
        if (this.world.getTotalWorldTime() % 20 == 0) this.sync();
        this.markDirty();
    }

    /**
     * Translate of the reference shouldIgnoreBlockState: blocks that must never
     * be printed directly (second halves of double blocks, piston heads).
     */
    private static boolean shouldSkipPlacement(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockDoor) {
            return state.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER;
        }
        if (block instanceof BlockBed) {
            return state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;
        }
        return block instanceof BlockPistonExtension;
    }

    /**
     * Translate of the reference getCannonAngles: yaw/pitch the barrel needs to
     * face the given target. Falls back to the placement yaw when idle.
     */
    public double[] getAimAngles(BlockPos target) {
        if (target == null) return new double[]{this.defaultYaw, 40.0};
        Vec3d diff = new Vec3d(target.getX() - this.pos.getX(), target.getY() - this.pos.getY(), target.getZ() - this.pos.getZ());
        double yaw = MathHelper.atan2(diff.x, diff.z) / Math.PI * 180.0;
        double distance = MathHelper.sqrt(diff.x * diff.x + diff.z * diff.z);
        double yOffset = distance * 2.0;
        double pitch = MathHelper.atan2(distance, diff.y * 3.0 + yOffset) / Math.PI * 180.0 + 10.0;
        return new double[]{yaw, pitch};
    }

    private void pullGunpowder() {
        for (EnumFacing f : EnumFacing.VALUES) {
            TileEntity te = this.world.getTileEntity(this.pos.offset(f));
            if (te == null || !te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, f.getOpposite())) continue;
            IItemHandler h = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, f.getOpposite());
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.getItem() == Items.GUNPOWDER && !s.isEmpty()) {
                    ItemStack taken = h.extractItem(i, Math.min(64, s.getCount()), false);
                    this.gunpowder += taken.getCount() * SHOTS_PER_GUNPOWDER;
                    return;
                }
            }
        }
    }

    private boolean consumeMaterial(ItemStack need) {
        // Creative-style infinite when no adjacent inventory has it? No —
        // official stalls. Search adjacent inventories first.
        List<IItemHandler> handlers = new ArrayList<>();
        for (EnumFacing f : EnumFacing.VALUES) {
            TileEntity te = this.world.getTileEntity(this.pos.offset(f));
            if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, f.getOpposite())) {
                handlers.add(te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, f.getOpposite()));
            }
        }
        if (handlers.isEmpty()) return false;
        // Simulate first.
        int needCount = 1;
        Map<IItemHandler, Map<Integer, Integer>> plan = new HashMap<>();
        for (IItemHandler h : handlers) {
            for (int i = 0; i < h.getSlots() && needCount > 0; i++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.isEmpty()) continue;
                if (s.getItem() != need.getItem() || s.getMetadata() != need.getMetadata()) continue;
                int take = Math.min(needCount, s.getCount());
                plan.computeIfAbsent(h, k -> new HashMap<>()).put(i, take);
                needCount -= take;
            }
        }
        if (needCount > 0) return false;
        for (Map.Entry<IItemHandler, Map<Integer, Integer>> en : plan.entrySet()) {
            for (Map.Entry<Integer, Integer> slot : en.getValue().entrySet()) {
                en.getKey().extractItem(slot.getKey(), slot.getValue(), false);
            }
        }
        return true;
    }

    public boolean onSneakUse(ItemStack stack, BlockPos anchorPos) {
        if (stack.getItem() instanceof ItemSchematic) {
            return this.loadSchematic(stack, anchorPos);
        }
        if (stack.getItem() == net.minecraft.init.Items.PAPER) {
            return false;
        }
        return false;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (this.schematic != null) nbt.setTag("Schematic", this.schematic);
        if (this.anchor != null) nbt.setLong("Anchor", this.anchor.toLong());
        nbt.setInteger("cursor", this.cursor);
        nbt.setBoolean("running", this.running);
        nbt.setInteger("gunpowder", this.gunpowder);
        nbt.setString("status", this.statusCache);
        nbt.setInteger("cooldown", this.printerCooldown);
        nbt.setFloat("defaultYaw", this.defaultYaw);
        if (this.aimTarget != null) nbt.setLong("aimTarget", this.aimTarget.toLong());
        if (this.lastShotTarget != null) nbt.setLong("lastShotTarget", this.lastShotTarget.toLong());
        nbt.setLong("lastShotTime", this.lastShotTime);
        nbt.setInteger("lastShotBlock", this.lastShotBlockId);
        nbt.setInteger("lastShotMeta", this.lastShotMeta);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("Schematic", 10)) this.schematic = nbt.getCompoundTag("Schematic");
        if (nbt.hasKey("Anchor")) this.anchor = BlockPos.fromLong(nbt.getLong("Anchor"));
        this.cursor = nbt.getInteger("cursor");
        this.running = nbt.getBoolean("running");
        this.gunpowder = nbt.getInteger("gunpowder");
        this.statusCache = nbt.getString("status");
        this.printerCooldown = nbt.getInteger("cooldown");
        this.defaultYaw = nbt.getFloat("defaultYaw");
        if (nbt.hasKey("aimTarget")) this.aimTarget = BlockPos.fromLong(nbt.getLong("aimTarget"));
        if (nbt.hasKey("lastShotTarget")) this.lastShotTarget = BlockPos.fromLong(nbt.getLong("lastShotTarget"));
        this.lastShotTime = nbt.getLong("lastShotTime");
        this.lastShotBlockId = nbt.getInteger("lastShotBlock");
        this.lastShotMeta = nbt.getInteger("lastShotMeta");
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("running", this.running);
        nbt.setString("status", this.statusCache);
        nbt.setInteger("cursor", this.cursor);
        nbt.setFloat("defaultYaw", this.defaultYaw);
        if (this.aimTarget != null) nbt.setLong("aimTarget", this.aimTarget.toLong());
        if (this.lastShotTarget != null) nbt.setLong("lastShotTarget", this.lastShotTarget.toLong());
        nbt.setLong("lastShotTime", this.lastShotTime);
        nbt.setInteger("lastShotBlock", this.lastShotBlockId);
        nbt.setInteger("lastShotMeta", this.lastShotMeta);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.running = nbt.getBoolean("running");
        this.statusCache = nbt.getString("status");
        this.cursor = nbt.getInteger("cursor");
        this.defaultYaw = nbt.getFloat("defaultYaw");
        if (nbt.hasKey("aimTarget")) this.aimTarget = BlockPos.fromLong(nbt.getLong("aimTarget"));
        else this.aimTarget = null;
        if (nbt.hasKey("lastShotTarget")) this.lastShotTarget = BlockPos.fromLong(nbt.getLong("lastShotTarget"));
        else this.lastShotTarget = null;
        this.lastShotTime = nbt.getLong("lastShotTime");
        this.lastShotBlockId = nbt.getInteger("lastShotBlock");
        this.lastShotMeta = nbt.getInteger("lastShotMeta");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        ByteBuf temp = Unpooled.buffer();
        ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(ByteBufUtils.readTag(buf));
    }

    @Override
    protected net.minecraft.util.math.AxisAlignedBB createRenderBoundingBox() {
        return com.melonstudios.melonlib.misc.AABB.wrap(this.pos, 16);
    }

    @Nullable
    public String getClipboardText() {
        if (this.schematic == null) return null;
        NBTTagList blocks = this.schematic.getTagList("Blocks", 10);
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < blocks.tagCount(); i++) {
            String b = blocks.getCompoundTagAt(i).getString("Block");
            counts.put(b, counts.getOrDefault(b, 0) + 1);
        }
        StringBuilder sb = new StringBuilder("Schematic materials:\n");
        List<String> keys = new ArrayList<>(counts.keySet());
        java.util.Collections.sort(keys);
        for (String k : keys) sb.append(counts.get(k)).append("x ").append(k).append("\n");
        return sb.toString();
    }
}
