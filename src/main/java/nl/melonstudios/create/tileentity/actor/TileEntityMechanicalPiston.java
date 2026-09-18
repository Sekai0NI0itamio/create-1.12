package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.block.BlockPistonPole;
import nl.melonstudios.create.block.actor.BlockMechanicalPiston;
import nl.melonstudios.create.block.actor.BlockMechanicalPistonHead;
import nl.melonstudios.create.entity.EntityContraptionPiston;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.kinetics.contraption.*;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TileEntityMechanicalPiston extends TileEntityKinetic implements ITileEntityWithContraption {
    public ContraptionResult.AssemblyFailure lastFailure = null;
    public UUID attachedContraptionUUID = null;
    public EntityContraptionPiston attachedContraptionEntity = null;

    public boolean tryAssemble() {
        if (this.isAssembled()) {
            this.enableDisassembly();
            return true;
        } else if (this.getSpeed() != 0.0F) {
            this.enableAssembly();
            return true;
        }
        return false;
    }

    protected boolean mightAssemble = false;
    protected boolean mightDisassemble = false;
    protected boolean pausedLastTick = false;
    protected boolean isPausedThisTick = false;

    public void enableAssembly() {
        if (this.world.isRemote) return;
        this.mightAssemble = true;
    }
    public void enableDisassembly() {
        if (this.world.isRemote) return;
        this.mightDisassemble = true;
    }

    @Nullable
    protected final EntityContraptionPiston getAttachedContraption() {
        return this.attachedContraptionEntity;
    }

    public BlockMechanicalPiston getBlock() {
        return (BlockMechanicalPiston) this.getBlockType();
    }
    public boolean isSticky() {
        return this.getBlock().sticky;
    }

    @Override
    public void onAssembly() {
        super.onAssembly();

        if (this.isAssembled()) this.disassemble();
    }

    public BlockPos getPolesFront() {
        if (!this.getBlock().extended) return this.pos;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        EnumFacing facing = this.getFacing();
        mutable.setPos(this.pos).move(facing);
        EnumFacing.Axis axis = facing.getAxis();
        IBlockState state;
        while ((state = this.world.getBlockState(mutable)).getBlock() == BlockInit.PISTON_POLE && state.getValue(BlockPistonPole.AXIS) == axis) {
            mutable.move(facing);
        }
        if (state.getBlock() == BlockInit.PISTON_HEAD && state.getValue(BlockMechanicalPistonHead.FACING) == facing) {
            return mutable.toImmutable();
        } else return this.pos;
    }
    public BlockPos getPolesBehind() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        EnumFacing facing = this.getFacing().getOpposite();
        mutable.setPos(this.pos).move(facing);
        EnumFacing.Axis axis = facing.getAxis();
        IBlockState state;
        while ((state = this.world.getBlockState(mutable)).getBlock() == BlockInit.PISTON_POLE && state.getValue(BlockPistonPole.AXIS) == axis) {
            mutable.move(facing);
        }
        mutable.move(facing.getOpposite());
        return mutable.toImmutable();
    }

    public void emergencyDisassemble() {
        this.assemblyChanged = true;
        this.mightDisassemble = this.mightAssemble = false;
        if (this.attachedContraptionEntity != null) {
            this.attachedContraptionEntity.placeBlocks();
            this.world.removeEntity(this.attachedContraptionEntity);
            this.world.playSound(null, this.pos, SoundInit.contraption_disassemble, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        this.attachedContraptionEntity = null;
        this.attachedContraptionUUID = null;
        this.numExtensionPoles = -1;
        this.sync();
    }
    public boolean disassemble() {
        if (this.assemblyChanged) return false;
        if (this.world.isRemote) return true;
        this.assemblyChanged = true;
        if (this.attachedContraptionEntity != null) {
            //this.attachedContraptionEntity.cachedExtension = Math.max(this.attachedContraptionEntity.cachedExtension, 0.0F);
            this.attachedContraptionEntity.placeBlocks();
            this.attachedContraptionEntity.piston = null;
            this.attachedContraptionEntity.pistonPos = null;
            this.world.removeEntity(this.attachedContraptionEntity);
        }
        this.attachedContraptionEntity = null;
        this.attachedContraptionUUID = null;
        int extension = Math.max(Math.round(this.extension), 0);
        int offset = this.numExtensionPoles - extension;
        EnumFacing facing = this.getFacing();
        BlockPos min = this.pos.offset(facing, -offset);
        BlockPos max = this.pos.offset(facing, extension);
        IBlockState pole = BlockInit.PISTON_POLE.getDefaultState().withProperty(BlockPistonPole.AXIS, facing.getAxis());
        for (BlockPos pos : BlockPos.getAllInBox(min, max)) {
            if (pos.equals(this.pos)) continue;
            if (pos.equals(max)) {
                IBlockState state = BlockInit.PISTON_HEAD.getDefaultState()
                        .withProperty(BlockMechanicalPistonHead.FACING, facing)
                        .withProperty(BlockMechanicalPistonHead.STICKY, this.getBlock().sticky);
                this.world.setBlockState(pos, state);
            } else {
                this.world.setBlockState(pos, pole);
            }
        }
        this.numExtensionPoles = -1;
        if (extension == 0)
            Utils.setBlockKineticTESafe2(this.world, this.pos, BlockMechanicalPiston.setAssembled(this.getState(), false), 3);
        //this.extensionOld = this.extension = 0.0F;
        this.sync();
        this.world.playSound(null, this.pos, SoundInit.contraption_disassemble, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }
    protected boolean assemble(boolean forward) {
        if (this.assemblyChanged) return false;
        if (this.world.isRemote) return true;
        this.assemblyChanged = true;
        BlockPos front = this.getPolesFront();
        BlockPos behind = this.getPolesBehind();
        boolean flag = front.equals(this.pos) && this.getBlock().extended;
        if (!forward && front.equals(this.pos)) flag = true;
        if (forward && behind.equals(this.pos)) flag = true;
        if (flag) {
            //TODO: lang key
            this.lastFailure = new ContraptionResult.AssemblyFailure("Missing extension poles");
            this.sync();
            return true;
        }
        int initialExtension = Math.abs(Utils.axis_choose(this.getFacing().getAxis(),
                this.pos.getX() - front.getX(),
                this.pos.getY() - front.getY(),
                this.pos.getZ() - front.getZ()
        ));
        this.numExtensionPoles = Utils.dist_manh(front, this.pos) + Utils.dist_manh(behind, this.pos);
        EntityContraptionPiston piston = new EntityContraptionPiston(this);
        if (forward || this.getBlock().sticky) {
            ContraptionResult result = this.assembleContraption(piston, front, behind);
            boolean flag1 = false;
            if (result.hasFailed()) {
                piston.setDead();
                if ("assembly_failure.no_structure".equals(result.getError().error)) {
                    flag1 = true;
                } else {
                    this.lastFailure = result.getError();
                    this.sync();
                    return true;
                }
            }
            if (!flag1) {
                piston.contraption = result.getContraption();
                this.world.spawnEntity(piston);
                this.attachedContraptionEntity = piston;
                this.attachedContraptionUUID = piston.getPersistentID();
            }
        }
        this.lastFailure = null;
        Utils.setBlockKineticTESafe2(this.world, this.pos, BlockMechanicalPiston.setAssembled(this.getState(), true), 3);
        for (BlockPos pos : BlockPos.getAllInBox(behind, front)) {
            if (pos.equals(this.pos)) continue;
            this.world.setBlockState(pos, Blocks.AIR.getDefaultState());
        }
        CreateLegacy.logger.debug("yummersiq");
        this.extensionOld = this.extension = initialExtension;
        this.sync();
        this.world.playSound(null, this.pos, SoundInit.contraption_assemble, SoundCategory.BLOCKS, 1.0F, 1.0F);
        this.world.playSound(null, this.pos, SoundInit.contraption_assemble_compound, SoundCategory.BLOCKS, 0.25F, 1.1F);
        return true;
    }

    @Override
    public void destroy() {
        super.destroy();
        this.assemblyChanged = false;
        if (this.isAssembled()) this.emergencyDisassemble();
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        if (this.numExtensionPoles == -1) return new AxisAlignedBB(this.pos);
        EnumFacing.Axis axis = this.getFacing().getAxis();
        switch (axis) {
            case X: return new AxisAlignedBB(this.pos.east(this.numExtensionPoles), this.pos.west(this.numExtensionPoles));
            case Y: return new AxisAlignedBB(this.pos.up(this.numExtensionPoles), this.pos.down(this.numExtensionPoles));
            case Z: return new AxisAlignedBB(this.pos.north(this.numExtensionPoles), this.pos.south(this.numExtensionPoles));
            default:return INFINITE_EXTENT_AABB;
        }
    }

    private static final boolean IGNORE = false;
    private static boolean isInsideInclusive(BlockPos pos, BlockPos p1, BlockPos p2) {
        if (IGNORE) return false;
        int minX = Math.min(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxX = Math.max(p1.getX(), p2.getX());
        int maxY = Math.max(p1.getY(), p2.getY());
        int maxZ = Math.max(p1.getZ(), p2.getZ());

        if (pos.getX() < minX || pos.getX() > maxX) return false;
        if (pos.getZ() < minZ || pos.getZ() > maxZ) return false;
        return pos.getY() >= minY && pos.getY() <= maxY;
    }
    protected ContraptionResult assembleContraption(IContraptionHolder holder, BlockPos start, BlockPos end) {
        return Contraption.assemble(holder, start.offset(this.getFacing()),
                (pos) -> isInsideInclusive(pos, start, end),
                this.getContraptionChecker()
        );
    }
    protected ContraptionAssemblyChecker getContraptionChecker() {
        return ContraptionAssembly.NO_CHECKER;
    }

    public EnumFacing getFacing() {
        return this.getState().getValue(BlockMechanicalPiston.FACING);
    }
    public boolean isAssembled() {
        return this.getBlock().extended && (this.numExtensionPoles != -1 || this.attachedContraptionUUID != null);
    }

    @Override
    public void onSpeedChanged(float lastSpeed) {
        super.onSpeedChanged(lastSpeed);
        if (this.speedFix == null) this.speedFix = lastSpeed;

        if (!this.world.isRemote) {
            if (lastSpeed != 0.0F && this.getSpeed() == 0.0F) {
                CreateLegacy.logger.debug("?");
                this.enableDisassembly();
            } else if (/*(lastSpeed == 0.0F && this.getSpeed() != 0.0F) || */Math.signum(lastSpeed) != Math.signum(this.getSpeed())) {
                CreateLegacy.logger.debug("!");
                this.enableAssembly();
            }
        }
    }

    private Float speedFix = null;
    public boolean assemblyChanged = false;
    public float extensionOld, extension;
    public int numExtensionPoles = -1;

    @Override
    public void tick() {
        this.assemblyChanged = false;
        this.extensionOld = this.extension;
        super.tick();

        if (this.mightAssemble) this.mightDisassemble = false;
        if (this.mightDisassemble != this.mightAssemble) {
            if (this.mightAssemble && this.getSpeed() != 0.0F && !this.isAssembled()) {
                this.assemble(this.getSpeed() > 0);
            }
            if (this.mightDisassemble && this.isAssembled()) {
                this.disassemble();
            }
        }

        if (this.numExtensionPoles == -1) this.attachedContraptionUUID = null;
        this.mightAssemble = this.mightDisassemble = false;

        if (this.attachedContraptionEntity != null && (this.attachedContraptionEntity.isDead || this.attachedContraptionEntity.world != this.world)) {
            CreateLegacy.logger.debug("gng {}", this.world.isRemote);
            this.attachedContraptionEntity = null;
        }
        if (this.attachedContraptionUUID != null && this.attachedContraptionEntity == null) {
            List<EntityContraptionPiston> list = this.world.getEntitiesWithinAABB(EntityContraptionPiston.class, AABB.wrap(this.pos, 3),
                    (e) -> e.getPersistentID().equals(this.attachedContraptionUUID));
            if (!list.isEmpty()) {
                CreateLegacy.logger.debug("Attached contraption! {} {}", this.world.isRemote, list.size());
                this.attachedContraptionEntity = list.get(list.size() - 1);
            } //else CreateLegacy.logger.debug("hello? :( {}", this.world.isRemote);
        }
        if (!this.world.isRemote) {
            if (this.attachedContraptionEntity != null && this.attachedContraptionEntity.isDead) {
                this.attachedContraptionUUID = null;
                this.attachedContraptionEntity = null;
                this.sync();
            }
        }

        if (this.isAssembled() && !this.overstressed) {
            if (!this.isPausedThisTick) {
                float movement = Math.max(-0.49F, Math.min(0.49F, this.getSpeed() / 512.0F));
                this.extension += movement;

                if (this.extension <= 0.0F) {
                    this.extension = 0.0F;
                    this.enableDisassembly();
                }
                if (this.extension >= this.numExtensionPoles) {
                    this.extension = this.numExtensionPoles;
                    this.enableDisassembly();
                }
            }
            EntityContraptionPiston piston = this.getAttachedContraption();
            if (piston != null) {
                piston.piston = this;
                piston.pistonPos = this.pos;
                piston.cachedExtensionOld = this.extensionOld;
                if (piston.cachedExtension != this.extension) {
                    piston.cachedExtension = this.extension;
                    try {
                        //piston.moveBB();
                    } catch (Exception e) {
                        CrashReport report = new CrashReport("Exception moving piston AABB", e);
                        throw new ReportedException(report);
                    }
                }
            } else CreateLegacy.logger.error("vro {}", this.world.isRemote);
            if (!this.world.isRemote && (this.world.getTotalWorldTime() & 63) == 0) {
                //Synchronize every so often to make sure it is equal at all times
                this.sync();
            }
            this.markDirty();
        }
        if (this.pausedLastTick != this.isPausedThisTick) this.sync();
        this.pausedLastTick = this.isPausedThisTick;
        this.isPausedThisTick = false;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setFloat("extension", this.extension);
        if (this.attachedContraptionUUID != null) nbt.setUniqueId("AttachedContraptionUUID", this.attachedContraptionUUID);
        nbt.setInteger("numExtensionPoles", this.numExtensionPoles);

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        this.extension = nbt.getFloat("extension");
        if (nbt.hasKey("AttachedContraptionUUIDLeast")) {
            this.attachedContraptionUUID = nbt.getUniqueId("AttachedContraptionUUID");
        } else {
            this.attachedContraptionUUID = null;
            this.attachedContraptionEntity = null;
        }
        this.numExtensionPoles = nbt.getInteger("numExtensionPoles");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);

        buf.writeFloat(this.extension);
        if (this.lastFailure != null) {
            String err = this.lastFailure.error;
            buf.writeInt(err.length());
            buf.internal().writeCharSequence(err, StandardCharsets.UTF_8);
            buf.append(err.length());
        } else {
            buf.writeInt(0);
        }
        if (this.attachedContraptionUUID != null) {
            buf.writeBoolean(true);
            buf.writeLong(this.attachedContraptionUUID.getMostSignificantBits());
            buf.writeLong(this.attachedContraptionUUID.getLeastSignificantBits());
        } else buf.writeBoolean(false);
        buf.writeInt(this.numExtensionPoles);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);

        this.extension = buf.readFloat();
        int len = buf.readInt();
        if (len > 0) {
            this.lastFailure = new ContraptionResult.AssemblyFailure(buf.readCharSequence(len, StandardCharsets.UTF_8).toString());
        } else this.lastFailure = null;
        if (buf.readBoolean()) {
            this.attachedContraptionUUID = new UUID(buf.readLong(), buf.readLong());
        } else {
            this.attachedContraptionUUID = null;
        }
        this.attachedContraptionEntity = null;
        this.numExtensionPoles = buf.readInt();

        this.invalidateRenderBoundingBox();
    }

    private static final boolean USE_OPTIMIZED_AABB = true;
    @Override
    public void collectCollisions(AxisAlignedBB aabb, List<AxisAlignedBB> collisions) {
        if (this.attachedContraptionEntity != null) {
            if (aabb.intersects(this.attachedContraptionEntity.getEntityBoundingBox())) {
                Contraption contraption = this.attachedContraptionEntity.attachedContraption();
                if (contraption != null) {
                    if (USE_OPTIMIZED_AABB) collisions.addAll(contraption.optimizedAABB);
                    else {
                        EnumFacing facing = this.getFacing();
                        double dx = this.attachedContraptionEntity.posX - 0.5 + (facing.getFrontOffsetX() * this.extension);
                        double dy = this.attachedContraptionEntity.posY - 0.5 + (facing.getFrontOffsetY() * this.extension);
                        double dz = this.attachedContraptionEntity.posZ - 0.5 + (facing.getFrontOffsetZ() * this.extension);
                        for (Map.Entry<BlockPos, IBlockState> entry : contraption.blocks.entrySet()) {
                            AxisAlignedBB bounds = entry.getValue().getCollisionBoundingBox(contraption, entry.getKey());
                            if (bounds != null) {
                                bounds = bounds.offset(
                                        entry.getKey().getX() + dx,
                                        entry.getKey().getY() + dy,
                                        entry.getKey().getZ() + dz
                                );
                                if (bounds.intersects(aabb)) collisions.add(bounds);
                            }
                        }
                    }
                }
            }
        }
    }

    public void pauseContraption() {
        this.isPausedThisTick = true;
    }
}
