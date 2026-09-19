package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentString;
import nl.melonstudios.create.block.actor.BlockBearingBase;
import nl.melonstudios.create.kinetics.contraption.EnumMovementType;
import nl.melonstudios.create.tileentity.marker.ITileEntityWithSubInteractions;
import nl.melonstudios.create.util.SubInteractionBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class TileEntityBearing extends TileEntityBearingBase implements ITileEntityWithSubInteractions {
    public EnumMovementType movementType = EnumMovementType.PLACE_WHEN_STOPPED;

    @Override
    public void onSpeedChanged(float lastSpeed) {
        super.onSpeedChanged(lastSpeed);

        if (!this.world.isRemote) {
            if (lastSpeed != 0.0F && this.getSpeed() == 0.0F && this.isAssembled()) {
                if (this.movementType == EnumMovementType.PLACE_WHEN_STOPPED ||
                        (this.movementType == EnumMovementType.PLACE_AT_START && this.isNearInitialAngle())) {
                    this.mightDisassemble = true;
                }
            } else if (lastSpeed == 0.0F && this.getSpeed() != 0.0F && !this.isAssembled()) {
                this.mightAssemble = true;
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setString("movementType", this.movementType.getName());

        return nbt;
    }
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        this.movementType = EnumMovementType.byName(nbt.getString("movementType"));
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeByte(this.movementType.getId());
    }
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        this.movementType = EnumMovementType.byId(buf.readByte());
    }

    private static void addSubInteractionBoxes(EnumFacing.Axis axis, TileEntityBearing te) {
        switch (axis) {
            case X:
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.DOWN, 0.25F, te::setMovementType));
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.UP, 0.25F, te::setMovementType));
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.NORTH, 0.25F, te::setMovementType));
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.SOUTH, 0.25F, te::setMovementType));
                break;
            case Y:
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.NORTH, 0.25F, te::setMovementType));
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.SOUTH, 0.25F, te::setMovementType));
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.WEST, 0.25F, te::setMovementType));
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.EAST, 0.25F, te::setMovementType));
                break;
            case Z:
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.DOWN, 0.25F, te::setMovementType));
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.UP, 0.25F, te::setMovementType));
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.WEST, 0.25F, te::setMovementType));
                te.subInteractionBoxes.add(SubInteractionBox.Helper.createCenteredSide(EnumFacing.EAST, 0.25F, te::setMovementType));
                break;
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        addSubInteractionBoxes(this.getState().getValue(BlockBearingBase.FACING).getAxis(), this);
    }

    @Override
    public void initializeClient() {
        super.initializeClient();
        addSubInteractionBoxes(this.getState().getValue(BlockBearingBase.FACING).getAxis(), this);
    }

    public boolean isNearInitialAngle() {
        float a = Math.abs(this.angle % 360.0F);
        return a < 22.5F || a > 360.0F - 22.5F;
    }

    public boolean setMovementType(EntityPlayer player, boolean sneaking, ItemStack held, int direction) {        if (SubInteractionBox.Helper.basicScrollRequirements(held, sneaking)) {
            int id = this.movementType.getId() + (int)Math.signum(direction);
            this.movementType = EnumMovementType.byId(id < 0 ? 2 : id > 2 ? 0 : id);
            this.sync();
            if (!this.world.isRemote) player.sendStatusMessage(new TextComponentString("Set movement type to " + this.movementType), true);
            return true;
        } return false;
    }

    private final ArrayList<SubInteractionBox> subInteractionBoxes = new ArrayList<>();
    @Override
    public Collection<SubInteractionBox> getSubInteractionBoxes() {
        return this.subInteractionBoxes;
    }
}
