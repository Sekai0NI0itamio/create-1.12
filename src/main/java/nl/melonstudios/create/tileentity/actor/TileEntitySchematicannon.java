package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.item.ItemSchematic;
import nl.melonstudios.create.tileentity.TileEntityKinetic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;

/**
 * Schematicannon: loads a finished schematic from the player's hand
 * (right-click handled here via first-tick hand scan is skipped — instead
 * sneak-right-click the cannon with the schematic to load), consumes
 * gunpowder (1 per 8 blocks) + materials from adjacent inventories, places
 * up to N blocks per tick while running.
 */
public class TileEntitySchematicannon extends TileEntityKinetic {
    public NBTTagCompound schematic;
    public BlockPos anchor;
    public int cursor;
    public boolean running;
    public int gunpowder;
    public String statusCache = "Idle";

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
        this.schematic = stack.getTagCompound().copy();
        this.anchor = anchor;
        this.cursor = 0;
        this.statusCache = "Ready";
        this.sync();
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.world.isRemote) return;
        if (!this.running || this.schematic == null || this.getSpeed() == 0) return;
        if (this.gunpowder <= 0) {
            this.pullGunpowder();
            if (this.gunpowder <= 0) {
                this.statusCache = "Out of Gunpowder";
                return;
            }
        }
        NBTTagList blocks = this.schematic.getTagList("Blocks", 10);
        int[] size = this.schematic.getIntArray("Size");
        BlockPos origin = BlockPos.fromLong(this.schematic.getLong("Origin"));
        int placed = 0;
        while (this.cursor < blocks.tagCount() && placed < 4) {
            NBTTagCompound e = blocks.getCompoundTagAt(this.cursor++);
            int[] rel = e.getIntArray("Pos");
            BlockPos target = this.anchor.add(rel[0], rel[1], rel[2]);
            if (!this.world.isBlockLoaded(target)) {
                this.statusCache = "Block is not loaded";
                return;
            }
            Block block = Block.getBlockFromName(e.getString("Block"));
            if (block == null) {
                placed++;
                continue;
            }
            @SuppressWarnings("deprecation")
            IBlockState want = block.getStateFromMeta(e.getInteger("Meta"));
            IBlockState cur = this.world.getBlockState(target);
            if (cur.getBlock() == block && cur.getBlock().getMetaFromState(cur) == e.getInteger("Meta")) {
                placed++;
                continue;
            }
            ItemStack need = new ItemStack(block, 1, e.getInteger("Meta"));
            if (!this.consumeMaterial(need)) {
                this.statusCache = "Missing Block";
                this.cursor--;
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
            placed++;
            if (this.cursor % 8 == 0 && this.gunpowder > 0) this.gunpowder--;
        }
        if (this.cursor >= blocks.tagCount()) {
            this.running = false;
            this.cursor = 0;
            this.statusCache = "Done";
            this.sync();
            return;
        }
        this.statusCache = "Running " + this.cursor + "/" + blocks.tagCount();
        if (this.world.getTotalWorldTime() % 20 == 0) this.sync();
        this.markDirty();
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
                    this.gunpowder += taken.getCount() * 8;
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
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("running", this.running);
        nbt.setString("status", this.statusCache);
        nbt.setInteger("cursor", this.cursor);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.running = nbt.getBoolean("running");
        this.statusCache = nbt.getString("status");
        this.cursor = nbt.getInteger("cursor");
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
        return com.melonstudios.melonlib.misc.AABB.wrap(this.pos, 2);
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
