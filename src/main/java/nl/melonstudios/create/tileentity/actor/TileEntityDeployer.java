package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import com.melonstudios.melonlib.recipe.Ingredient;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nl.melonstudios.create.block.actor.BlockDeployer;
import nl.melonstudios.create.init.RecipeInit;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.item.ItemSandpaper;
import nl.melonstudios.create.kinetics.contraption.ContraptionInventory;
import nl.melonstudios.create.kinetics.contraption.IContraptionActor;
import nl.melonstudios.create.kinetics.contraption.accessor.IContraptionAccessor;
import nl.melonstudios.create.recipe.DeployerRecipe;
import nl.melonstudios.create.recipe.sequence.SequenceRecipe;
import nl.melonstudios.create.recipe.sequence.SequenceStep;
import nl.melonstudios.create.recipe.sequence.SequencedRecipes;
import nl.melonstudios.create.recipe.server.DeployerRecipes;
import nl.melonstudios.create.recipe.server.SandpaperPolishingRecipes;
import nl.melonstudios.create.recipe.server.DeployerApplicationRecipes;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.tileentity.marker.IDepot;
import nl.melonstudios.create.tileentity.marker.IHaltBeltContents;
import nl.melonstudios.create.tileentity.marker.ITileEntityWithSubInteractions;
import nl.melonstudios.create.util.PlayerDeployer;
import nl.melonstudios.create.util.SubInteractionBox;
import nl.melonstudios.create.util.Utils;
import nl.melonstudios.create.util.filter.IItemFilter;
import nl.melonstudios.create.util.filter.ItemFilterExact;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TileEntityDeployer extends TileEntityKinetic implements IContraptionActor, ITileEntityWithSubInteractions, IHaltBeltContents, IItemHandler {
    private PlayerDeployer player;
    public TileEntityDeployer() {
        this.createInteractions();
    }

    @Override
    public void setWorld(World worldIn) {
        this.world = worldIn;
        this.player = new PlayerDeployer(this);
        this.player.setPosition(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5);
    }

    @Override
    public void setPos(BlockPos posIn) {
        super.setPos(posIn);
        if (this.player != null)
            this.player.setPosition(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5);
    }

    private final Vector3f itemUsePosOld = new Vector3f();
    private final Vector3f itemUsePos = new Vector3f();
    private static BlockPos pos(Vector3f vec) {
        return new BlockPos(vec.x, vec.y, vec.z);
    }
    private boolean checkForRedstone() {
        return !this.world.isBlockPowered(this.pos) && this.world.isBlockIndirectlyGettingPowered(this.pos) == 0;
    }

    public int progressOld = 0;
    public int progress = 0;
    @Override
    public void tick() {
        super.tick();

        this.progressOld = this.progress;
        if (this.getSpeed() != 0.0F) {
            if (this.progress != 0 || (this.cloggedItem.isEmpty() && this.checkForRedstone())) {
                this.progress += (int) Math.abs(this.getSpeed());
                if (this.progressOld < 1000 && this.progress >= 1000) {
                    EnumFacing facing = this.getState().getValue(BlockDeployer.FACING);
                    this.itemUsePos.set(
                            this.pos.getX() + 0.5F + facing.getFrontOffsetX() * 2,
                            this.pos.getY() + 0.5F + facing.getFrontOffsetY() * 2,
                            this.pos.getZ() + 0.5F + facing.getFrontOffsetZ() * 2
                    );
                    BlockPos use = pos(this.itemUsePos);
                    this.player.setPosition(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5);
                    IDepot depot = IDepot.get(this.world, use);
                    if (facing == EnumFacing.DOWN && depot != null) {
                        if (!this.world.isRemote && !this.heldItem.isEmpty()) {
                            ItemStack in = depot.getPresentedItem();
                            if (!in.isEmpty()) {
                                recipes:
                                {
                                    {
                                        DeployerRecipe recipe = DeployerRecipes.instance.getRecipeForInput(in, this.heldItem);
                                        if (recipe != null) {
                                            ItemStack out = recipe.result.copy();
                                            depot.decreasePresentedAndAddOutput(out);
                                            switch (recipe.inputType) {
                                                case CONSUME:
                                                    this.heldItem.shrink(1);
                                                    break;
                                                case DAMAGE:
                                                    this.heldItem.damageItem(1, this.player);
                                                    if (this.heldItem.getItemDamage() >= this.heldItem.getMaxDamage()) {
                                                        this.heldItem = ItemStack.EMPTY;
                                                    }
                                                    break;
                                            }
                                            if (recipe.inputType != DeployerRecipe.InputType.CONSUME && this.heldItem.getItem() instanceof ItemSandpaper) {
                                                this.world.playSound(null,
                                                        use.getX() + 0.5F, use.getY() + depot.getItemHeight(), use.getZ() + 0.5F,
                                                        SoundInit.item_sandpaper_used, SoundCategory.BLOCKS, 1.0F, 1.0F
                                                );
                                            } else {
                                                this.world.playSound(null,
                                                        use.getX() + 0.5F, use.getY() + depot.getItemHeight(), use.getZ() + 0.5F,
                                                        SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0F, 1.0F
                                                );
                                            }
                                            if (this.heldItem.isEmpty()) {
                                                this.world.playSound(null,
                                                        use.getX() + 0.5F, use.getY() + depot.getItemHeight(), use.getZ() + 0.5F,
                                                        SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F
                                                );
                                            }
                                            break recipes;
                                        }
                                    }
                                    {
                                        SandpaperPolishingRecipes.Recipe polish = SandpaperPolishingRecipes.getRecipeForInput(in);
                                        if (polish != null && (this.heldItem.getItem() == ItemInit.SANDPAPER
                                                || this.heldItem.getItem() == ItemInit.RED_SANDPAPER)) {
                                            depot.decreasePresentedAndAddOutput(polish.result.copy());
                                            this.heldItem.damageItem(1, this.player);
                                            if (this.heldItem.getItemDamage() >= this.heldItem.getMaxDamage()) {
                                                this.heldItem = ItemStack.EMPTY;
                                            }
                                            this.world.playSound(null,
                                                    use.getX() + 0.5F, use.getY() + depot.getItemHeight(), use.getZ() + 0.5F,
                                                    SoundInit.item_sandpaper_used, SoundCategory.BLOCKS, 1.0F, 1.0F
                                            );
                                            break recipes;
                                        }
                                    }
                                    {
                                        DeployerApplicationRecipes.Recipe apply = DeployerApplicationRecipes.instance.getRecipeForInput(in, this.heldItem);
                                        if (apply != null) {
                                            depot.decreasePresentedAndAddOutput(apply.result.copy());
                                            switch (apply.inputType) {
                                                case CONSUME:
                                                    this.heldItem.shrink(1);
                                                    break;
                                                case DAMAGE:
                                                    this.heldItem.damageItem(1, this.player);
                                                    if (this.heldItem.getItemDamage() >= this.heldItem.getMaxDamage()) {
                                                        this.heldItem = ItemStack.EMPTY;
                                                    }
                                                    break;
                                            }
                                            break recipes;
                                        }
                                    }
                                    {
                                        String recipeID = SequencedRecipes.getRecipeForInput(in, this.world.isRemote);
                                        SequenceRecipe recipe = recipeID != null ? RecipeInit.getSequenceRecipes(this.world.isRemote).getRecipe(recipeID) : null;
                                        if (recipe != null) {
                                            SequenceStep first = recipe.getFirstStep();
                                            if ("deploying".equals(first.name)) {
                                                Ingredient applied = Ingredient.read(first.data.getCompoundTag("Applied"));
                                                if (applied.matches(this.heldItem)) {
                                                    DeployerRecipe.InputType inputType = DeployerRecipe.InputType.get(first.data.getString("inputType"));
                                                    ItemStack processing = recipe.processing.copy();
                                                    SequenceRecipe.initialize(processing, recipeID);
                                                    processing = SequenceRecipe.advance(processing);
                                                    depot.decreasePresentedAndAddOutput(processing);
                                                    switch (inputType) {
                                                        case CONSUME:
                                                            this.heldItem.shrink(1);
                                                            break;
                                                        case DAMAGE:
                                                            this.heldItem.damageItem(1, this.player);
                                                            if (this.heldItem.getItemDamage() >= this.heldItem.getMaxDamage()) {
                                                                this.heldItem = ItemStack.EMPTY;
                                                            }
                                                            break;
                                                    }
                                                    if (inputType != DeployerRecipe.InputType.CONSUME && this.heldItem.getItem() instanceof ItemSandpaper) {
                                                        this.world.playSound(null,
                                                                use.getX() + 0.5F, use.getY() + depot.getItemHeight(), use.getZ() + 0.5F,
                                                                SoundInit.item_sandpaper_used, SoundCategory.BLOCKS, 1.0F, 1.0F
                                                        );
                                                    } else {
                                                        this.world.playSound(null,
                                                                use.getX() + 0.5F, use.getY() + depot.getItemHeight(), use.getZ() + 0.5F,
                                                                SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0F, 1.0F
                                                        );
                                                    }
                                                    if (this.heldItem.isEmpty()) {
                                                        this.world.playSound(null,
                                                                use.getX() + 0.5F, use.getY() + depot.getItemHeight(), use.getZ() + 0.5F,
                                                                SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F
                                                        );
                                                    }
                                                }
                                                break recipes;
                                            }
                                        }
                                    }
                                    {
                                        if (SequenceRecipe.isInSequence(in)) {
                                            SequenceStep next = SequenceRecipe.getNextStep(in);
                                            if (next != null && "deploying".equals(next.name)) {
                                                Ingredient applied = Ingredient.read(next.data.getCompoundTag("Applied"));
                                                if (applied.matches(this.heldItem)) {
                                                    DeployerRecipe.InputType inputType = DeployerRecipe.InputType.get(next.data.getString("inputType"));
                                                    in = SequenceRecipe.advance(in.copy());
                                                    depot.decreasePresentedAndAddOutput(in);
                                                    switch (inputType) {
                                                        case CONSUME:
                                                            this.heldItem.shrink(1);
                                                            break;
                                                        case DAMAGE:
                                                            this.heldItem.damageItem(1, this.player);
                                                            if (this.heldItem.getItemDamage() >= this.heldItem.getMaxDamage()) {
                                                                this.heldItem = ItemStack.EMPTY;
                                                            }
                                                            break;
                                                    }
                                                    if (inputType != DeployerRecipe.InputType.CONSUME && this.heldItem.getItem() instanceof ItemSandpaper) {
                                                        this.world.playSound(null,
                                                                use.getX() + 0.5F, use.getY() + depot.getItemHeight(), use.getZ() + 0.5F,
                                                                SoundInit.item_sandpaper_used, SoundCategory.BLOCKS, 1.0F, 1.0F
                                                        );
                                                    } else {
                                                        this.world.playSound(null,
                                                                use.getX() + 0.5F, use.getY() + depot.getItemHeight(), use.getZ() + 0.5F,
                                                                SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0F, 1.0F
                                                        );
                                                    }
                                                    if (this.heldItem.isEmpty()) {
                                                        this.world.playSound(null,
                                                                use.getX() + 0.5F, use.getY() + depot.getItemHeight(), use.getZ() + 0.5F,
                                                                SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F
                                                        );
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (!this.heldItem.isEmpty()) {
                            if (this.heldItem.getItem() instanceof ItemBlock) {
                                IBlockState pre = world.getBlockState(use);
                                if (pre.getBlock().isReplaceable(world, use)) {
                                    ItemBlock ib = (ItemBlock) this.heldItem.getItem();
                                    IBlockState placed = ib.getBlock().getStateForPlacement(world, use, facing.getOpposite(),
                                            this.itemUsePos.x, this.itemUsePos.y, this.itemUsePos.z, this.heldItem.getMetadata(),
                                            this.player, EnumHand.MAIN_HAND);
                                    if (placed.getBlock().canPlaceBlockAt(world, use)) {
                                        ib.placeBlockAt(this.heldItem, this.player, world, use, facing.getOpposite(),
                                                this.itemUsePos.x, this.itemUsePos.y, this.itemUsePos.z, placed);
                                        SoundType st = placed.getBlock().getSoundType(placed, world, use, null);
                                        world.playSound(null, use, st.getPlaceSound(), SoundCategory.BLOCKS,
                                                (1.0F + st.getVolume()) * 0.5F, 0.9F + world.rand.nextFloat() * 0.2F);
                                        this.heldItem.shrink(1);
                                    }
                                }
                            } else {
                                this.heldItem.onItemUse(this.player, world, use,
                                        EnumHand.MAIN_HAND, facing.getOpposite(),
                                        this.itemUsePos.x, this.itemUsePos.y, this.itemUsePos.z
                                );
                            }
                        } else {
                            IBlockState state = this.world.getBlockState(use);
                            state.getBlock().onBlockActivated(
                                    this.world, use, state, this.player, EnumHand.MAIN_HAND, facing.getOpposite(),
                                    this.itemUsePos.x, this.itemUsePos.y, this.itemUsePos.z
                            );
                        }
                    }
                    this.sync();
                }
                if (this.progress > 2000) {
                    this.progress = 0;
                }
            }
            this.markDirty();
        }
    }

    public boolean skipRenderItem = false;
    @Override
    public void setOnContraption(boolean onContraption) {
        this.speed = onContraption ? 64.0F : 0.0F;
        this.progress = 0;
        this.skipRenderItem = onContraption;
    }

    @Override
    public boolean isOnContraption() {
        return this.skipRenderItem;
    }

    @Override
    public void contraptionTick(IContraptionAccessor contraption, World world,
                                Vector3fc position, BlockPos blockPosition, boolean moved, Vector3fc movement) {
        this.progressOld = this.progress;
        this.progress += (int) Math.abs(this.speed);
        if (this.progress > 2000) this.progress = 0;
        if (world.isRemote) return;

        this.player.setPosition(position.x(), position.y(), position.z());
        ContraptionInventory inventory = contraption.getInventory();

        this.itemUsePosOld.set(this.itemUsePos);
        EnumFacing facing = this.getState().getValue(BlockDeployer.FACING);
        contraption.getWorldPos(this.pos.offset(facing, 2), this.itemUsePos);
        BlockPos use = pos(this.itemUsePos);
        if (!pos(this.itemUsePosOld).equals(use)) {
            if (this.heldItem.isEmpty()) {
                this.heldItem = inventory.retrieveItem(this.filter, 64, false);
            }
            Vector3f facingVec = new Vector3f();
            contraption.getNormal(facing, facingVec);
            facingVec.normalize();
            final EnumFacing vecFacing = getFacingFromVector3f(facingVec);
            if (!this.heldItem.isEmpty()) {
                if (this.heldItem.getItem() instanceof ItemBlock) {
                    IBlockState pre = world.getBlockState(use);
                    if (pre.getBlock().isReplaceable(world, use)) {
                        ItemBlock ib = (ItemBlock) this.heldItem.getItem();
                        IBlockState placed = ib.getBlock().getStateForPlacement(world, use, vecFacing.getOpposite(),
                                this.itemUsePos.x, this.itemUsePos.y, this.itemUsePos.z, this.heldItem.getMetadata(),
                                this.player, EnumHand.MAIN_HAND);
                        if (placed.getBlock().canPlaceBlockAt(world, use)) {
                            if (ib.placeBlockAt(this.heldItem, this.player, world, use, vecFacing.getOpposite(),
                                    this.itemUsePos.x, this.itemUsePos.y, this.itemUsePos.z, placed)) {
                                SoundType st = placed.getBlock().getSoundType(placed, world, use, null);
                                world.playSound(null, use, st.getPlaceSound(), SoundCategory.BLOCKS,
                                        (1.0F + st.getVolume()) * 0.5F, 0.9F + world.rand.nextFloat() * 0.2F);
                                this.heldItem.shrink(1);
                            }
                        }
                    }
                } else {
                    this.heldItem.onItemUse(this.player, world, use,
                            EnumHand.MAIN_HAND, vecFacing.getOpposite(),
                            this.itemUsePos.x, this.itemUsePos.y, this.itemUsePos.z
                    );
                }
            } else {
                IBlockState state = world.getBlockState(use);
                state.getBlock().onBlockActivated(world, use, state, this.player, EnumHand.MAIN_HAND, vecFacing.getOpposite(),
                        this.itemUsePos.x, this.itemUsePos.y, this.itemUsePos.z);
            }
            this.progress = 1000;
        }

        if (!this.cloggedItem.isEmpty()) {
            this.cloggedItem = inventory.insertItem(this.cloggedItem, false);
        }
    }

    private static EnumFacing getFacingFromVector3f(Vector3f facingVec) {
        return EnumFacing.getFacingFromVector(facingVec.x, facingVec.y, facingVec.z);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        if (this.filter != null) {
            nbt.setTag("Filter", this.filter.serialize(new NBTTagCompound()));
        }
        if (!this.heldItem.isEmpty()) {
            nbt.setTag("HeldItem", this.heldItem.writeToNBT(new NBTTagCompound()));
        }
        if (!this.cloggedItem.isEmpty()) {
            nbt.setTag("CloggedItem", this.cloggedItem.writeToNBT(new NBTTagCompound()));
        }
        nbt.setInteger("progress", this.progress);

        return super.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        if (nbt.hasKey("Filter", 10)) {
            this.filter = IItemFilter.deserialize(nbt.getCompoundTag("Filter"));
        } else this.filter = null;
        if (nbt.hasKey("HeldItem", 10)) {
            this.heldItem = new ItemStack(nbt.getCompoundTag("HeldItem"));
        } else this.heldItem = ItemStack.EMPTY;
        if (nbt.hasKey("CloggedItem", 10)) {
            this.cloggedItem = new ItemStack(nbt.getCompoundTag("CloggedItem"));
        } else this.cloggedItem = ItemStack.EMPTY;
        this.progress = nbt.getInteger("progress");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);

        if (this.filter != null) {
            buf.writeBoolean(true);
            this.filter.serialize(buf);
        } else {
            buf.writeBoolean(false);
        }

        if (!this.heldItem.isEmpty()) {
            buf.writeBoolean(true);
            StackUtil.writeItemStack(this.heldItem, buf, true, true);
        } else {
            buf.writeBoolean(false);
        }

        if (!this.cloggedItem.isEmpty()) {
            buf.writeBoolean(true);
            StackUtil.writeItemStack(this.cloggedItem, buf, true, true);
        } else {
            buf.writeBoolean(false);
        }

        buf.writeInt(this.progress);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);

        if (buf.readBoolean()) {
            this.filter = IItemFilter.deserialize(buf);
        } else this.filter = null;

        if (buf.readBoolean()) {
            this.heldItem = StackUtil.readItemStack(buf, true, true);
        } else this.heldItem = ItemStack.EMPTY;

        if (buf.readBoolean()) {
            this.cloggedItem = StackUtil.readItemStack(buf, true, true);
        } else this.cloggedItem = ItemStack.EMPTY;

        this.progress = buf.readInt();
    }

    public void setFilter(ItemStack stack) {
        if (stack.isEmpty()) {
            this.filter = null;
        } else {
            this.filter = new ItemFilterExact(stack);
        }
        this.sync();
    }

    public IItemFilter filter = null;
    public ItemStack heldItem = ItemStack.EMPTY;
    public ItemStack cloggedItem = ItemStack.EMPTY;

    public boolean isEmpty() {
        return this.heldItem.isEmpty() && this.cloggedItem.isEmpty();
    }

    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack;
        if (index == 0) {
            stack = this.heldItem;
            this.heldItem = ItemStack.EMPTY;
        } else {
            stack = this.cloggedItem;
            this.cloggedItem = ItemStack.EMPTY;
        }
        this.sync();
        return stack;
    }
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == 0) {
            this.heldItem = stack;
        } else {
            this.cloggedItem = stack;
        }
        this.sync();
    }

    @Override
    public boolean shouldHaltItem(ItemStack stack) {
        if (this.getState().getValue(BlockDeployer.FACING) != EnumFacing.DOWN) return false;
        return this.getTheoreticalSpeed() != 0.0F && this.checkForRedstone();
    }

    @Override
    public void destroy() {
        super.destroy();
        StackUtil.dropItemsAt(this.world, this.pos, this.heldItem, this.cloggedItem);
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }

    private final List<SubInteractionBox> interactionsX = new ArrayList<>();
    private final List<SubInteractionBox> interactionsY = new ArrayList<>();
    private final List<SubInteractionBox> interactionsZ = new ArrayList<>();
    @Override
    public Collection<SubInteractionBox> getSubInteractionBoxes() {
        IBlockState state = this.getState();
        EnumFacing facing = state.getValue(BlockDeployer.FACING);
        boolean rotated = state.getValue(BlockDeployer.ROTATED);
        return Utils.axis_choose(facing.rotateAround(BlockDeployer.getShaftAxis(facing, rotated)).getAxis(), this.interactionsX, this.interactionsY, this.interactionsZ);
    }

    private void createInteractions() {
        SetFilterInteraction interaction = new SetFilterInteraction(this);
        this.interactionsX.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.WEST, 0.25F, interaction));
        this.interactionsX.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.EAST, 0.25F, interaction));
        this.interactionsY.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.DOWN, 0.25F, interaction));
        this.interactionsY.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.UP, 0.25F, interaction));
        this.interactionsZ.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.NORTH, 0.25F, interaction));
        this.interactionsZ.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.SOUTH, 0.25F, interaction));
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? this.heldItem : this.cloggedItem;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0) return stack;
        if (this.heldItem.isEmpty()) {
            if (simulate) return ItemStack.EMPTY;
            this.heldItem = stack.copy();
            this.sync();
            return ItemStack.EMPTY;
        }
        if (ItemHandlerHelper.canItemStacksStack(this.heldItem, stack)) {
            ItemStack copy = stack.copy();
            ItemStack ret = copy.splitStack(Math.min(this.getSlotLimit(slot), this.heldItem.getMaxStackSize()) - this.heldItem.getCount());
            if (!simulate) {
                this.heldItem.grow(ret.getCount());
                this.sync();
            }
            return copy;
        }
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0 || slot == 0) return ItemStack.EMPTY;
        if (simulate) {
            ItemStack copy = this.cloggedItem.copy();
            return copy.splitStack(amount);
        } else {
            this.sync();
            return this.cloggedItem.splitStack(amount);
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
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

    private static class SetFilterInteraction implements SubInteractionBox.Interaction {
        private final TileEntityDeployer te;
        private SetFilterInteraction(TileEntityDeployer te) {
            this.te = te;
        }

        @Override
        public boolean interact(@Nullable EntityPlayer player, boolean sneaking, ItemStack held) {
            this.te.setFilter(held);
            if (player != null) {
                if (held.isEmpty()) {
                    player.playSound(SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM, 1.0F, 1.0F);
                    player.sendStatusMessage(new TextComponentString("Cleared item filter"), true);
                } else {
                    player.playSound(SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, 1.0F, 1.0F);
                    player.sendStatusMessage(new TextComponentString("Set item filter to " + held.getDisplayName()), true);
                }
            }
            return true;
        }
    }
}
