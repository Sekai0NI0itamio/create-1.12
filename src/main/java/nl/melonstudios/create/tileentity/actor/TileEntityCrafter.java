package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.block.state.EnumDirection;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.util.CrafterContext;
import nl.melonstudios.create.util.InventoryCrafter;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TileEntityCrafter extends TileEntityKinetic implements IItemHandler {
    public static final boolean SKIP_ANIMATION = true;

    public TileEntityCrafter() {

    }

    public CrafterContext crafterContext = null;
    public ItemStack containedItem = ItemStack.EMPTY;
    private boolean wasPoweredBefore = true;

    public EnumFacing getFacing() {
        return EnumFacing.HORIZONTALS[this.getBlockMetadata() & 3];
    }
    public EnumDirection getDirection() {
        return EnumDirection.byId((this.getBlockMetadata() >> 2) & 3);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.world.isRemote && this.crafterContext == null) {
            boolean powered = this.world.isBlockPowered(this.pos);
            if (powered && !this.wasPoweredBefore) {
                this.startCraftingIfReady(true);
            }
            this.wasPoweredBefore = powered;
        }

        //TODO: fix
        if (this.crafterContext != null) {
            if (this.crafterContext.currentPattern != null) {
                this.crafterContext.addProgress(Math.abs(this.getSpeed()) * 0.015625F);
                if (this.crafterContext.progress >= 1.0F && !this.world.isRemote) {
                    TileEntityCrafter pointer = this.getPointerCrafter();
                    if (pointer != null) {
                        pointer.acceptContextPattern(this);
                        this.world.playSound(null, this.pos, SoundInit.crafter_click, SoundCategory.BLOCKS, 1.0F,
                                0.5F + this.crafterContext.crafterPositions.size() / 16.0F);
                    } else {
                        List<TileEntityCrafter> crafters = this.crafterContext.crafterPositions.stream()
                                .map(this.world::getTileEntity)
                                .map(te -> Utils.cast(te, TileEntityCrafter.class))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        InventoryCrafter inventoryCrafter = new InventoryCrafter(convertToGrid(
                                crafters, this.getFacing()
                        ));
                        IRecipe recipe = CraftingManager.findMatchingRecipe(inventoryCrafter, this.world);
                        if (recipe != null) {
                            ItemStack result = recipe.getCraftingResult(inventoryCrafter);
                            this.world.playSound(null, this.pos, SoundInit.crafter_click, SoundCategory.BLOCKS, 1.0F, 2.0F);
                            this.world.playSound(null, this.pos, SoundInit.crafter_craft, SoundCategory.BLOCKS, 1.0F, 1.0F);
                            List<ItemStack> containers = new ArrayList<>();
                            for (TileEntityCrafter crafter : crafters) {
                                if (crafter.containedItem.getItem() != ItemInit.CRAFTER_COVER) {
                                    if (!crafter.containedItem.isEmpty()) {
                                        ItemStack container = crafter.containedItem.getItem().getContainerItem(crafter.containedItem);
                                        if (!container.isEmpty()) containers.add(container);
                                        crafter.containedItem = ItemStack.EMPTY;
                                    }
                                }
                                crafter.sync();
                            }
                            BlockPos pointerPos = this.getPointerPos();
                            TileEntity te = this.world.getTileEntity(pointerPos);
                            if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.getPointerFacing().getOpposite())) {
                                IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.getPointerFacing().getOpposite());
                                if (handler != null) {
                                    for (int i = 0; i < handler.getSlots(); i++) {
                                        if (!result.isEmpty()) {
                                            result = handler.insertItem(i, result, false);
                                        }
                                        for (int j = 0; j < containers.size(); j++) {
                                            ItemStack stack = containers.get(j);
                                            if (!stack.isEmpty()) {
                                                containers.set(j, handler.insertItem(i, stack, false));
                                            }
                                        }
                                    }
                                }
                            }
                            if (!result.isEmpty()) {
                                StackUtil.dropItemsAt(this.world, this.pos, result);
                            }
                            for (ItemStack container : containers) {
                                if (!container.isEmpty()) {
                                    StackUtil.dropItemsAt(this.world, this.pos, container);
                                }
                            }
                        } else {
                            List<ItemStack> waste = new ArrayList<>();
                            for (TileEntityCrafter crafter : crafters) {
                                if (!crafter.containedItem.isEmpty()) {
                                    if (crafter.containedItem.getItem() != ItemInit.CRAFTER_COVER) {
                                        waste.add(crafter.containedItem.copy());
                                        crafter.containedItem = ItemStack.EMPTY;
                                    }
                                }
                                crafter.sync();
                            }
                            for (ItemStack stack : waste) {
                                if (!stack.isEmpty()) {
                                    StackUtil.dropItemsAt(this.world, this.pos, stack);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private TileEntityCrafter cachedPointerCrafter = null;
    @Nullable
    public TileEntityCrafter getPointerCrafter() {
        if (this.cachedPointerCrafter == null || this.cachedPointerCrafter.isInvalid()) {
            BlockPos offPos = this.getPointerPos();
            if (this.crafterContext != null && !this.crafterContext.crafterPositions.contains(offPos)) return null;
            this.cachedPointerCrafter = Utils.cast(this.world.getTileEntity(offPos), TileEntityCrafter.class);
        }
        return this.cachedPointerCrafter;
    }
    private BlockPos cachedPointerPos = null;
    public BlockPos getPointerPos() {
        if (this.cachedPointerPos == null) {
            EnumFacing offset = this.getPointerFacing();
            this.cachedPointerPos = this.pos.offset(offset);
        }
        return this.cachedPointerPos;
    }
    public EnumFacing getPointerFacing() {
        return this.getDirection().getRelative(this.getFacing());
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();

        this.cachedPointerCrafter = null;
        this.cachedPointerPos = null;
    }

    @Override
    public void invalidate() {
        super.invalidate();

        this.cachedPointerCrafter = null;
        this.cachedPointerPos = null;
    }

    @Override
    public void setPos(BlockPos posIn) {
        super.setPos(posIn);

        this.cachedPointerCrafter = null;
        this.cachedPointerPos = null;
    }

    @Override
    public void setWorld(World worldIn) {
        super.setWorld(worldIn);

        this.cachedPointerCrafter = null;
        this.cachedPointerPos = null;
    }

    @Override
    public void destroy() {
        StackUtil.dropItemsAt(this.world, this.pos, this.containedItem.copy());
        if (this.crafterContext != null) this.crafterContext.interrupt(this.world);
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.containedItem;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!this.containedItem.isEmpty()) return stack;
        ItemStack copy = stack.copy();
        ItemStack split = copy.splitStack(1);
        if (simulate) return copy;
        this.containedItem = split;
        this.startCraftingIfReady(false);
        this.sync();
        return copy;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        if (!this.containedItem.isEmpty()) {
            nbt.setTag("ContainedItem", this.containedItem.writeToNBT(new NBTTagCompound()));
        }
        if (this.crafterContext != null) {
            nbt.setTag("CrafterContext", this.crafterContext.serializeNBT());
        }

        return nbt;
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);

        if (!this.containedItem.isEmpty()) {
            buf.writeBoolean(true);
            StackUtil.writeItemStack(this.containedItem, buf, true, true);
        } else {
            buf.writeBoolean(false);
        }

        if (this.crafterContext != null) {
            buf.writeBoolean(true);
            this.crafterContext.serialize(buf);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        if (nbt.hasKey("ContainedItem", 10)) {
            this.containedItem = new ItemStack(nbt.getCompoundTag("ContainedItem"));
        } else this.containedItem = ItemStack.EMPTY;
        if (nbt.hasKey("CrafterContext")) {
            this.crafterContext = new CrafterContext();
            this.crafterContext.deserializeNBT(nbt.getCompoundTag("CrafterContext"));
        } else this.crafterContext = null;
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);

        if (buf.readBoolean()) {
            this.containedItem = StackUtil.readItemStack(buf, true, true);
        } else this.containedItem = ItemStack.EMPTY;

        if (buf.readBoolean()) {
            this.crafterContext = new CrafterContext();
            this.crafterContext.deserialize(buf);
        } else this.crafterContext = null;
    }

    public boolean shouldRenderItemInside() {
        if (this.containedItem.isEmpty() || this.containedItem.getItem() == ItemInit.CRAFTER_COVER) return false;
        return this.crafterContext == null || !this.crafterContext.passed;
    }

    public void acceptContextPattern(TileEntityCrafter other) {
        if (this.crafterContext == null) throw new IllegalStateException("Got crafter context but was not in the process of assembly?");
        EnumDirection shift = other.getDirection();
        this.crafterContext.currentPattern = new Int2ObjectArrayMap<>();
        for (Int2ObjectMap.Entry<ItemStack> entry : other.crafterContext.currentPattern.int2ObjectEntrySet()) {
            this.addShifted(this.crafterContext.currentPattern, entry.getIntKey(), entry.getValue(), shift);
        }
        if (!this.containedItem.isEmpty() && this.containedItem.getItem() != ItemInit.CRAFTER_COVER) {
            this.crafterContext.currentPattern.put(0, this.containedItem);
        }
        other.crafterContext.currentPattern = null;
        this.crafterContext.passed = true;
        this.sync();
        other.sync();
    }

    private void addShifted(Int2ObjectMap<ItemStack> map, int key, ItemStack value, EnumDirection shift) {
        switch (shift) {
            case UP:
                map.put(movePacked(key, 0, -1), value);
                break;
            case DOWN:
                map.put(movePacked(key, 0, 1), value);
                break;
            case LEFT:
                map.put(movePacked(key, 1, 0), value);
                break;
            case RIGHT:
                map.put(movePacked(key, -1, 0), value);
                break;
        }
    }
    private static int movePacked(int packed, int x, int y) {
        short a = (short) (packed & 0xFFFF);
        short b = (short) ((packed >> 16) & 0xFFFF);
        return ((a+x) & 0xFFFF) | (((b+y) << 16) & 0xFFFF);
    }

    public boolean isOccupied() {
        return this.containedItem.getItem() == ItemInit.CRAFTER_COVER || this.crafterContext != null;
    }
    public void getConnectedCrafters(Set<TileEntityCrafter> crafters, World world, BlockPos.MutableBlockPos mutable) {
        if (crafters.contains(this)) return;
        crafters.add(this);
        for (EnumFacing side : Utils.getSurrounding(this.getFacing().getAxis())) {
            mutable.setPos(this.pos).move(side);
            TileEntityCrafter other = Utils.cast(world.getTileEntity(mutable), TileEntityCrafter.class);
            if (other == null || crafters.contains(other)) continue;
            if (areCraftersConnected(this, other, side))
                other.getConnectedCrafters(crafters, world, mutable);
        }
    }
    public void startCraftingIfReady(boolean redstone) {
        if (this.getSpeed() == 0.0F) return;
        if (this.crafterContext != null) return;
        Set<TileEntityCrafter> crafters = new HashSet<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        this.getConnectedCrafters(crafters, this.world, pos);
        if (redstone) {
            this.startCrafting(crafters);
        } else {
            for (TileEntityCrafter crafter : crafters) {
                if (crafter.containedItem.isEmpty()) return;
            }
            this.startCrafting(crafters);
        }
    }
    public void startCrafting(Set<TileEntityCrafter> crafters) {
        for (TileEntityCrafter crafter : crafters) {
            crafter.crafterContext = new CrafterContext(crafters);
            if (crafter == this) {
                crafter.crafterContext.currentPattern = new Int2ObjectArrayMap<>();
                crafter.crafterContext.passed = true;
                crafter.crafterContext.currentPattern.put(0, crafter.containedItem);
            }
            crafter.sync();
        }
    }

    //Assuming they are right next to each other
    public static boolean areCraftersConnected(TileEntityCrafter one, TileEntityCrafter other, EnumFacing oneToOther) {
        EnumFacing facing = one.getFacing();
        if (facing != other.getFacing()) return false;
        EnumDirection assumed = EnumDirection.getRelativeMirror(facing, oneToOther);
        return one.getDirection() == assumed || other.getDirection() == assumed.getOpposite();
    }
    public static TileEntityCrafter[][] convertToGrid(Collection<TileEntityCrafter> crafters, EnumFacing facing) {
        if (crafters.isEmpty()) throw new IllegalArgumentException("Must have at least one crafter");
        if (facing.getAxis() == EnumFacing.Axis.Y) throw new IllegalArgumentException("Crafter facing must be horizontal");
        List<TileEntityCrafter> sorted = crafters instanceof List ? (List<TileEntityCrafter>) crafters: new ArrayList<>(crafters);
        if (sorted.size() == 1) return new TileEntityCrafter[][]{{sorted.get(0)}};
        sorted.sort(Comparators.LOOKUP[facing.getHorizontalIndex()]);
        TileEntityCrafter first = sorted.get(0);
        TileEntityCrafter last = sorted.get(sorted.size()-1);
        int w, h;
        TileEntityCrafter[][] array;
        switch (facing) {
            case NORTH:
                w = first.pos.getX() - last.pos.getX() + 1;
                h = last.pos.getY() - first.pos.getY() + 1;
                array = new TileEntityCrafter[w][h];
                break;
            case SOUTH:
                w = last.pos.getX() - first.pos.getX() + 1;
                h = last.pos.getY() - first.pos.getY() + 1;
                array = new TileEntityCrafter[w][h];
                break;
            case WEST:
                w = first.pos.getZ() - last.pos.getZ() + 1;
                h = last.pos.getY() - first.pos.getY() + 1;
                array = new TileEntityCrafter[w][h];
                break;
            case EAST:
                w = last.pos.getZ() - first.pos.getZ() + 1;
                h = last.pos.getY() - first.pos.getY() + 1;
                array = new TileEntityCrafter[w][h];
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + facing);
        }
        for (int i = 0; i < sorted.size(); i++) {
            array[i % w][i / w] = sorted.get(i);
        }
        return array;
    }

    public static class Comparators {
        private Comparators() {
            throw new AssertionError("no");
        }

        public static final Comparator<TileEntityCrafter>[] LOOKUP;
        public static final Comparator<TileEntityCrafter> NORTH = new North();
        public static final Comparator<TileEntityCrafter> SOUTH = new South();
        public static final Comparator<TileEntityCrafter> WEST = new West();
        public static final Comparator<TileEntityCrafter> EAST = new East();

        static {
            //noinspection unchecked
            LOOKUP = new Comparator[4];
            LOOKUP[0] = SOUTH;
            LOOKUP[1] = WEST;
            LOOKUP[2] = NORTH;
            LOOKUP[3] = EAST;
        }

        private static class North implements Comparator<TileEntityCrafter> {
            @Override
            public int compare(TileEntityCrafter o1, TileEntityCrafter o2) {
                if (o1 == o2 || o1.pos.equals(o2.pos)) {
                    return 0;
                }
                if (o1.pos.getY() < o2.pos.getY()) return -1;
                if (o1.pos.getX() > o2.pos.getX()) return -1;
                return 1;
            }
        }
        private static class South implements Comparator<TileEntityCrafter> {
            @Override
            public int compare(TileEntityCrafter o1, TileEntityCrafter o2) {
                if (o1 == o2 || o1.pos.equals(o2.pos)) {
                    return 0;
                }
                if (o1.pos.getY() < o2.pos.getY()) return -1;
                if (o1.pos.getX() < o2.pos.getX()) return -1;
                return 1;
            }
        }
        private static class West implements Comparator<TileEntityCrafter> {
            @Override
            public int compare(TileEntityCrafter o1, TileEntityCrafter o2) {
                if (o1 == o2 || o1.pos.equals(o2.pos)) {
                    return 0;
                }
                if (o1.pos.getY() < o2.pos.getY()) return -1;
                if (o1.pos.getZ() > o2.pos.getZ()) return -1;
                return 1;
            }
        }
        private static class East implements Comparator<TileEntityCrafter> {
            @Override
            public int compare(TileEntityCrafter o1, TileEntityCrafter o2) {
                if (o1 == o2 || o1.pos.equals(o2.pos)) {
                    return 0;
                }
                if (o1.pos.getY() < o2.pos.getY()) return -1;
                if (o1.pos.getZ() < o2.pos.getZ()) return -1;
                return 1;
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T) this;
        return super.getCapability(capability, facing);
    }
}
