package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.init.RecipeInit;
import nl.melonstudios.create.recipe.MixingRecipe;
import nl.melonstudios.create.recipe.server.MixingRecipes;
import nl.melonstudios.create.tileentity.TileEntityBasin;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.tileentity.marker.ISpeedRequirement;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TileEntityMixer extends TileEntityKinetic implements ISpeedRequirement {
    public TileEntityMixer() {
        super();

        this.setTickRateLazy(5);
    }

    public final BlockPos.MutableBlockPos basinPos = new BlockPos.MutableBlockPos();
    public TileEntityBasin cachedBasin = null;
    public int loweringOld = 0;
    public int lowering = 0;
    public int progress = 0;
    public String currentRecipe = null;

    @Override
    public void tick() {
        this.loweringOld = this.lowering;
        this.markDirty();
        super.tick();

        TileEntityBasin basin = this.getBasin();
        if (basin == null) this.currentRecipe = null;
        if (this.currentRecipe != null) {
            MixingRecipe recipe = RecipeInit.getMixingRecipes(this.world.isRemote).getRecipe(this.currentRecipe);
            if (recipe == null || !recipe.matches(basin)) {
                this.currentRecipe = null;
                this.sync();
            }
        }
        MixingRecipe recipe = this.currentRecipe != null ? this.getRecipe() : null;
        boolean process = recipe != null && recipe.checkOutputSpace(basin);
        if (process) {
            if (this.lowering < 20) this.lowering++;
        } else {
            this.progress = 0;
            if (this.lowering > 0) this.lowering--;
        }

        if (this.lowering >= 20 && recipe != null && basin != null) {
            this.recipeFX(basin, this.basinPos.getX() + 0.5, this.basinPos.getY() + 0.5, this.basinPos.getZ() + 0.5);
            basin.addedItemRotation += (int)(this.getSpeed() * 0.3);
            if ((this.progress += (int) this.getSpeed()) >= recipe.processingTime) {
                this.progress = 0;
                if (recipe.removeRequiredInput(basin)) {
                    basin.dumpRecipeResults(recipe);
                } else throw new RuntimeException("Recipe went wrong! pls fix");
                this.sync();
            }
        }
    }

    @Override
    public void tickLazy() {
        if (this.currentRecipe == null && this.getSpeed() >= this.minimumSpeed() && this.getBasin() != null) {
            this.searchRecipe(this.getBasin());
        }
    }
    private void searchRecipe(TileEntityBasin basin) {
        this.currentRecipe = MixingRecipes.getRecipeForInput(basin, this.world.isRemote);
    }

    private void recipeFX(TileEntityBasin basin, double x, double y, double z) {
        if (this.world.isRemote) {
            if ((this.world.getTotalWorldTime() & 1) == 0) {
                CreateLegacy.proxy.mixerFX(basin, x, y, z);
            }
        }
    }

    @Nullable
    public TileEntityBasin getBasin() {
        this.basinPos.setPos(this.pos).move(EnumFacing.DOWN, 2);
        if (this.cachedBasin == null || this.cachedBasin.isInvalid() || !this.cachedBasin.getPos().equals(this.basinPos)) {
            this.cachedBasin = Utils.cast(this.world.getTileEntity(this.basinPos), TileEntityBasin.class);
        }
        return this.cachedBasin;
    }
    @Nullable
    public MixingRecipe getRecipe() {
        return RecipeInit.getMixingRecipes(this.world.isRemote).getRecipe(this.currentRecipe);
    }

    @Override
    public float minimumSpeed() {
        return 0.0F;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        if (this.lowering != 0) nbt.setInteger("lower", this.lowering);
        if (this.progress != 0) nbt.setInteger("progress", this.progress);
        if (this.currentRecipe != null) nbt.setString("recipe", this.currentRecipe);

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        this.lowering = nbt.getInteger("lower");
        this.progress = nbt.getInteger("progress");
        if (nbt.hasKey("recipe")) this.currentRecipe = nbt.getString("recipe");
        else this.currentRecipe = null;
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);

        buf.writeInt(this.lowering);
        buf.writeInt(this.progress);

        if (this.currentRecipe != null) {
            buf.writeInt(this.currentRecipe.length());
            buf.internal().writeCharSequence(this.currentRecipe, StandardCharsets.UTF_8);
            buf.append(this.currentRecipe.length());
        } else {
            buf.writeInt(0);
        }
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);

        this.lowering = buf.readInt();
        this.progress = buf.readInt();

        int len = buf.readInt();
        if (len > 0) {
            this.currentRecipe = buf.readCharSequence(len, StandardCharsets.UTF_8).toString();
        } else this.currentRecipe = null;
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
