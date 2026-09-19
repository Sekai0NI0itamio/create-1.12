package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.SoundType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.init.RecipeInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.recipe.CuttingRecipe;
import nl.melonstudios.create.recipe.server.CuttingRecipes;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.tileentity.marker.ITileEntityWithSubInteractions;
import nl.melonstudios.create.tileentity.marker.ITopOpenInventory;
import nl.melonstudios.create.util.SubInteractionBox;
import nl.melonstudios.create.util.filter.IItemFilter;
import nl.melonstudios.create.util.filter.ItemFilterExact;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TileEntitySawProcessing extends TileEntityKinetic implements ITileEntityWithSubInteractions, ITopOpenInventory, IItemHandler {
    public static void addSubInteractionsAlongX(TileEntitySawProcessing te) {
        te.subInteractionBoxes.add(SubInteractionBox.Helper.createDefaultAt(0.25F, 0.75F, 0.5F, te::setFilter));
        te.subInteractionBoxes.add(SubInteractionBox.Helper.createDefaultAt(0.75F, 0.75F, 0.5F, te::setFilter));
    }
    public static void addSubInteractionsAlongZ(TileEntitySawProcessing te) {
        te.subInteractionBoxes.add(SubInteractionBox.Helper.createDefaultAt(0.5F, 0.75F, 0.25F, te::setFilter));
        te.subInteractionBoxes.add(SubInteractionBox.Helper.createDefaultAt(0.5F, 0.75F, 0.75F, te::setFilter));
    }

    public TileEntitySawProcessing() {
        super();
        this.setTickRateLazy(10);
    }

    public ItemStack currentlyProcessing = ItemStack.EMPTY;
    public String currentRecipeID;
    public CuttingRecipe currentRecipe;
    public int progress;
    public int lastProgress;
    public ItemStack outputQueue = ItemStack.EMPTY;

    @Override
    public void initialize() {
        super.initialize();

        if (this.getBlockMetadata() == 4) addSubInteractionsAlongX(this);
        else addSubInteractionsAlongZ(this);
    }

    @Override
    public void initializeClient() {
        super.initializeClient();

        if (this.getBlockMetadata() == 4) addSubInteractionsAlongX(this);
        else addSubInteractionsAlongZ(this);
    }

    @Override
    public void tick() {
        super.tick();

        this.lastProgress = this.progress;
        if (!this.currentlyProcessing.isEmpty() && this.getSpeed() != 0.0F) {
            this.markDirty();
            this.progress += this.getProgressTick();
            EnumFacing side = this.getProcessingDirection();
            if (this.world.isRemote) {
                CreateLegacy.proxy.spawnItemFX(
                        this.pos.getX() + 0.5,
                        this.pos.getY() + 0.75,
                        this.pos.getZ() + 0.5,
                        -side.getFrontOffsetX() * (Math.abs(this.getSpeed()) * 0.0009765625),
                        0.05 + this.world.rand.nextDouble() * 0.1,
                        -side.getFrontOffsetZ() * (Math.abs(this.getSpeed()) * 0.0009765625),
                        this.currentlyProcessing
                );
            }
            if (this.currentRecipe != null) {
                if (this.progress >= this.currentRecipe.processingTime * this.currentlyProcessing.getCount()) {
                    int size = this.currentlyProcessing.getCount();
                    this.playCutSound(this.currentlyProcessing);
                    this.currentlyProcessing = ItemStack.EMPTY;
                    ItemStack result = this.currentRecipe.result.copy();
                    result.setCount(size * result.getCount());
                    this.outputQueue = result;
                    this.currentRecipeID = null;
                    this.currentRecipe = null;
                    this.progress = 0;
                }
            } else {
                if (this.progress >= this.getProgressTick() * 10) {
                    this.playCutSound(this.currentlyProcessing);
                    this.outputQueue = this.currentlyProcessing.copy();
                    this.currentlyProcessing = ItemStack.EMPTY;
                    this.currentRecipeID = null;
                    this.currentRecipe = null;
                    this.progress = 0;
                }
            }
        }
        if (!this.outputQueue.isEmpty() && this.getSpeed() != 0.0F) {
            this.pushResult();
        }
    }

    public int getProgressTick() {
        return Math.max(1, MathHelper.floor(Math.abs(this.getSpeed())  * 0.125F));
    }
    private void playCutSound(ItemStack input) {
        if (this.world.isRemote || input.isEmpty())
            return;
        boolean wood = false;
        if (input.getItem() instanceof ItemBlock) {
            wood = ((ItemBlock) input.getItem()).getBlock().getSoundType() == SoundType.WOOD;
        }
        this.world.playSound(null, this.pos,
                wood ? SoundInit.saw_activate_wood : SoundInit.saw_activate_stone,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
    private void pushResult() {
        if (this.world.isRemote) return;
        EnumFacing side = this.getProcessingDirection();
        BlockPos drop = this.pos.offset(side);
        if (this.world.getBlockState(drop).getBlock().isReplaceable(this.world, drop)) {
            EntityItem entity = new EntityItem(this.world,
                    this.pos.getX() + 0.5 + side.getFrontOffsetX() * 0.65,
                    this.pos.getY() + 0.75F,
                    this.pos.getZ() + 0.5 + side.getFrontOffsetZ() * 0.65,
                    this.outputQueue.copy()
            );
            entity.motionX = side.getFrontOffsetX() * 0.1;
            entity.motionZ = side.getFrontOffsetZ() * 0.1;
            entity.motionY = 0.05;
            entity.setDefaultPickupDelay();
            this.world.spawnEntity(entity);
            this.outputQueue = ItemStack.EMPTY;
            this.sync();
        } else {
            TileEntity te = this.world.getTileEntity(drop);
            if (te instanceof ITopOpenInventory) {
                this.outputQueue = ((ITopOpenInventory)te).tryInsertItem(this.outputQueue, side.getOpposite());
                this.sync();
            }
        }
    }
    public EnumFacing getProcessingDirection() {
        boolean x = this.getBlockMetadata() == 4;
        if (x) return this.getSpeed() > 0.0F ? EnumFacing.NORTH : EnumFacing.SOUTH;
        else return this.getSpeed() > 0.0F ? EnumFacing.EAST : EnumFacing.WEST;
    }

    @Override
    public void tickLazy() {
        super.tickLazy();

        if (this.currentlyProcessing.isEmpty()) {
            AxisAlignedBB aabb = new AxisAlignedBB(this.pos);
            List<EntityItem> items = this.world.getEntities(EntityItem.class,
                    (entity) -> entity.getEntityBoundingBox().intersects(aabb));
            if (!items.isEmpty()) {
                EntityItem select = items.get(0);
                this.handleSteppedOn(select);
            }
        }
    }

    public void handleSteppedOn(EntityItem entityItem) {
        if (this.currentlyProcessing.isEmpty() && this.outputQueue.isEmpty() && !entityItem.isDead && !entityItem.getItem().isEmpty()) {
            ItemStack over = this.tryInsertItem(entityItem.getItem());
            if (over.isEmpty()) {
                entityItem.setDead();
            } else entityItem.setItem(over);
        }
    }
    public void setCurrentlyProcessing(ItemStack stack) {
        this.currentlyProcessing = stack;
        this.currentRecipeID = CuttingRecipes.getRecipeForInput(this.currentlyProcessing, this.recipeFilter, this.world.isRemote);
        this.currentRecipe = this.currentRecipeID != null ? CuttingRecipes.instance.getRecipe(this.currentRecipeID) : null;
        this.progress = 0;
        this.sync();
    }

    private final ArrayList<SubInteractionBox> subInteractionBoxes = new ArrayList<>();
    @Nullable
    public IItemFilter recipeFilter = null;

    @Override
    public Collection<SubInteractionBox> getSubInteractionBoxes() {
        return this.subInteractionBoxes;
    }

    private boolean setFilter(@Nullable EntityPlayer player, boolean sneaking, ItemStack held) {
        if (sneaking) return false;
        ItemStack copy = held.copy();
        if (held.isEmpty()) this.recipeFilter = null;
        else this.recipeFilter = new ItemFilterExact(copy);
        this.sync();
        if (player != null) {
            if (held.isEmpty()) {
                player.playSound(SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM, 1.0F, 1.0F);
                player.sendStatusMessage(new TextComponentString("Cleared recipe filter"), true);
            } else {
                player.playSound(SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, 1.0F, 1.0F);
                player.sendStatusMessage(new TextComponentString("Set recipe filter to " + copy.getDisplayName()), true);
            }
        }
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound nbt = super.writeToNBT(compound);

        if (this.recipeFilter != null) nbt.setTag("Filter", this.recipeFilter.serialize(new NBTTagCompound()));
        if (!this.currentlyProcessing.isEmpty()) nbt.setTag("CurrentlyProcessing", this.currentlyProcessing.writeToNBT(new NBTTagCompound()));
        if (this.currentRecipe != null) nbt.setString("currentRecipe", this.currentRecipeID);
        nbt.setInteger("progress", this.progress);
        if (!this.outputQueue.isEmpty()) nbt.setTag("OutputQueue", this.outputQueue.writeToNBT(new NBTTagCompound()));

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        if (nbt.hasKey("Filter", 10)) {
            this.recipeFilter = IItemFilter.deserialize(nbt.getCompoundTag("Filter"));
        } else this.recipeFilter = null;
        if (nbt.hasKey("CurrentlyProcessing", 10)) {
            this.currentlyProcessing = new ItemStack(nbt.getCompoundTag("CurrentlyProcessing"));
        } else this.currentlyProcessing = ItemStack.EMPTY;
        if (nbt.hasKey("currentRecipe")) {
            this.currentRecipeID = nbt.getString("currentRecipe");
            this.currentRecipe = CuttingRecipes.instance.getRecipe(this.currentRecipeID);
        } else this.currentRecipe = null;
        this.progress = nbt.getInteger("progress");
        if (nbt.hasKey("OutputQueue", 10)) {
            this.outputQueue = new ItemStack(nbt.getCompoundTag("OutputQueue"));
        } else this.outputQueue = ItemStack.EMPTY;
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);

        if (this.recipeFilter != null) {
            buf.writeBoolean(true);
            this.recipeFilter.serialize(buf);
        } else buf.writeBoolean(false);
        if (!this.currentlyProcessing.isEmpty()) {
            buf.writeBoolean(true);
            StackUtil.writeItemStack(this.currentlyProcessing, buf, false, true);
        } else buf.writeBoolean(false);
        if (this.currentRecipe != null) {
            buf.writeBoolean(true);
            buf.writeInt(this.currentRecipeID.length());
            buf.internal().writeCharSequence(this.currentRecipeID, StandardCharsets.UTF_8);
            buf.append(this.currentRecipeID.length());
        } else buf.writeBoolean(false);
        buf.writeShort(this.progress);
        if (!this.outputQueue.isEmpty()) {
            buf.writeBoolean(true);
            StackUtil.writeItemStack(this.outputQueue, buf, true, true);
        } else buf.writeBoolean(false);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);

        if (buf.readBoolean()) {
            this.recipeFilter = IItemFilter.deserialize(buf);
        } else this.recipeFilter = null;
        if (buf.readBoolean()) {
            this.currentlyProcessing = StackUtil.readItemStack(buf, false, true);
        } else this.currentlyProcessing = ItemStack.EMPTY;
        if (buf.readBoolean()) {
            int len = buf.readInt();
            this.currentRecipeID = buf.readCharSequence(len, StandardCharsets.UTF_8).toString();
            this.currentRecipe = RecipeInit.getCuttingRecipes(true).getRecipe(this.currentRecipeID);
        } else {
            this.currentRecipeID = null;
            this.currentRecipe = null;
        }
        this.progress = buf.readUnsignedShort();
        if (buf.readBoolean()) {
            this.outputQueue = StackUtil.readItemStack(buf, true, true);
        } else this.outputQueue = ItemStack.EMPTY;
    }

    @Override
    public void destroy() {
        super.destroy();

        StackUtil.dropItemsAt(this.world, this.pos, this.currentlyProcessing.copy(), this.outputQueue.copy());
    }

    @Override
    public ItemStack tryInsertItem(ItemStack stack) {
        if (!this.outputQueue.isEmpty() || !this.currentlyProcessing.isEmpty()) return stack;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        this.setCurrentlyProcessing(copy);
        ItemStack ret = stack.copy();
        ret.shrink(1);
        this.sync();
        return ret.isEmpty() ? ItemStack.EMPTY : ret;
    }

    @Override
    public boolean isInsertionSlotEmpty(ItemStack stack) {
        return this.currentlyProcessing.isEmpty() && this.outputQueue.isEmpty();
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? this.currentlyProcessing : this.outputQueue;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0 || stack.isEmpty() || !this.currentlyProcessing.isEmpty()) return stack;
        ItemStack ret = stack.copy();
        ItemStack split = ret.splitStack(1);
        if (simulate) return ret;
        this.setCurrentlyProcessing(split);
        this.sync();
        return ret;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 1 || amount == 0 || this.outputQueue.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = simulate ? this.outputQueue.copy() : this.outputQueue;
        if (!simulate) this.sync();
        return copy.splitStack(amount);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T)this;
        return super.getCapability(capability, facing);
    }
}
