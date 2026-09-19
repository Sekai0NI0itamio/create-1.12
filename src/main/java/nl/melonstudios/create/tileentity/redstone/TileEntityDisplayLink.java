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
            // Like the original (tickSource returns early while POWERED): a powered link pauses gathering.
            if (this.isPausedByRedstone()) return;
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

    private boolean isPausedByRedstone() {
        try {
            net.minecraft.block.state.IBlockState state = this.world.getBlockState(this.pos);
            if (state.getBlock() instanceof nl.melonstudios.create.block.redstone.BlockDisplayLink) {
                return state.getValue(nl.melonstudios.create.block.redstone.BlockDisplayLink.POWERED);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private BlockPos sourcePos() {
        if (this.world == null) return null;
        // The link reads the block it was placed against (opposite of its facing).
        try {
            net.minecraft.block.state.IBlockState state = this.world.getBlockState(this.pos);
            if (state.getBlock() instanceof nl.melonstudios.create.block.redstone.BlockDisplayLink) {
                EnumFacing facing = state.getValue(nl.melonstudios.create.block.redstone.BlockDisplayLink.FACING);
                return this.pos.offset(facing.getOpposite());
            }
        } catch (Exception ignored) {
        }
        // Fallback for legacy placements: behind = north.
        return this.pos.north();
    }

    private TileEntity source() {
        BlockPos p = this.sourcePos();
        if (p == null || !this.world.isBlockLoaded(p)) return null;
        TileEntity te = this.world.getTileEntity(p);
        if (te != null) return te;
        // Fallback for legacy placements: below.
        p = this.pos.down();
        if (this.world.isBlockLoaded(p)) return this.world.getTileEntity(p);
        return null;
    }

    private String readValue(TileEntity src) {
        try {
            // Redstone sources (dust, levers, torches) have no tile entity, so handle
            // this mode before the null check, reading the signal at the source block.
            if (this.mode == 5) {
                BlockPos p = this.sourcePos();
                if (p == null || !this.world.isBlockLoaded(p)) return "--";
                int power = this.world.getStrongPower(p);
                return power + "/15";
            }
            if (src == null) return "--";
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

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("mode", this.mode);
        nbt.setBoolean("label", this.showLabel);
        nbt.setString("line", this.line);
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        this.mode = nbt.getInteger("mode") % MODENAMES.length;
        this.showLabel = nbt.getBoolean("label");
        this.line = nbt.getString("line");
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        io.netty.buffer.ByteBuf temp = io.netty.buffer.Unpooled.buffer();
        net.minecraftforge.fml.common.network.ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        this.readPacket(net.minecraftforge.fml.common.network.ByteBufUtils.readTag(buf));
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
