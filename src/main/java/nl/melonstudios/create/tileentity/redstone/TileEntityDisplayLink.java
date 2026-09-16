package nl.melonstudios.create.tileentity.redstone;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import nl.melonstudios.create.tileentity.TileEntityFluidTank;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Display link TE: polls the block behind (opposite of player-facing side =
 * north by default; uses the block below when ambiguous) every 20 ticks and
 * caches one display line. Nixie tube / board TESRs read this line.
 * Modes: SPEED, STRESS, ITEMS, FLUID, BOILER, REDSTONE.
 */
public class TileEntityDisplayLink extends TileEntityOptimizedBase {
    public static final String[] MODENAMES = {"Speed", "Stress", "Items", "Fluid", "Boiler", "Redstone"};
    public int mode = 0;
    public boolean showLabel = true;
    public String line = "";

    @Override
    public void tick() {
        if (this.world != null && !this.world.isRemote && this.world.getTotalWorldTime() % 20 == 0) {
            String next = this.computeLine();
            if (!next.equals(this.line)) {
                this.line = next;
                this.sync();
            }
        }
    }

    @Override
    public void tickLazy() {
    }

    private String computeLine() {
        TileEntity src = this.source();
        String value = this.readValue(src);
        if (this.showLabel) return MODENAMES[this.mode % MODENAMES.length] + ": " + value;
        return value;
    }

    private TileEntity source() {
        if (this.world == null) return null;
        // Behind = north; fall back to below.
        BlockPos p = this.pos.north();
        if (this.world.isBlockLoaded(p) && this.world.getTileEntity(p) != null) return this.world.getTileEntity(p);
        p = this.pos.down();
        if (this.world.isBlockLoaded(p)) return this.world.getTileEntity(p);
        return null;
    }

    private String readValue(TileEntity src) {
        if (src == null) return "--";
        try {
            if (this.mode == 0 && src instanceof TileEntityKinetic) {
                float s = ((TileEntityKinetic) src).getSpeed();
                return ((int) s) + " RPM";
            }
            if (this.mode == 1 && src instanceof TileEntityKinetic) {
                TileEntityKinetic k = (TileEntityKinetic) src;
                return String.format("%.1f SU", k.stress);
            }
            if (this.mode == 2) {
                if (src instanceof net.minecraftforge.items.IItemHandler) {
                    int n = 0;
                    net.minecraftforge.items.IItemHandler h = (net.minecraftforge.items.IItemHandler) src;
                    for (int i = 0; i < h.getSlots(); i++) n += h.getStackInSlot(i).getCount();
                    return n + " items";
                }
                return "--";
            }
            if (this.mode == 3) {
                if (src instanceof TileEntityFluidTank) {
                    TileEntityFluidTank t = (TileEntityFluidTank) src;
                    net.minecraftforge.fluids.FluidStack f = t.getFluid();
                    if (f == null) return "empty";
                    return f.amount + " mB " + f.getLocalizedName();
                }
                return "--";
            }
            if (this.mode == 4) {
                if (src instanceof TileEntityFluidTank) {
                    TileEntityFluidTank t = (TileEntityFluidTank) src;
                    if (!t.isBoilerActive()) return "boiler off";
                    return "boiler Lv" + t.getBoilerLevel();
                }
                return "--";
            }
            if (this.mode == 5) {
                int p = this.world.getStrongPower(this.pos);
                return p + "/15";
            }
        } catch (Exception e) {
            return "err";
        }
        return "--";
    }

    public String displayLineFor(EnumFacing side) {
        return this.line.isEmpty() ? "--" : this.line;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("mode", this.mode);
        nbt.setBoolean("label", this.showLabel);
        nbt.setString("line", this.line);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.mode = nbt.getInteger("mode") % MODENAMES.length;
        this.showLabel = nbt.getBoolean("label");
        this.line = nbt.getString("line");
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeByte(this.mode);
        buf.writeBoolean(this.showLabel);
        byte[] b = this.line.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeInt(b.length);
        buf.writeBytes(b);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        this.mode = buf.readUnsignedByte() % MODENAMES.length;
        this.showLabel = buf.readBoolean();
        int n = buf.readInt();
        byte[] b = new byte[Math.min(n, 256)];
        buf.readBytes(b);
        if (n > 256) buf.skipBytes(n - 256);
        this.line = new String(b, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
