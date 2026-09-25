package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.BlockStateProperties;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.init.RecipeInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.recipe.PressingRecipe;
import nl.melonstudios.create.recipe.sequence.SequenceRecipe;
import nl.melonstudios.create.recipe.sequence.SequenceStep;
import nl.melonstudios.create.recipe.sequence.SequencedRecipes;
import nl.melonstudios.create.recipe.server.PressingRecipes;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.tileentity.marker.IDepot;
import nl.melonstudios.create.tileentity.marker.IHaltBeltContents;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class TileEntityPress extends TileEntityKinetic implements IHaltBeltContents {
    public static final float SPEED_MULTIPLIER = 1.0F;

    @SideOnly(Side.CLIENT)
    public EnumFacing.Axis getRenderAxis() {
        return this.getState().getValue(BlockStateProperties.HORIZONTAL_AXIS);
    }

    public float lastProgress;
    public float progress;

    @Override
    public void tick() {
        super.tick();

        boolean shouldMove = false;
        boolean flag = false;
        if (this.getSpeed() != 0.0F) {
            shouldMove = this.progress >= 1000;
            IDepot depot = IDepot.get(this.world, this.pos.down(2));
            if (depot != null) {
                ItemStack stack = depot.getPresentedItem();
                recipes:
                {
                    {
                        PressingRecipe recipe = PressingRecipes.getRecipeForInput(stack, this.world.isRemote);
                        if (recipe != null) {
                            shouldMove = true;
                            if (this.lastProgress < 1000 && this.progress >= 1000) {
                                this.squishParticles(
                                        stack,
                                        this.pos.getX() + 0.5,
                                        this.pos.getY() - 2 + depot.getItemHeight(),
                                        this.pos.getZ() + 0.5,
                                        depot
                                );
                                depot.decreasePresentedAndAddOutput(recipe.result.copy());
                                flag = true;
                            }
                            break recipes;
                        }
                    }
                    {
                        String recipeID = SequencedRecipes.getRecipeForInput(stack, this.world.isRemote);
                        SequenceRecipe recipe = recipeID != null ? RecipeInit.getSequenceRecipes(this.world.isRemote).getRecipe(recipeID) : null;
                        if (recipe != null) {
                            SequenceStep first = recipe.getFirstStep();
                            if ("pressing".equals(first.name)) {
                                shouldMove = true;
                                if (this.lastProgress < 1000 && this.progress >= 1000) {
                                    this.squishParticles(
                                            stack,
                                            this.pos.getX() + 0.5,
                                            this.pos.getY() - 2 + depot.getItemHeight(),
                                            this.pos.getZ() + 0.5,
                                            depot
                                    );
                                    ItemStack processing = recipe.processing.copy();
                                    SequenceRecipe.initialize(processing, recipeID);
                                    processing = SequenceRecipe.advance(processing);
                                    depot.decreasePresentedAndAddOutput(processing);
                                    flag = true;
                                }
                            }
                            break recipes;
                        }
                    }
                    {
                        if (SequenceRecipe.isInSequence(stack)) {
                            SequenceStep next = SequenceRecipe.getNextStep(stack);
                            if (next != null && "pressing".equals(next.name)) {
                                shouldMove = true;
                                if (this.lastProgress < 1000 && this.progress >= 1000) {
                                    this.squishParticles(
                                            stack,
                                            this.pos.getX() + 0.5,
                                            this.pos.getY() - 2 + depot.getItemHeight(),
                                            this.pos.getZ() + 0.5,
                                            depot
                                    );
                                    stack = SequenceRecipe.advance(stack).copy();
                                    depot.decreasePresentedAndAddOutput(stack);
                                    flag = true;
                                }
                            }
                        }
                    }
                }
            } else {
                TileEntity teBelow = this.world.getTileEntity(this.pos.down(2));
                if (teBelow instanceof TileEntityBeltBase) {
                    // Reference BeltPressingCallbacks: items riding a belt under the
                    // press are processed in place at the cycle midpoint. The belt
                    // slot is read/rewritten through its item capability so slow
                    // belts carry the pressed result onwards.
                    TileEntityBeltBase belt = (TileEntityBeltBase) teBelow;
                    IItemHandler cap = belt.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                    ItemStack peek = cap != null ? cap.getStackInSlot(0) : ItemStack.EMPTY;
                    if (!peek.isEmpty() && this.isPressable(peek)) {
                        shouldMove = true;
                        if (this.lastProgress < 1000 && this.progress >= 1000) {
                            ItemStack taken = cap.extractItem(0, 1, false);
                            if (!taken.isEmpty()) {
                                ItemStack result = this.pressResultFor(taken);
                                this.squishParticles(
                                        taken,
                                        this.pos.getX() + 0.5,
                                        this.pos.getY() - 2 + 0.75,
                                        this.pos.getZ() + 0.5,
                                        null
                                );
                                if (!result.isEmpty()) {
                                    ItemStack leftover = cap.insertItem(0, result, false);
                                    if (!leftover.isEmpty() && !this.world.isRemote) {
                                        BlockPos bp = belt.getPos();
                                        StackUtil.spawnItemNoVelocity(this.world,
                                                bp.getX() + 0.5, bp.getY() + 1.0, bp.getZ() + 0.5,
                                                leftover);
                                    }
                                }
                                flag = true;
                            }
                        }
                    }
                } else {
                List<EntityItem> entityItems = this.world.getEntitiesWithinAABB(
                        EntityItem.class,
                        new AxisAlignedBB(this.pos.down()),
                        entityItem -> entityItem.isEntityAlive() && entityItem.onGround
                );
                if (!entityItems.isEmpty()) {
                    for (EntityItem entityItem : entityItems) {
                        ItemStack stack = entityItem.getItem();
                        PressingRecipe recipe = PressingRecipes.getRecipeForInput(stack, this.world.isRemote);
                        if (recipe != null) {
                            shouldMove = true;
                            if (this.lastProgress < 1000 && this.progress >= 1000) {
                                this.squishParticles(stack, entityItem.posX, entityItem.posY, entityItem.posZ, null);
                                stack.shrink(1);
                                if (stack.isEmpty()) entityItem.setDead();
                                else entityItem.setItem(stack);
                                if (!this.world.isRemote) {
                                    StackUtil.spawnItemNoVelocity(this.world, entityItem.posX, entityItem.posY, entityItem.posZ, recipe.result.copy());
                                }
                                flag = true;
                            }
                            break;
                        }
                        if (this.tryPressWorldSequenced(entityItem, stack)) {
                            shouldMove = true;
                            if (this.lastProgress < 1000 && this.progress >= 1000) flag = true;
                            break;
                        }
                    }
                }
                }
            }
        }

        this.lastProgress = this.progress;
        if (shouldMove) {
            if (flag) {
                this.progress = 1000.0F;
                this.sync();
            }
            else {
                this.progress += Math.abs(this.getSpeed() * SPEED_MULTIPLIER);
                if (this.progress > 2000.0F) {
                    this.progress = 0.0F;
                    this.sync();
                }
            }
        }
    }

    private boolean isPressable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean client = this.world.isRemote;
        if (PressingRecipes.getRecipeForInput(stack, client) != null) return true;
        if (SequenceRecipe.isInSequence(stack)) {
            SequenceStep next = SequenceRecipe.getNextStep(stack);
            return next != null && "pressing".equals(next.name);
        }
        String recipeID = SequencedRecipes.getRecipeForInput(stack, client);
        if (recipeID != null) {
            SequenceRecipe recipe = RecipeInit.getSequenceRecipes(client).getRecipe(recipeID);
            return recipe != null && "pressing".equals(recipe.getFirstStep().name);
        }
        return false;
    }

    private ItemStack pressResultFor(ItemStack single) {
        boolean client = this.world.isRemote;
        PressingRecipe recipe = PressingRecipes.getRecipeForInput(single, client);
        if (recipe != null) return recipe.result.copy();
        if (SequenceRecipe.isInSequence(single)) {
            SequenceStep next = SequenceRecipe.getNextStep(single);
            if (next != null && "pressing".equals(next.name)) {
                return SequenceRecipe.advance(single).copy();
            }
            return ItemStack.EMPTY;
        }
        String recipeID = SequencedRecipes.getRecipeForInput(single, client);
        SequenceRecipe seq = recipeID != null ? RecipeInit.getSequenceRecipes(client).getRecipe(recipeID) : null;
        if (seq != null && "pressing".equals(seq.getFirstStep().name)) {
            ItemStack processing = seq.processing.copy();
            SequenceRecipe.initialize(processing, recipeID);
            return SequenceRecipe.advance(processing);
        }
        return ItemStack.EMPTY;
    }

    private boolean tryPressWorldSequenced(EntityItem entityItem, ItemStack stack) {
        boolean client = this.world.isRemote;
        String recipeID = SequencedRecipes.getRecipeForInput(stack, client);
        SequenceRecipe recipe = recipeID != null ? RecipeInit.getSequenceRecipes(client).getRecipe(recipeID) : null;
        if (recipe != null && "pressing".equals(recipe.getFirstStep().name)) {
            if (this.lastProgress < 1000 && this.progress >= 1000) {
                this.squishParticles(stack, entityItem.posX, entityItem.posY, entityItem.posZ, null);
                stack.shrink(1);
                ItemStack processing = recipe.processing.copy();
                SequenceRecipe.initialize(processing, recipeID);
                processing = SequenceRecipe.advance(processing);
                if (stack.isEmpty()) entityItem.setItem(processing);
                else {
                    entityItem.setItem(stack);
                    if (!this.world.isRemote) {
                        StackUtil.spawnItemNoVelocity(this.world, entityItem.posX, entityItem.posY, entityItem.posZ, processing);
                    }
                }
            }
            return true;
        }
        if (SequenceRecipe.isInSequence(stack)) {
            SequenceStep next = SequenceRecipe.getNextStep(stack);
            if (next != null && "pressing".equals(next.name)) {
                if (this.lastProgress < 1000 && this.progress >= 1000) {
                    this.squishParticles(stack, entityItem.posX, entityItem.posY, entityItem.posZ, null);
                    ItemStack advanced = SequenceRecipe.advance(stack).copy();
                    stack.shrink(1);
                    if (stack.isEmpty()) entityItem.setItem(advanced);
                    else {
                        entityItem.setItem(stack);
                        if (!this.world.isRemote) {
                            StackUtil.spawnItemNoVelocity(this.world, entityItem.posX, entityItem.posY, entityItem.posZ, advanced);
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void squishParticles(ItemStack stack, double x, double y, double z, @Nullable IDepot depot) {
        if (!this.world.isRemote) {
            float scaled = MathHelper.log2((int) Math.abs(this.getSpeed())) / 8.0F;
            float pitch = (float) MathHelper.clampedLerp(0.7F, 1.0F, scaled);
            if (depot != null && depot.isWool()) {
                this.world.playSound(null, x, y, z, SoundInit.block_press_activate, SoundCategory.BLOCKS, 0.5F, pitch);
                this.world.playSound(null, x, y, z, SoundEvents.BLOCK_CLOTH_FALL, SoundCategory.BLOCKS, 1.0F, pitch);
            } else {
                this.world.playSound(null, x, y, z, SoundInit.block_press_activate, SoundCategory.BLOCKS, 1.0F, pitch);
            }
        } else {
            Random rnd = this.world.rand;
            int offset = rnd.nextInt(45);
            for (int i = 0; i < 8; i++) {
                int rot = i * 45 + offset;
                double sin = Math.sin(Math.toRadians(rot)) * 0.2;
                double cos = Math.cos(Math.toRadians(rot)) * 0.2;
                CreateLegacy.proxy.spawnItemFX(x, y, z, sin, 0.1, cos, stack);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setFloat("lastProgress", this.lastProgress);
        nbt.setFloat("progress", this.progress);

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        this.lastProgress = nbt.getFloat("lastProgress");
        this.progress = nbt.getFloat("progress");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeFloat(this.progress);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        this.progress = buf.readFloat();
    }

    @Override
    public boolean shouldHaltItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (PressingRecipes.getRecipeForInput(stack, this.world.isRemote) != null) return true;
        boolean client = this.world.isRemote;
        if (SequenceRecipe.isInSequence(stack)) {
            NBTTagCompound data = stack.getSubCompound("SequencedAssembly");
            if (data == null) return false;
            SequenceRecipe recipe = RecipeInit.getSequenceRecipes(client).getRecipe(data.getString("id"));
            if (recipe == null) return false;
            SequenceStep next = recipe.getStep(data.getInteger("step"));
            return next != null && "pressing".equals(next.name);
        }
        String recipeID = SequencedRecipes.getRecipeForInput(stack, client);
        if (recipeID != null) {
            SequenceRecipe recipe = RecipeInit.getSequenceRecipes(client).getRecipe(recipeID);
            return recipe != null && "pressing".equals(recipe.getFirstStep().name);
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    public float multiplier() {
        IDepot depot = IDepot.get(this.world, this.pos.down(2));
        if (depot != null) {
            return (float) (1.0F + (1.0F - depot.getItemHeight()));
        }
        return 1.0F;
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
