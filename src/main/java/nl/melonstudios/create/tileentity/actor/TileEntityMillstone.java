package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.recipe.PulverizationRecipe;
import nl.melonstudios.create.recipe.server.MillingRecipes;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.tileentity.marker.ITopOpenInventory;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class TileEntityMillstone extends TileEntityKinetic implements IItemHandler, ITopOpenInventory {
    public ItemStack input = ItemStack.EMPTY;
    public final ItemStack[] output = new ItemStack[] {
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
    };

    public int timer;
    private PulverizationRecipe lastMillingRecipe = null;

    @Override
    public void tick() {
        super.tick();

        if (this.getSpeed() == 0) return;
        for (ItemStack stack : this.output) {
            if (stack.getCount() < stack.getMaxStackSize()) continue;
            return;
        }

        if (this.timer > 0) {
            this.timer -= this.getProcessingSpeed();

            if (this.world.isRemote) {
                this.spawnParticles();
                return;
            }
            if (this.timer <= 0) {
                this.process();
            }
            this.markDirty();
            return;
        }

        if (this.input.isEmpty()) return;

        if (this.lastMillingRecipe == null || !this.lastMillingRecipe.input.test(this.input)) {
            PulverizationRecipe recipe = MillingRecipes.instance.getRecipeForInput(this.input);
            if (recipe != null) {
                this.lastMillingRecipe = recipe;
                this.timer = this.lastMillingRecipe.processingTime;
                this.sync();
            } else {
                this.timer = 100;
                this.sync();
            }
            return;
        }

        this.timer = this.lastMillingRecipe.processingTime;
        this.sync();
    }

    public int getProcessingSpeed() {
        return MathHelper.clamp((int)Math.abs(this.getSpeed() / 16.0F), 1, 512);
    }
    public void spawnParticles() {
        if (this.input.isEmpty()) return;

        if (this.world.getTotalWorldTime() % 5 == 0) {
            CreateLegacy.proxy.millstoneFX(this);
        }
    }
    private void process() {
        if (this.lastMillingRecipe == null || !this.lastMillingRecipe.input.test(this.input)) {
            PulverizationRecipe recipe = MillingRecipes.instance.getRecipeForInput(this.input);
            if (recipe == null) return;
            this.lastMillingRecipe = recipe;
        }

        this.input.shrink(1);
        stacks:
        for (ItemStack stack : Utils.rollChancedResults(this.lastMillingRecipe.results)) {
            if (stack.isEmpty()) continue;
            for (int i = 0; i < this.output.length; i++) {
                if (this.output[i].isEmpty()) {
                    this.output[i] = stack;
                    continue stacks;
                }
                if (ItemStack.areItemsEqual(this.output[i], stack) && ItemStack.areItemStackTagsEqual(this.output[i], stack)) {
                    int space = this.output[i].getMaxStackSize() - this.output[i].getCount();
                    if (space <= 0) continue;
                    space = Math.min(space, stack.getCount());
                    this.output[i].grow(space);
                    stack.shrink(space);
                    if (stack.isEmpty()) continue stacks;
                }
            }
        }
        this.sync();
    }

    @Override
    public void destroy() {
        super.destroy();
        StackUtil.dropItemsAt(this.world, this.pos, this.input);
        StackUtil.dropItemsAt(this.world, this.pos, this.output);
    }

    @Override
    public void tickLazy() {
        super.tickLazy();
        // Reference DirectBeltInputBehaviour covers belts/funnels/chutes; loose
        // items dropped onto the millstone from above are collected here.
        if (this.world.isRemote) return;
        List<EntityItem> items = this.world.getEntitiesWithinAABB(EntityItem.class,
                new AxisAlignedBB(this.pos.up()),
                entityItem -> entityItem.isEntityAlive() && !entityItem.getItem().isEmpty());
        for (EntityItem entityItem : items) {
            ItemStack stack = entityItem.getItem();
            ItemStack over = this.tryInsertItem(stack);
            if (over.isEmpty()) entityItem.setDead();
            else if (over.getCount() != stack.getCount()) entityItem.setItem(over);
            if (!this.input.isEmpty() && this.input.getCount() >= 64) break;
        }
    }

    private boolean canMill(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (this.lastMillingRecipe != null && this.lastMillingRecipe.input.test(stack)) return true;
        return MillingRecipes.instance.getRecipeForInput(stack) != null;
    }

    @Override
    public ItemStack tryInsertItem(ItemStack stack) {
        if (stack.isEmpty() || !this.canMill(stack)) return stack;
        if (this.input.isEmpty()) {
            int take = Math.min(stack.getCount(), 64);
            this.input = stack.copy();
            this.input.setCount(take);
            stack.shrink(take);
            this.sync();
            return stack.isEmpty() ? ItemStack.EMPTY : stack;
        }
        if (ItemStack.areItemsEqual(this.input, stack) && ItemStack.areItemStackTagsEqual(this.input, stack)) {
            int space = 64 - this.input.getCount();
            if (space <= 0) return stack;
            int move = Math.min(space, stack.getCount());
            this.input.grow(move);
            stack.shrink(move);
            this.sync();
        }
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    @Override
    public boolean isInsertionSlotEmpty(ItemStack stack) {
        if (stack.isEmpty()) return this.input.isEmpty();
        if (!this.canMill(stack)) return false;
        if (this.input.isEmpty()) return true;
        return ItemStack.areItemsEqual(this.input, stack)
                && ItemStack.areItemStackTagsEqual(this.input, stack)
                && this.input.getCount() < 64;
    }

    @Override
    public ItemStack tryInsertItem(ItemStack stack, @Nullable EnumFacing side) {
        return this.tryInsertItem(stack);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound nbt = super.writeToNBT(compound);

        if (!this.input.isEmpty()) {
            nbt.setTag("Input", this.input.writeToNBT(new NBTTagCompound()));
        }
        for (int i = 0; i < this.output.length; i++) {
            ItemStack out = this.output[i];
            if (!out.isEmpty()) {
                nbt.setTag("Output_" + i, out.writeToNBT(new NBTTagCompound()));
            }
        }
        nbt.setInteger("timer", this.timer);

        return nbt;
    }
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        if (compound.hasKey("Input", 10)) {
            this.input = new ItemStack(compound.getCompoundTag("Input"));
        } else this.input = ItemStack.EMPTY;
        for (int i = 0; i < this.output.length; i++) {
            if (compound.hasKey("Output_" + i, 10)) {
                this.output[i] = new ItemStack(compound.getCompoundTag("Output_" + i));
            }
        }
        this.timer = compound.getInteger("timer");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);

        if (!this.input.isEmpty()) {
            buf.writeBoolean(true);
            StackUtil.writeItemStack(this.input, buf, true, true);
        } else buf.writeBoolean(false);
        for (int i = 0; i < this.output.length; i++) {
            if (!this.output[i].isEmpty()) {
                buf.writeBoolean(true);
                StackUtil.writeItemStack(this.output[i], buf, true, true);
            } else buf.writeBoolean(false);
        }
        buf.writeInt(this.timer);
    }
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);

        if (buf.readBoolean()) {
            this.input = StackUtil.readItemStack(buf, true, true);
        } else this.input = ItemStack.EMPTY;
        for (int i = 0; i < this.output.length; i++) {
            if (buf.readBoolean()) {
                this.output[i] = StackUtil.readItemStack(buf, true, true);
            } else this.output[i] = ItemStack.EMPTY;
        }
        this.timer = buf.readInt();
    }

    @Override
    public int getSlots() {
        return this.output.length + 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? this.input : this.output[slot-1];
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0 || stack.isEmpty() || !this.canMill(stack)) return stack;
        if (!this.input.isEmpty()
                && !(ItemStack.areItemsEqual(this.input, stack) && ItemStack.areItemStackTagsEqual(this.input, stack))) {
            return stack;
        }
        int present = this.input.isEmpty() ? 0 : this.input.getCount();
        int space = 64 - present;
        if (space <= 0) return stack;
        int move = Math.min(space, stack.getCount());
        if (simulate) {
            ItemStack ret = stack.copy();
            ret.shrink(move);
            return ret.isEmpty() ? ItemStack.EMPTY : ret;
        }
        if (this.input.isEmpty()) {
            this.input = stack.copy();
            this.input.setCount(move);
        } else {
            this.input.grow(move);
        }
        this.sync();
        if (move == stack.getCount()) return ItemStack.EMPTY;
        ItemStack ret = stack.copy();
        ret.shrink(move);
        return ret;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot == 0 || amount == 0) return ItemStack.EMPTY;
        ItemStack prev = simulate ? this.output[slot-1].copy() : this.output[slot-1];
        if (!simulate) this.sync();
        return prev.splitStack(amount);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && MillingRecipes.instance.getRecipeForInput(stack) != null;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T)this;
        return super.getCapability(capability, facing);
    }
}
