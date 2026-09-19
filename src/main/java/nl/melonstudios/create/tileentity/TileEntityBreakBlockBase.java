package nl.melonstudios.create.tileentity;

import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class TileEntityBreakBlockBase extends TileEntityKinetic {
    public static final AtomicInteger NEXT_BREAKER_ID = new AtomicInteger();
    protected int ticksUntilNextProgress, destroyProgress;
    protected int breakerId = -NEXT_BREAKER_ID.incrementAndGet();
    protected BlockPos breakingPos;

    public TileEntityBreakBlockBase() {}

    @Override
    public void onSpeedChanged(float lastSpeed) {
        super.onSpeedChanged(lastSpeed);
        if (this.ticksUntilNextProgress == -1) {
            this.destroyNextTick();
        }
    }

    @Override
    public void tickLazy() {
        super.tickLazy();
        if (this.ticksUntilNextProgress == -1) {
            this.destroyNextTick();
        }
    }

    public void destroyNextTick() {
        this.ticksUntilNextProgress = 1;
    }

    protected abstract BlockPos getBreakingPos();
    protected boolean shouldRun() {
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setInteger("progress", this.destroyProgress);
        compound.setInteger("nextTick", this.ticksUntilNextProgress);
        if (this.breakingPos != null) compound.setLong("breaking", this.breakingPos.toLong());

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.destroyProgress = compound.getInteger("progress");
        this.ticksUntilNextProgress = compound.getInteger("nextTick");
        if (compound.hasKey("breaking")) this.breakingPos = BlockPos.fromLong(compound.getLong("breaking"));
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (!this.world.isRemote && this.destroyProgress != 0)
            this.world.sendBlockBreakProgress(this.breakerId, this.breakingPos, -1);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.world.isRemote) return;
        if (!this.shouldRun()) return;
        if (this.getSpeed() == 0) return;

        this.breakingPos = this.getBreakingPos();

        if (this.ticksUntilNextProgress < 0) return;
        if (this.ticksUntilNextProgress-- > 0) return;

        IBlockState stateToBreak = this.world.getBlockState(this.breakingPos);
        float blockHardness = stateToBreak.getBlockHardness(this.world, this.breakingPos);

        if (!this.canBreak(stateToBreak, blockHardness)) {
            if (this.destroyProgress != 0) {
                this.destroyProgress = 0;
                this.world.sendBlockBreakProgress(this.breakerId, this.breakingPos, -1);
            }
            return;
        }

        float breakSpeed = this.getBreakSpeed();
        this.destroyProgress += MathHelper.clamp((int)(breakSpeed / blockHardness), 1, 10 - this.destroyProgress);
        this.world.playSound(null, this.breakingPos, stateToBreak.getBlock().getSoundType().getHitSound(),
                SoundCategory.BLOCKS, .25F, 1.0F);

        if (this.destroyProgress >= 10) {
            this.onBlockBroken(stateToBreak);
            this.destroyProgress = 0;
            this.ticksUntilNextProgress = -1;
            this.world.sendBlockBreakProgress(this.breakerId, this.breakingPos, -1);
            return;
        }

        this.ticksUntilNextProgress = (int) (blockHardness / breakSpeed);
        this.world.sendBlockBreakProgress(this.breakerId, this.breakingPos, this.destroyProgress);
    }

    public boolean canBreak(IBlockState stateToBreak, float blockHardness) {
        return isBreakable(stateToBreak, blockHardness);
    }

    public static boolean isBreakable(IBlockState stateToBreak, float blockHardness) {
        return !(stateToBreak.getMaterial().isLiquid() || stateToBreak.getBlock() instanceof BlockAir || blockHardness < 0);
    }

    public void onBlockBroken(IBlockState stateToBreak) {
        this.world.destroyBlock(this.breakingPos, true);
    }

    protected float getBreakSpeed() {
        return Math.abs(this.getSpeed()) / 100.0F;
    }
}
