package nl.melonstudios.create.tileentity.generator;

import com.melonstudios.melonlib.misc.BlockStateProperties;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.tileentity.TileEntityKineticGeneratorBase;
import nl.melonstudios.create.util.Utils;
import nl.melonstudios.create.util.interfaces.IRotate;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

public class TileEntityWaterWheel extends TileEntityKineticGeneratorBase {
    public static final EnumMap<Axis, Set<BlockPos>> SMALL_OFFSETS = new EnumMap<>(Axis.class);
    public static final EnumMap<Axis, Set<BlockPos>> LARGE_OFFSETS = new EnumMap<>(Axis.class);

    static {
        for (Axis axis : Axis.values()) {
            HashSet<BlockPos> offsets = new HashSet<>();
            for (EnumFacing facing : EnumFacing.VALUES) {
                if (facing.getAxis() != axis) offsets.add(BlockPos.ORIGIN.offset(facing));
            }
            SMALL_OFFSETS.put(axis, offsets);

            offsets = new HashSet<>();
            for (EnumFacing facing : EnumFacing.VALUES) {
                if (facing.getAxis() == axis) continue;
                BlockPos centralOffset = BlockPos.ORIGIN.offset(facing, 2);
                offsets.add(centralOffset);
                for (EnumFacing facing1 : EnumFacing.VALUES) {
                    if (facing1.getAxis() == axis) continue;
                    if (facing1.getAxis() == facing.getAxis()) continue;
                    offsets.add(centralOffset.offset(facing1));
                }
            }
            LARGE_OFFSETS.put(axis, offsets);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        this.determineAndApplyFlowSource();
    }

    public TileEntityWaterWheel() {
        super();
        this.setTickRateLazy(60);
    }

    public int flowScore;

    protected int getSize() {
        return 1;
    }
    protected Set<BlockPos> getOffsetsToCheck() {
        return (this.getSize() == 1 ? SMALL_OFFSETS : LARGE_OFFSETS).get(this.getAxis());
    }
    protected Axis getAxis() {
        Axis axis = Axis.X;
        IBlockState state = this.getState();
        if (state.getBlock() instanceof IRotate) axis = ((IRotate)state.getBlock()).getRotationAxis(state);
        return axis;
    }

    @Override
    public void tickLazy() {
        super.tickLazy();

        if (!this.world.isRemote) this.determineAndApplyFlowSource();
    }

    public void determineAndApplyFlowSource() {
        Vec3d wheelPlane =
                new Vec3d(new BlockPos(1, 1, 1).subtract(
                        EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, this.getAxis()).getDirectionVec()));

        int flowScore = 0;
        boolean lava = false;
        for (BlockPos pos : this.getOffsetsToCheck()) {
            BlockPos targetPos = pos.add(this.pos);
            Vec3d flowAtPos = this.getFlowVectorAtPosition(targetPos);
            flowAtPos = new Vec3d(flowAtPos.x * wheelPlane.x, flowAtPos.y * wheelPlane.y, flowAtPos.z * wheelPlane.z);
            lava |= this.isLava(this.world.getBlockState(targetPos));

            if (flowAtPos.lengthSquared() == 0) continue;

            flowAtPos = flowAtPos.normalize();
            Vec3d normal = new Vec3d(pos).normalize();
            Vec3d positiveMotion = Utils.rotate(normal, 90, this.getAxis());
            double dot = flowAtPos.dotProduct(positiveMotion);
            if (Math.abs(dot) > 0.5F) flowScore += Math.signum(dot);
        }
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

    protected boolean isLava(IBlockState state) {
        return state.getBlock().getMaterial(state) == Material.LAVA;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.flowScore = compound.getInteger("flowScore");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setInteger("flowScore", this.flowScore);

        return compound;
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeInt(this.flowScore);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        this.flowScore = buf.readInt();
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return new AxisAlignedBB(this.pos).grow(this.getSize());
    }

    @Override
    public float getGeneratedSpeed() {
        return (float) (MathHelper.clamp(this.flowScore, -1, 1) * 8) / this.getSize();
    }

    @SideOnly(Side.CLIENT)
    public EnumFacing getRenderFacing() {
        return this.getState().getValue(BlockStateProperties.FACING);
    }
}
