package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.blockdict.BlockDictionary;
import com.melonstudios.melonlib.misc.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockChorusFlower;
import net.minecraft.block.BlockChorusPlant;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockStem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import nl.melonstudios.create.block.actor.BlockSaw;
import nl.melonstudios.create.kinetics.contraption.ContraptionInventory;
import nl.melonstudios.create.kinetics.contraption.IContraptionActor;
import nl.melonstudios.create.kinetics.contraption.accessor.IContraptionAccessor;
import nl.melonstudios.create.tileentity.TileEntityBreakBlockBase;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

public class TileEntitySaw extends TileEntityBreakBlockBase implements IContraptionActor {
    @Override
    protected BlockPos getBreakingPos() {
        return this.pos.offset(this.getState().getValue(BlockSaw.FACING).getToEnumFacing());
    }

    @Override
    public boolean canBreak(IBlockState stateToBreak, float blockHardness) {
        return super.canBreak(stateToBreak, blockHardness) && isSawable(stateToBreak);
    }

    public static boolean isSawable(IBlockState stateToBreak) {
        if (BlockDictionary.isBlockTagged(stateToBreak, "logWood"))
            return true;
        if (BlockDictionary.isBlockTagged(stateToBreak, "treeLeaves"))
            return true;
        Block block = stateToBreak.getBlock();
        return block instanceof BlockCactus
                || block instanceof BlockReed
                || block instanceof BlockChorusPlant
                || block instanceof BlockChorusFlower
                || block instanceof BlockStem;
    }

    @Override
    public void onBlockBroken(IBlockState stateToBreak) {
        cutDownTree(this.world, this.breakingPos);
    }

    protected float getBreakSpeed() {
        return Math.abs(this.getSpeed()) / 50.0F;
    }

    public EnumFacing facing() {
        return this.getState().getValue(BlockSaw.FACING).getToEnumFacing();
    }

    public static void cutDownTree(World world, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            mutable.setPos(pos).move(facing);
            if (BlockDictionary.isBlockTagged(world.getBlockState(mutable), "logWood")) {
                world.destroyBlock(pos, true);
                return;
            }
        }
        ArrayList<BlockPos> logs = new ArrayList<>();
        logs.add(pos);
        spreadBranch(world, pos, logs, mutable);
        if (!logs.isEmpty()) {
            ArrayList<BlockPos> logsCopy = new ArrayList<>(logs);
            BlockPos.MutableBlockPos m2 = new BlockPos.MutableBlockPos();
            for (BlockPos log : logsCopy) {
                spreadLeaves(world, log, logs, mutable, m2);
            }
        }

        for (BlockPos log : logs) {
            world.destroyBlock(log, true);
        }
    }
    public static void cutDownTree(World world, BlockPos pos, NonNullList<ItemStack> drops) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            mutable.setPos(pos).move(facing);
            IBlockState state = world.getBlockState(mutable);
            if (BlockDictionary.isBlockTagged(state, "logWood")) {
                state.getBlock().getDrops(drops, world, mutable, state, 0);
                world.destroyBlock(pos, false);
                return;
            }
        }
        ArrayList<BlockPos> logs = new ArrayList<>();
        logs.add(pos);
        spreadBranch(world, pos, logs, mutable);
        if (!logs.isEmpty()) {
            ArrayList<BlockPos> logsCopy = new ArrayList<>(logs);
            BlockPos.MutableBlockPos m2 = new BlockPos.MutableBlockPos();
            for (BlockPos log : logsCopy) {
                spreadLeaves(world, log, logs, mutable, m2);
            }
        }

        for (BlockPos log : logs) {
            IBlockState state = world.getBlockState(log);
            state.getBlock().getDrops(drops, world, log, state, 0);
            world.destroyBlock(log, false);
        }
    }

    private static void spreadBranch(World world, BlockPos pos, ArrayList<BlockPos> list, BlockPos.MutableBlockPos mutable) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    mutable.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (!list.contains(mutable)) {
                        IBlockState state = world.getBlockState(mutable);
                        if (BlockDictionary.isBlockTagged(state, "logWood")) {
                            BlockPos to = mutable.toImmutable();
                            list.add(to);
                            spreadBranch(world, to, list, mutable);
                        }
                    }
                }
            }
        }
    }
    private static void spreadLeaves(World world, BlockPos pos, ArrayList<BlockPos> list,
                                     BlockPos.MutableBlockPos mutable, BlockPos.MutableBlockPos m2) {
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                zLoop:
                for (int z = -3; z <= 3; z++) {
                    mutable.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    IBlockState state = world.getBlockState(mutable);
                    if (BlockDictionary.isBlockTagged(state, "treeLeaves")) {
                        for (EnumFacing side : EnumFacing.VALUES) {
                            m2.setPos(mutable);
                            m2.move(side);
                            if (BlockDictionary.isBlockTagged(world.getBlockState(m2), "logWood") && !list.contains(m2)) {
                                continue zLoop;
                            }
                        }
                        list.add(mutable.toImmutable());
                    }
                }
            }
        }
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

    protected void getBreakingPosVec(Vector3f store) {
        store.set(this.pos.getX(), this.pos.getY(), this.pos.getZ());
        EnumFacing facing = this.getState().getValue(BlockSaw.FACING).getToEnumFacing();
        store.add(facing.getFrontOffsetX(), facing.getFrontOffsetY(), facing.getFrontOffsetZ());
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

        if (!this.canBreak(stateToBreak, blockHardness)) {
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
                    cutDownTree(world, this.breakingPos);
                } else {
                    NonNullList<ItemStack> drops = NonNullList.create();
                    cutDownTree(world, this.breakingPos, drops);
                    List<ItemStack> leftovers = new ArrayList<>();
                    ItemStack leftover;
                    for (ItemStack stack : drops) {
                        leftover = inventory.insertItem(stack, false);
                        if (!leftover.isEmpty()) leftovers.add(leftover.copy());
                    }
                    if (!leftovers.isEmpty()) {
                        StackUtil.dropItemsAt(world, this.breakingPos, leftovers.toArray(new ItemStack[0]));
                    }
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
}
