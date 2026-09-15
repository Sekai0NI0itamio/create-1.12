package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.BlockStateProperties;
import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.network.TrackedByteBuf;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.actor.BlockCrushingWheel;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.DamageSourceInit;
import nl.melonstudios.create.recipe.PulverizationRecipe;
import nl.melonstudios.create.recipe.server.CrushingRecipes;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.util.Utils;

import java.io.IOException;
import java.util.List;

/**
 * Crushing wheel pair controller, merged into the wheel TE (no separate
 * controller block in the backport: each wheel checks its neighbor and the
 * leftmost/lowest wheel of a valid pair runs the crushing).
 *
 * Official rules mirrored:
 * - Two wheels on the same horizontal axis, adjacent along it, spinning
 *   opposite directions (signs differ, neither zero).
 * - Processing speed scales with |speed|/50 (controller crushingspeed).
 * - Items are pulled from EntityItems above the gap; outputs eject downward
 *   (or onto a depot/belt below).
 * - Living things caught in the gap take crushing damage.
 */
public class TileEntityCrushingWheel extends TileEntityKinetic {
    public ItemStack input = ItemStack.EMPTY;
    public int timer;
    private PulverizationRecipe lastRecipe = null;
    private int validPairDir = 0;

    private static final int[][] OFFSETS = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

    @SideOnly(Side.CLIENT)
    public EnumFacing.Axis getRenderAxis() {
        return this.getState().getValue(BlockStateProperties.HORIZONTAL_AXIS);
    }

    /** Am I the primary wheel of a valid counter-rotating pair? */
    private boolean findPair() {
        validPairDir = 0;
        if (this.getSpeed() == 0) return false;
        EnumFacing.Axis axis;
        try {
            axis = this.getState().getValue(BlockStateProperties.HORIZONTAL_AXIS);
        } catch (Exception e) {
            return false;
        }
        for (int[] o : OFFSETS) {
            EnumFacing.Axis offAxis = o[0] != 0 ? EnumFacing.Axis.X : EnumFacing.Axis.Z;
            if (offAxis != axis) continue;
            BlockPos npos = this.pos.add(o[0], o[1], o[2]);
            if (!(this.world.getBlockState(npos).getBlock() instanceof BlockCrushingWheel)) continue;
            EnumFacing.Axis naxis;
            try {
                naxis = this.world.getBlockState(npos).getValue(BlockStateProperties.HORIZONTAL_AXIS);
            } catch (Exception e) {
                continue;
            }
            if (naxis != axis) continue;
            if (!(this.world.getTileEntity(npos) instanceof TileEntityCrushingWheel)) continue;
            TileEntityCrushingWheel other = (TileEntityCrushingWheel) this.world.getTileEntity(npos);
            if (other.getSpeed() == 0) continue;
            if ((this.getSpeed() > 0) == (other.getSpeed() > 0)) continue;
            // Primary = the wheel with the lower coordinate along the axis.
            int mine = axis == EnumFacing.Axis.X ? this.pos.getX() : this.pos.getZ();
            int theirs = axis == EnumFacing.Axis.X ? npos.getX() : npos.getZ();
            if (mine < theirs) {
                validPairDir = o[0] != 0 ? (o[0] > 0 ? 1 : -1) : (o[2] > 0 ? 2 : -2);
                return true;
            }
            return false;
        }
        return false;
    }

    public float crushingSpeed() {
        return Math.abs(this.getSpeed() / 50.0F);
    }

    public int getProcessingSpeed() {
        return MathHelper.clamp((int) Math.abs(this.getSpeed() / 8.0F), 1, 512);
    }

