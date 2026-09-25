package nl.melonstudios.create.tileentity.fluid;

import com.melonstudios.melonlib.misc.AABB;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;

import javax.annotation.Nullable;

/**
 * Per-pipe state for the fluid transfer graph.
 *
 * <p>The pipe itself stores no fluid: it is a relay. Each tile keeps only
 * the state the network needs to stay consistent:</p>
 * <ul>
 * <li>{@code cooldown} — single-driver claim; the pipe that runs a walk
 * stamps every visited pipe so the graph moves once per interval.</li>
 * <li>{@code marker} — fluid type currently in flight on this graph;
 * blocks mixing while set (reference: one fluid per network, collisions
 * rejected).</li>
 * <li>{@code filter} — cheap smart-pipe filtering: an allow-listed fluid
 * name, or null for "any". No UI yet; set by the smart-pipe block.</li>
 * <li>{@code pumpPressure} — override from a future pump block; -1 keeps
 * the passive pressure from {@link FluidPipeNetwork}.</li>
 * </ul>
 *
 * <p>Sync is minimal on purpose: clients only need the in-flight fluid
 * type for rendering. Amounts, cooldowns and pressure stay server side.</p>
 */
public class TileEntityFluidPipe extends TileEntityOptimizedBase {
    /** Ticks until this pipe may drive a walk again. */
    int cooldown = 0;
    private int setupDelay = FluidPipeNetwork.SETUP_DELAY;
    private int pumpPressure = -1;
    /** Allowed fluid name, or null for any. */
    private String filter;
    /** In-flight fluid marker (amount always 1; type only). */
    private FluidStack marker;

    public TileEntityFluidPipe() {
        super();
        this.setTickRateLazy(20);
    }

    @Override
    public void tick() {
        if (this.world == null || this.world.isRemote) return;
        if (this.setupDelay > 0) this.setupDelay--;
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        if (this.setupDelay > 0) {
            this.cooldown = 1;
            return;
        }
        FluidPipeNetwork.transferTick(this.world, this);
        this.markDirty();
    }

    @Override
    public void tickLazy() {
    }

    /** Called by the network after fluid actually moved (hook for particles/sound). */
    void onFluidMoved() {
        this.sync();
    }

    // ---- network-facing state ----

    int getPumpPressure() {
        return this.pumpPressure;
    }

    /** Pump worker override; -1 restores passive pressure. */
    public void setPumpPressure(int pressure) {
        this.pumpPressure = pressure;
        this.markDirty();
    }

    @Nullable
    FluidStack getMarker() {
        return this.marker;
    }

    void setMarker(@Nullable FluidStack marker) {
        boolean changed = (this.marker == null) != (marker == null)
                || (this.marker != null && !this.marker.isFluidEqual(marker));
        this.marker = marker == null ? null : new FluidStack(marker.getFluid(), 1);
        if (changed) this.sync();
    }

    /** Cheap smart-pipe filter: null fluid clears the filter. */
    public void setFilter(@Nullable Fluid fluid) {
        this.filter = fluid == null ? null : fluid.getName();
        this.markDirty();
        this.sync();
    }

    @Nullable
    public String getFilterName() {
        return this.filter;
    }

    /** True when this pipe may carry the fluid (null = dry sample probe). */
    public boolean matchesFilter(@Nullable FluidStack fluid) {
        if (this.filter == null) return true;
        if (fluid == null) return true;
        return fluid.getFluid() != null && this.filter.equals(fluid.getFluid().getName());
    }

    // ---- persistence ----

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("PumpPressure", this.pumpPressure);
        if (this.filter != null) nbt.setString("Filter", this.filter);
        if (this.marker != null) {
            NBTTagCompound m = new NBTTagCompound();
            this.marker.writeToNBT(m);
            nbt.setTag("Marker", m);
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.pumpPressure = nbt.hasKey("PumpPressure", 3) ? nbt.getInteger("PumpPressure") : -1;
        this.filter = nbt.hasKey("Filter", 8) ? nbt.getString("Filter") : null;
        if (nbt.hasKey("Marker", 10)) {
            this.marker = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("Marker"));
        } else {
            this.marker = null;
        }
        if (this.filter != null && FluidRegistry.getFluid(this.filter) == null) {
            this.filter = null;
        }
    }

    // ---- minimal sync: in-flight fluid type only ----

    @Override
    public NBTTagCompound writePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        if (this.marker != null && this.marker.getFluid() != null) {
            nbt.setString("Fluid", this.marker.getFluid().getName());
        }
        return nbt;
    }

    @Override
    public void readPacket(NBTTagCompound nbt) {
        if (nbt.hasKey("Fluid", 8)) {
            Fluid f = FluidRegistry.getFluid(nbt.getString("Fluid"));
            this.marker = f == null ? null : new FluidStack(f, 1);
        } else {
            this.marker = null;
        }
    }

    @Override
    public void writePacket(com.melonstudios.melonlib.network.TrackedByteBuf buf) throws java.io.IOException {
        io.netty.buffer.ByteBuf temp = Unpooled.buffer();
        ByteBufUtils.writeTag(temp, this.writePacket());
        buf.writeBytes(temp);
    }

    @Override
    public void readPacket(io.netty.buffer.ByteBuf buf) throws java.io.IOException {
        this.readPacket(ByteBufUtils.readTag(buf));
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
