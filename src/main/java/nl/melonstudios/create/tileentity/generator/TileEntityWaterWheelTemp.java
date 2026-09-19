package nl.melonstudios.create.tileentity.generator;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import nl.melonstudios.create.block.generator.BlockWaterWheelTemp;
import nl.melonstudios.create.tileentity.TileEntityKineticGeneratorBase;
import nl.melonstudios.create.util.Utils;

public class TileEntityWaterWheelTemp extends TileEntityKineticGeneratorBase {
    public TileEntityWaterWheelTemp() {
        super();
        this.setTickRateLazy(60);
    }
    @Override
    public float getGeneratedSpeed() {
        return MathHelper.clamp(this.flowScore, -1, 1) * 8;
    }

    public EnumFacing.Axis getAxis() {
        return this.getState().getValue(BlockWaterWheelTemp.AXIS);
    }

    @Override
    public void tickLazy() {
        super.tickLazy();

        if (!this.world.isRemote) this.determineAndApplyFlowSource();
    }

    public int flowScore;

    public void determineAndApplyFlowSource() {
        Vec3d wheelPlane =
                new Vec3d(new BlockPos(1, 1, 1).subtract(
                        EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, this.getAxis()).getDirectionVec()));
        int flowScore = 0;

        BlockPos pos = BlockPos.ORIGIN.down();
        BlockPos targetPos = this.pos.down();

        Vec3d flowAtPos = this.getFlowVectorAtPosition(targetPos);
        flowAtPos = new Vec3d(flowAtPos.x * wheelPlane.x, flowAtPos.y * wheelPlane.y, flowAtPos.z * wheelPlane.z);

        if (flowAtPos.lengthSquared() == 0) {
            this.setFlowScoreAndUpdate(0);
            return;
        }

        flowAtPos = flowAtPos.normalize();
        Vec3d normal = new Vec3d(pos).normalize();
        Vec3d positiveMotion = Utils.rotate(normal, 90, this.getAxis());
        double dot = flowAtPos.dotProduct(positiveMotion);
        if (Math.abs(dot) > 0.5F) flowScore += Math.signum(dot);

        this.setFlowScoreAndUpdate(flowScore);
    }

    public Vec3d getFlowVectorAtPosition(BlockPos pos) {
        IBlockState state = this.world.getBlockState(pos);
        if (state.getBlock() instanceof BlockLiquid) {
            BlockLiquid liquid = (BlockLiquid) state.getBlock();
            return liquid.modifyAcceleration(this.world, pos, null, Vec3d.ZERO);
        }
        return Vec3d.ZERO;
    }

    public void setFlowScoreAndUpdate(int score) {
        if (this.flowScore == score) return;
        this.flowScore = score;
        this.updateGeneratedRotation();
        this.markDirty();
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return new AxisAlignedBB(this.pos).grow(1);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("flowSource", this.flowScore);
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.flowScore = compound.getInteger("flowSource");
    }

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = super.writePacket();
        nbt.setInteger("flow", this.flowScore);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        super.readPacket(nbt);
        this.flowScore = nbt.getInteger("flow");
    }
}
