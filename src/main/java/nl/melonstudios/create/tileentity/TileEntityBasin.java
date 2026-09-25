package nl.melonstudios.create.tileentity;

import com.google.common.collect.ImmutableList;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.capability.fluid.FluidHandlerBasin;
import nl.melonstudios.create.recipe.MixingRecipe;
import nl.melonstudios.create.tileentity.marker.ITileEntityWithSubInteractions;
import nl.melonstudios.create.tileentity.marker.ITopOpenInventory;
import nl.melonstudios.create.util.SubInteractionBox;
import nl.melonstudios.create.util.filter.IItemFilter;
import nl.melonstudios.create.util.filter.ItemFilterExact;
import nl.melonstudios.create.util.interfaces.IExcludeAttachingCapabilities;
import nl.melonstudios.create.util.interfaces.IHeatProvider;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class TileEntityBasin extends TileEntityOptimizedBase implements ITileEntityWithSubInteractions, ITopOpenInventory, IItemHandler {
    public final FluidHandlerBasin fluid = new FluidHandlerBasin();
    public final NonNullList<ItemStack> inventory = NonNullList.create();
    public final NonNullList<FluidStack> fluidQueue = NonNullList.create();
    public final NonNullList<ItemStack> inventoryQueue = NonNullList.create();
    public IItemFilter recipeFilter = null;

    public TileEntityBasin() {
        this.setTickRateLazy(Integer.MAX_VALUE);

        this.subInteractionBoxes = this.flag() ? null : ImmutableList.of(
                SubInteractionBox.Helper.createDefaultAt(0.0F, 0.75F, 0.5F, this::setRecipeFilter),
                SubInteractionBox.Helper.createDefaultAt(1.0F, 0.75F, 0.5F, this::setRecipeFilter),
                SubInteractionBox.Helper.createDefaultAt(0.5F, 0.75F, 1.0F, this::setRecipeFilter),
                SubInteractionBox.Helper.createDefaultAt(0.5F, 0.75F, 0.0F, this::setRecipeFilter)
        );
    }

    protected boolean flag() {
        return false;
    }
    private static class ForTesting extends TileEntityBasin implements IExcludeAttachingCapabilities {
        private ForTesting() {
            super();
        }

        @Override
        protected boolean flag() {
            return true;
        }
    }
    public TileEntityBasin copyForTesting() {
        if (this.flag()) throw new IllegalStateException("Cannot copy for testing a copied basin");
        NBTTagCompound nbt = this.writePacket();
        TileEntityBasin te = new ForTesting();
        te.readPacket(nbt);
        return te;
    }

    public boolean hasAnyFluid() {
        for (IFluidTankProperties properties : this.fluid.getTankProperties()) {
            FluidStack contents = properties.getContents();
            if (contents != null && contents.amount > 0) return true;
        }
        return false;
    }
    public boolean setRecipeFilter(@Nullable EntityPlayer player, boolean sneaking, ItemStack held) {
        if (held.isEmpty()) {
            this.recipeFilter = null;
        } else {
            this.recipeFilter = new ItemFilterExact(held);
        }
        this.sync();
        if (player != null) {
            if (held.isEmpty()) {
                player.playSound(SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM, 1.0F, 1.0F);
                player.sendStatusMessage(new TextComponentString("Cleared recipe filter"), true);
            } else {
                player.playSound(SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, 1.0F, 1.0F);
                player.sendStatusMessage(new TextComponentString("Set recipe filter to " + held.getDisplayName()), true);
            }
        }
        return true;
    }

    @Override
    public void tick() {
        this.itemRotationOld = this.itemRotation;
        this.itemRotation += this.addedItemRotation;
        this.addedItemRotation = 0;

        // Lazy intake runs here because this basin disables lazy ticks.
        // Collects loose items dropped in from above (belts, chutes, players);
        // funnels and chutes push through tryInsertItem / the item capability.
        if (!this.world.isRemote && (this.world.getTotalWorldTime() % 10 == 0)) {
            List<net.minecraft.entity.item.EntityItem> items = this.world.getEntitiesWithinAABB(
                    net.minecraft.entity.item.EntityItem.class,
                    new net.minecraft.util.math.AxisAlignedBB(this.pos.up()),
                    entityItem -> entityItem.isEntityAlive() && !entityItem.getItem().isEmpty());
            for (net.minecraft.entity.item.EntityItem entityItem : items) {
                ItemStack stack = entityItem.getItem();
                int before = stack.getCount();
                ItemStack over = this.tryInsertItem(stack);
                if (over.isEmpty()) entityItem.setDead();
                else if (over.getCount() != before) entityItem.setItem(over);
            }
        }
    }

    @Override
    public void tickLazy() {

    }

    private final BlockPos.MutableBlockPos heaterPos = new BlockPos.MutableBlockPos();
    public int getHeat() {
        this.heaterPos.setPos(this.pos).move(EnumFacing.DOWN);
        IBlockState state = this.world.getBlockState(this.heaterPos);
        if (state.getBlock() instanceof IHeatProvider) {
            return ((IHeatProvider)state.getBlock()).getHeat(this.world, this.heaterPos, state);
        }
        return 0;
    }

    public int itemRotation = 0;
    public int itemRotationOld = 0;
    public int addedItemRotation = 0;

    public void cleanupFluids() {
        this.fluid.optimize();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        this.optimizeInventory();
        super.writeToNBT(nbt);

        this.fluid.writeToNBT(nbt);

        if (!this.inventory.isEmpty()) {
            NBTTagList list = new NBTTagList();
            for (ItemStack stack : this.inventory) {
                list.appendTag(stack.writeToNBT(new NBTTagCompound()));
            }
            nbt.setTag("Inventory", list);
        }

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        this.fluid.readFromNBT(nbt);

        this.inventory.clear();
        if (nbt.hasKey("Inventory", 9)) {
            NBTTagList list = nbt.getTagList("Inventory", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                this.inventory.add(new ItemStack(list.getCompoundTagAt(i)));
            }
        }
        this.optimizeInventory();
    }

    @Override
    public void writePacket(TrackedByteBuf buf) {
        ByteBuf temp = Unpooled.buffer();
        ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @Override
    public void readPacket(ByteBuf buf) {
        this.readPacket(ByteBufUtils.readTag(buf));
    }

    @Override
    public NBTTagCompound writePacket() {
        this.optimizeInventory();
        NBTTagCompound nbt = new NBTTagCompound();

        this.fluid.writeToNBT(nbt);

        if (!this.inventory.isEmpty()) {
            NBTTagList list = new NBTTagList();
            for (ItemStack stack : this.inventory) {
                list.appendTag(stack.writeToNBT(new NBTTagCompound()));
            }
            nbt.setTag("Inventory", list);
        }

        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.fluid.readFromNBT(nbt);

        this.inventory.clear();
        if (nbt.hasKey("Inventory", 9)) {
            NBTTagList list = nbt.getTagList("Inventory", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                this.inventory.add(new ItemStack(list.getCompoundTagAt(i)));
            }
        }
        this.optimizeInventory();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return (T)this.fluid;
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return (T)this;
        return super.getCapability(capability, facing);
    }

    public void optimizeInventory() {
        this.inventory.removeIf(ItemStack::isEmpty);
        this.cleanupFluids();
    }

    @Override
    public void destroy() {
        StackUtil.dropItemsAt(this.world, this.pos, this.inventory.toArray(new ItemStack[0]));
    }

    private final List<SubInteractionBox> subInteractionBoxes;
    @Override
    public Collection<SubInteractionBox> getSubInteractionBoxes() {
        return this.subInteractionBoxes;
    }

    @Override
    public ItemStack tryInsertItem(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        this.sync();
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack prev = this.inventory.get(i);
            if (prev.isEmpty()) {
                this.inventory.set(i, stack.splitStack(64));
                return stack;
            }
            if (ItemStack.areItemsEqual(prev, stack) && ItemStack.areItemStackTagsEqual(prev, stack)) {
                int space = Math.min(prev.getMaxStackSize(), 64) - prev.getCount();
                if (space > 0) {
                    int move = Math.min(space, stack.getCount());
                    prev.grow(move);
                    stack.shrink(move);
                }
            }
            if (stack.isEmpty()) return ItemStack.EMPTY;
        }
        this.inventory.add(stack.splitStack(64));
        return stack;
    }

    @Override
    public boolean isInsertionSlotEmpty(ItemStack stack) {
        return stack.getCount() <= 64 &&
                this.inventory.stream().noneMatch(item ->
                        (ItemStack.areItemsEqual(item, stack) && ItemStack.areItemStackTagsEqual(item, stack))
                );
    }

    @Override
    public int getSlots() {
        return this.inventory.size()+1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == this.inventory.size() ? ItemStack.EMPTY : this.inventory.get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        if (slot == this.inventory.size()) {
            if (this.mayInsertNewItem(copy)) {
                if (simulate) {
                    copy.splitStack(this.getSlotLimit(slot));
                    return copy;
                }
                this.inventory.add(copy.splitStack(this.getSlotLimit(slot)));
                this.sync();
            }
            return copy;
        }
        ItemStack prev = this.inventory.get(slot);
        if (ItemStack.areItemsEqual(prev, copy) && ItemStack.areItemStackTagsEqual(prev, copy)) {
            int space = Math.min(prev.getMaxStackSize(), this.getSlotLimit(slot)) - prev.getCount();
            if (space > 0) {
                ItemStack ret = copy.splitStack(space);
                if (simulate) {
                    return copy;
                }
                prev.grow(ret.getCount());
                this.sync();
                return copy;
            }
            return copy;
        }
        // Occupied by a different ingredient: behave like the top-open insert
        // and append a new stack instead of rejecting (reference input
        // inventory accepts mixed ingredients across its 9 slots).
        if (this.mayInsertNewItem(copy)) {
            if (simulate) {
                copy.splitStack(this.getSlotLimit(slot));
                return copy;
            }
            this.inventory.add(copy.splitStack(this.getSlotLimit(slot)));
            this.sync();
        }
        return copy;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0 || slot == this.inventory.size()) return ItemStack.EMPTY;
        ItemStack prev = this.inventory.get(slot);
        if (simulate) {
            return prev.copy().splitStack(amount);
        }
        this.sync();
        return prev.splitStack(amount);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    private boolean mayInsertNewItem(ItemStack stack) {
        for (ItemStack prev : this.inventory) {
            if (ItemStack.areItemsEqual(prev, stack) && ItemStack.areItemStackTagsEqual(prev, stack)) {
                return false;
            }
        }
        return true;
    }

    public void dumpRecipeResults(MixingRecipe recipe) {
        for (ItemStack stack : recipe.itemOutputs) {
            this.tryInsertItem(stack.copy());
        }
        for (FluidStack stack : recipe.fluidOutputs) {
            this.fluid.fill(stack, true);
        }
    }
}
