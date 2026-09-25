package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.StackUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.block.actor.BlockDrill;
import nl.melonstudios.create.kinetics.contraption.ContraptionInventory;
import nl.melonstudios.create.kinetics.contraption.IContraptionActor;
import nl.melonstudios.create.kinetics.contraption.accessor.IContraptionAccessor;
import nl.melonstudios.create.tileentity.TileEntityBreakBlockBase;
import nl.melonstudios.create.tileentity.TileEntityChute;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

public class TileEntityDrill extends TileEntityBreakBlockBase implements IContraptionActor {
    public TileEntityDrill() {
        this.setTickRateLazy(10);
    }

    @Override
    protected BlockPos getBreakingPos() {
        return this.pos.offset(this.getState().getValue(BlockDrill.FACING));
    }
    protected void getBreakingPosVec(Vector3f store) {
        store.set(this.pos.getX(), this.pos.getY(), this.pos.getZ());
        EnumFacing facing = this.getState().getValue(BlockDrill.FACING);
        store.add(facing.getFrontOffsetX(), facing.getFrontOffsetY(), facing.getFrontOffsetZ());
    }

    private boolean onContraption = false;

    @Override
    public void setOnContraption(boolean onContraption) {
        this.speed = onContraption ? 64.0F : 0.0F;
        this.onContraption = onContraption;
    }

    @Override
    public boolean isOnContraption() {
        return this.onContraption;
    }

    @Override
    public void contraptionTick(IContraptionAccessor contraption, World world, Vector3fc position, BlockPos blockPosition, boolean moved, Vector3fc movement) {
        if (this._tick(contraption, world, position, blockPosition, moved, movement)) {
            contraption.pauseContraption();
        }
    }

    private final BlockPos.MutableBlockPos lastBreakingPos = new BlockPos.MutableBlockPos();
    private final Vector3f breakingPosVec = new Vector3f();
    private boolean _tick(IContraptionAccessor contraption,  World world, Vector3fc position, BlockPos blockPosition, boolean moved, Vector3fc movement) {
        if (this.breakingPos != null) this.lastBreakingPos.setPos(this.breakingPos);
        this.getBreakingPosVec(this.breakingPosVec);
        this.breakingPos = contraption.getWorldPos(this.breakingPosVec);
        if (!this.lastBreakingPos.equals(this.breakingPos)) {
            this.ticksUntilNextProgress = 0;
        }
        if (this.ticksUntilNextProgress < 0) {
            return false;
        }
        if (this.ticksUntilNextProgress-- > 0) {
            return true;
        }

        IBlockState stateToBreak = world.getBlockState(this.breakingPos);
        float blockHardness = stateToBreak.getBlockHardness(world, this.breakingPos);

        if (stateToBreak.getCollisionBoundingBox(world, this.breakingPos) == AABB.NULL_AABB || !this.canBreak(stateToBreak, blockHardness)) {
            if (this.destroyProgress != 0) {
                this.destroyProgress = 0;
                if (!world.isRemote) {
                    world.sendBlockBreakProgress(this.breakerId, this.breakingPos, -1);
                }
            }
            return false;
        }

        float breakSpeed = Math.abs(this.speed) * 0.01F;
        this.destroyProgress += MathHelper.clamp((int)(breakSpeed / blockHardness), 1, 10 - this.destroyProgress);
        if (!world.isRemote) {
            world.playSound(null, this.breakingPos, stateToBreak.getBlock().getSoundType().getHitSound(),
                    SoundCategory.BLOCKS, .25F, 1.0F);
        }

        if (this.destroyProgress >= 10) {
            if (!world.isRemote) {
                ContraptionInventory inventory = contraption.getInventory();
                if (inventory.hasNoInventories()) {
                    world.destroyBlock(this.breakingPos, true);
                } else {
                    IBlockState state = world.getBlockState(this.breakingPos);
                    NonNullList<ItemStack> drops = NonNullList.create();
                    state.getBlock().getDrops(drops, world, this.breakingPos, state, 0);
                    List<ItemStack> leftovers = new ArrayList<>();
                    for (ItemStack  stack : drops) {
                        stack = inventory.insertItem(stack, false);
                        if (!stack.isEmpty()) leftovers.add(stack);
                    }
                    if (!leftovers.isEmpty()) {
                        StackUtil.dropItemsAt(world, this.breakingPos, leftovers.toArray(new ItemStack[0]));
                    }
                    world.destroyBlock(this.breakingPos, false);
                }
            }
            this.destroyProgress = 0;
            this.ticksUntilNextProgress = -1;
            if (!world.isRemote) {
                world.sendBlockBreakProgress(this.breakerId, this.breakingPos, -1);
            }
            return false;
        }

        this.ticksUntilNextProgress = (int) (blockHardness / breakSpeed);
        if (!world.isRemote) {
            world.sendBlockBreakProgress(this.breakerId, this.breakingPos, this.destroyProgress);
        }
        return true;
    }

    @Override
    public void onBlockBroken(IBlockState stateToBreak) {
        // Reference DrillBlockEntity.optimiseCobbleGen: stationary drill drops
        // are offered to the inventory below the broken block (belt, hopper,
        // depot, basin...) and to a chute above it before falling loose.
        if (this.world.isRemote || this.breakingPos == null) {
            super.onBlockBroken(stateToBreak);
            return;
        }
        NonNullList<ItemStack> drops = NonNullList.create();
        stateToBreak.getBlock().getDrops(drops, this.world, this.breakingPos, stateToBreak, 0);
        List<ItemStack> leftovers = new ArrayList<>();
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;
            ItemStack rest = this.insertIntoBreakOutput(stack);
            if (!rest.isEmpty()) leftovers.add(rest);
        }
        if (!leftovers.isEmpty()) {
            StackUtil.dropItemsAt(this.world, this.breakingPos, leftovers.toArray(new ItemStack[0]));
        }
        this.world.destroyBlock(this.breakingPos, false);
    }

    private ItemStack insertIntoBreakOutput(ItemStack stack) {
        ItemStack rest = stack.copy();
        TileEntity below = this.world.getTileEntity(this.breakingPos.down());
        if (below != null && below.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP)) {
            IItemHandler handler = below.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots() && !rest.isEmpty(); i++) {
                    rest = handler.insertItem(i, rest, false);
                }
            }
        }
        if (!rest.isEmpty()) {
            TileEntity above = this.world.getTileEntity(this.breakingPos.up());
            if (above instanceof TileEntityChute) {
                rest = ((TileEntityChute) above).insertItem(0, rest, false);
            }
        }
        return rest;
    }
}
