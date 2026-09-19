package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.block.actor.BlockContraptionInterface;
import nl.melonstudios.create.kinetics.contraption.ContraptionInventory;
import nl.melonstudios.create.kinetics.contraption.IContraptionActor;
import nl.melonstudios.create.kinetics.contraption.accessor.IContraptionAccessor;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.Utils;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.IOException;

public abstract class TileEntityContraptionInterfaceBase extends TileEntityOptimizedBase implements IContraptionActor {
    public TileEntityContraptionInterfaceBase() {
        super();
    }

    @Override
    public void tick() {
        this.wasConnected = this.isConnected();
    }

    @Override
    public void tickLazy() {

    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("DisconnectionTimer", this.disconnectionTimer);
        compound.setBoolean("Powered", this.powered);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.disconnectionTimer = compound.getInteger("DisconnectionTimer");
        this.powered = compound.getBoolean("Powered");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        buf.writeInt(this.disconnectionTimer);
        buf.writeBoolean(this.powered);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.disconnectionTimer = buf.readInt();
        this.powered = buf.readBoolean();
    }

    /** Redstone handling (reference: neighbourChanged stops transfer while powered). */
    public void neighbourChanged() {
        if (this.world == null || this.world.isRemote) return;
        boolean isPowered = this.world.isBlockPowered(this.pos);
        if (isPowered == this.powered) return;
        this.powered = isPowered;
        if (this.powered) {
            this.resetTarget();
            this.connectedInv = null;
        }
        this.sync();
    }

    public boolean isPowered() {
        return this.powered;
    }

    /** Reference analog: transfer only counts while the link is live. */
    public boolean canTransfer() {
        return this.isConnected();
    }

    public EnumFacing getFacing() {
        return this.getState().getValue(BlockDirectional.FACING);
    }
    public ContraptionInventory getInventory() {
        return this.connectedInv != null ? this.connectedInv : ContraptionInventory.empty();
    }

    public boolean wasConnected() {
        return this.wasConnected;
    }
    public boolean isConnected() {
        return this.onContraption ? this.target != null : this.disconnectionTimer > 0;
    }
    public float getConnectorOffset() {
        return 0.5F;
    }

    private boolean wasConnected = false;
    private ContraptionInventory connectedInv;
    private TileEntityContraptionInterfaceBase target;
    protected int disconnectionTimer = 0;
    protected boolean powered = false;
    private BlockPos lastConnection = BlockPos.ORIGIN;
    private boolean onContraption = false;
    private final Vector3f contraptionFacing = new Vector3f();

    public void setDisconnectionTimer(int ticks) {
        this.disconnectionTimer = ticks;
        this.sync();
    }
    private boolean isValidRotation() {
        return (Math.abs(this.contraptionFacing.x) > 0.75F || Math.abs(this.contraptionFacing.y) > 0.75F || Math.abs(this.contraptionFacing.z) > 0.75F);
    }

    @Override
    public void setOnContraption(boolean onContraption) {
        this.onContraption = onContraption;
    }

    @Override
    public boolean isOnContraption() {
        return this.onContraption;
    }

    private static BlockContraptionInterface.Variant variantOf(IBlockState state) {
        try {
            return state.getValue(BlockContraptionInterface.VARIANT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void contraptionTick(IContraptionAccessor contraption, World world, Vector3fc position, BlockPos blockPosition, boolean moved, Vector3fc movement) {
        this.wasConnected = this.isConnected();
        if (this.powered) {
            this.resetTarget();
            return;
        }
        EnumFacing facing = this.getFacing();
        contraption.getNormal(facing, this.contraptionFacing);
        if (this.isValidRotation()) {
            EnumFacing globalFacing = EnumFacing.getFacingFromVector(this.contraptionFacing.x, this.contraptionFacing.y, this.contraptionFacing.z);
            BlockContraptionInterface.Variant ownVariant = variantOf(this.getState());
            TileEntityContraptionInterfaceBase found = null;
            BlockPos foundPos = null;
            // Reference searches the 2 blocks ahead along facing; first opposite-facing match wins.
            for (int d = 1; d <= 2; d++) {
                BlockPos candidate = contraption.getWorldPos(this.pos.offset(facing, d));
                IBlockState targetState = world.getBlockState(candidate);
                if (targetState.getBlock() != this.getBlockType()) continue;
                if (targetState.getValue(BlockDirectional.FACING) != globalFacing.getOpposite()) continue;
                if (ownVariant != null && variantOf(targetState) != ownVariant) continue;
                TileEntityContraptionInterfaceBase targetTE =
                        Utils.cast(world.getTileEntity(candidate), TileEntityContraptionInterfaceBase.class);
                if (targetTE == null || targetTE.isInvalid() || targetTE.isPowered()) continue;
                found = targetTE;
                foundPos = candidate;
                break;
            }
            if (found == null) {
                this.resetTarget();
            } else if (this.target != found) {
                if (!this.lastConnection.equals(foundPos)) {
                    this.target = found;
                    this.target.connectedInv = contraption.getInventory();
                    this.target.disconnectionTimer = 10;
                    this.target.sync();
                    contraption.pauseContraption();
                } else this.resetTarget();
            } else if (!this.target.isInvalid()) {
                if (this.target.disconnectionTimer-- > 0) {
                    contraption.pauseContraption();
                    this.target.connectedInv = contraption.getInventory();
                } else this.resetTarget();
            }
            this.lastConnection = (foundPos != null ? foundPos : contraption.getWorldPos(this.pos.offset(facing, 2))).toImmutable();
        } else this.resetTarget();
    }

    protected void resetTarget() {
        if (this.target != null) {
            this.target.connectedInv = null;
            this.target.disconnectionTimer = 0;
            this.target.target = null;
            this.target = null;
        }
    }
}