    @Override
    public void tick() {
        super.tick();

        if (!findPair()) {
            this.timer = 0;
            return;
        }
        if (this.crushingSpeed() == 0) return;

        // Hurt living things caught between the wheels.
        AxisAlignedBB gap = new AxisAlignedBB(this.pos).grow(0.6, 0.2, 0.6);
        List<EntityLivingBase> victims = this.world.getEntitiesWithinAABB(EntityLivingBase.class, gap,
                e -> e != null && e.isEntityAlive());
        for (EntityLivingBase victim : victims) {
            DamageSource src = DamageSourceInit.CRUSHING;
            victim.attackEntityFrom(src, 4.0F);
        }

        if (this.timer > 0) {
            this.timer -= this.getProcessingSpeed();
            if (this.world.isRemote) return;
            if (this.timer <= 0) this.process();
            this.markDirty();
            return;
        }

        if (this.input.isEmpty()) {
            // Pull one loose item from above the gap.
            List<EntityItem> items = this.world.getEntitiesWithinAABB(EntityItem.class,
                    new AxisAlignedBB(this.pos.up()).grow(0.4, 0.6, 0.4),
                    EntityItem::isEntityAlive);
            if (items.isEmpty()) return;
            EntityItem ei = items.get(0);
            ItemStack stack = ei.getItem();
            PulverizationRecipe recipe = CrushingRecipes.instance.getRecipeForInput(stack);
            if (recipe == null) return;
            this.lastRecipe = recipe;
            this.input = stack.copy();
            this.input.setCount(1);
            stack.shrink(1);
            if (stack.isEmpty()) ei.setDead();
            else ei.setItem(stack);
            this.timer = recipe.processingTime;
            this.sync();
            return;
        }

        if (this.lastRecipe == null || !this.lastRecipe.input.test(this.input)) {
            PulverizationRecipe recipe = CrushingRecipes.instance.getRecipeForInput(this.input);
            if (recipe == null) {
                this.input = ItemStack.EMPTY;
                return;
            }
            this.lastRecipe = recipe;
        }
        this.timer = this.lastRecipe.processingTime;
        this.sync();
    }

    private void process() {
        if (this.lastRecipe == null || !this.lastRecipe.input.test(this.input)) {
            PulverizationRecipe recipe = CrushingRecipes.instance.getRecipeForInput(this.input);
            if (recipe == null) {
                this.input = ItemStack.EMPTY;
                return;
            }
            this.lastRecipe = recipe;
        }
        this.input = ItemStack.EMPTY;
        double x = this.pos.getX() + 0.5;
        double y = this.pos.getY() - 0.4;
        double z = this.pos.getZ() + 0.5;
        for (ItemStack stack : Utils.rollChancedResults(this.lastRecipe.results)) {
            if (stack.isEmpty()) continue;
            if (!this.world.isRemote) {
                StackUtil.spawnItemNoVelocity(this.world, x, y, z, stack);
            }
        }
        this.world.playEvent(2001, this.pos, net.minecraft.block.Block.getStateId(
                net.minecraft.init.Blocks.COBBLESTONE.getDefaultState()));
        this.sync();
    }

    @Override
    public void destroy() {
        super.destroy();
        StackUtil.dropItemsAt(this.world, this.pos, this.input);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (!this.input.isEmpty()) nbt.setTag("Input", this.input.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("timer", this.timer);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("Input", 10)) this.input = new ItemStack(nbt.getCompoundTag("Input"));
        else this.input = ItemStack.EMPTY;
        this.timer = nbt.getInteger("timer");
    }

    @Override
    public void writePacket(TrackedByteBuf buf) throws IOException {
        super.writePacket(buf);
        buf.writeBoolean(!this.input.isEmpty());
        if (!this.input.isEmpty()) StackUtil.writeItemStack(this.input, buf, true, true);
        buf.writeInt(this.timer);
    }

    @Override
    public void readPacket(ByteBuf buf) throws IOException {
        super.readPacket(buf);
        if (buf.readBoolean()) this.input = StackUtil.readItemStack(buf, true, true);
        else this.input = ItemStack.EMPTY;
        this.timer = buf.readInt();
    }

    @Override
    protected AxisAlignedBB createRenderBoundingBox() {
        return AABB.wrap(this.pos, 1);
    }
}
