package nl.melonstudios.create.block.actor;

import com.melonstudios.melonlib.misc.AABB;
import com.melonstudios.melonlib.misc.BlockStateProperties;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticRotatedPillarBase;
import nl.melonstudios.create.block.state.EnumBeltPart;
import nl.melonstudios.create.extensions.IExtensionBlock;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.tileentity.actor.TileEntityBeltBase;
import nl.melonstudios.create.tileentity.actor.TileEntityBeltStraight;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockBeltStraight extends BlockBeltBase implements IExtensionBlock {
    private static final AxisAlignedBB DEFAULT_AABB = AABB.create(0, 4, 0, 16, 12, 16);
    private static final AxisAlignedBB VERTICAL_X_AABB = AABB.create(4, 0, 0, 12, 16, 16);
    private static final AxisAlignedBB VERTICAL_Z_AABB = AABB.create(0, 0, 4, 16, 16, 12);

    public static final PropertyEnum<EnumFacing.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final PropertyBool VERTICAL = PropertyBool.create("vertical");
    public BlockBeltStraight() {
        super();

        this.setDefaultState(this.getDefaultState()
                .withProperty(PART, EnumBeltPart.MIDDLE)
                .withProperty(VERTICAL, false)
                .withProperty(AXIS, EnumFacing.Axis.X)
        );
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, PART, VERTICAL, AXIS);
    }

    @Nullable
    @Override
    public TileEntityBeltBase createNewTileEntity(World worldIn, int meta) {
        return new TileEntityBeltStraight();
    }

    @Override
    public EnumFacing.Axis getTransportAxis(IBlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean isFunctional(IBlockState state) {
        return !state.getValue(VERTICAL);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing.Axis axis = ((meta >> 3) & 1) != 0 ? EnumFacing.Axis.Z : EnumFacing.Axis.X;
        boolean vertical = ((meta >> 2) & 1) != 0;
        EnumBeltPart part = EnumBeltPart.byId(meta & 3);
        return this.getDefaultState()
                .withProperty(PART, part)
                .withProperty(VERTICAL, vertical)
                .withProperty(AXIS, axis);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(PART).getId() | ((state.getValue(VERTICAL) ? 1 : 0) << 2) | ((state.getValue(AXIS) != EnumFacing.Axis.X ? 1 : 0) << 3);
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return state.getValue(AXIS) == EnumFacing.Axis.X ? EnumFacing.Axis.Z : EnumFacing.Axis.X;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(VERTICAL) ? (state.getValue(AXIS) == EnumFacing.Axis.X ? VERTICAL_X_AABB : VERTICAL_Z_AABB) : DEFAULT_AABB;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        if (!this.isFunctional(state)) return;
        super.onEntityCollidedWithBlock(worldIn, pos, state, entityIn);

        if (entityIn.onGround && entityIn.isEntityAlive() && !entityIn.isSneaking() && !(entityIn instanceof EntityItem)) {
            // Reference BeltMovementHandler.canBeTransported + transportEntity:
            // flying players ride nothing, non-player living are slowed while
            // riding so they do not outrun the belt.
            if (entityIn instanceof EntityPlayer && ((EntityPlayer) entityIn).capabilities.isFlying) return;
            if (entityIn instanceof EntityLivingBase && !(entityIn instanceof EntityPlayer)) {
                ((EntityLivingBase) entityIn).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 10, 1, false, false));
            }
            if (entityIn.posY > 0.7 + pos.getY() && entityIn.posY < 0.8 + pos.getY()) {
                TileEntity te = worldIn.getTileEntity(pos);
                if (te instanceof TileEntityBeltBase) {
                    TileEntityBeltBase belt = (TileEntityBeltBase) te;
                    // Blocks per tick, matching the item rate above and the reference
                    // BeltMovementHandler movement speed (speed/480).
                    double speed = belt.getSpeed() / 480.0;
                    if (speed != 0.0) {
                        EnumFacing.Axis axis = state.getValue(AXIS);
                        EnumFacing facing = axis == EnumFacing.Axis.X ? EnumFacing.WEST : EnumFacing.SOUTH;
                        entityIn.move(MoverType.SHULKER_BOX, facing.getFrontOffsetX() * speed, 0.0, facing.getFrontOffsetZ() * speed);
                    }
                }
            }
        }
    }

    /**
     * Reference BeltBlock.onRemove replaces a cascaded PULLEY segment with its
     * shaft block (axis kept) instead of air, so the kinetic network keeps its
     * support bracket. The neighbour TE's shaft-item refund is suppressed via
     * dropShaftOnDestroy to avoid duplicating block + item.
     */
    private void removeChainedSegment(World world, BlockPos off) {
        IBlockState old = world.getBlockState(off);
        if (old.getBlock() != this) return;
        if (old.getValue(PART) == EnumBeltPart.PULLEY) {
            TileEntity te = world.getTileEntity(off);
            if (te instanceof TileEntityBeltBase) {
                ((TileEntityBeltBase) te).dropShaftOnDestroy = false;
            }
            world.setBlockState(off, BlockInit.SHAFT.getDefaultState()
                    .withProperty(BlockKineticRotatedPillarBase.AXIS, this.getRotationAxis(old)), 3);
        } else {
            world.setBlockState(off, Blocks.AIR.getDefaultState(), 3);
        }
        world.playEvent(2001, off, Block.getStateId(old));
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);

        EnumBeltPart part = state.getValue(PART);

        if (state.getValue(VERTICAL)) {
            if (part != EnumBeltPart.END) {
                this.removeChainedSegment(worldIn, pos.up());
            }
            if (part != EnumBeltPart.START) {
                this.removeChainedSegment(worldIn, pos.down());
            }
        } else {
            EnumFacing.Axis axis = state.getValue(AXIS);
            EnumFacing p = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, axis);
            EnumFacing n = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.NEGATIVE, axis);

            if (part != EnumBeltPart.END) {
                this.removeChainedSegment(worldIn, pos.offset(p));
            }
            if (part != EnumBeltPart.START) {
                this.removeChainedSegment(worldIn, pos.offset(n));
            }
        }
        // Reference initBelt: chains shorter than 2 segments cannot exist.
        // Pulley-to-shaft splits above can orphan singletons; pop them.
        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos npos = pos.offset(side);
            if (worldIn.getBlockState(npos).getBlock() != this) {
                continue;
            }
            List<BlockPos> chain = BeltSlicer.getChain(worldIn, npos);
            if (chain.size() < 2) {
                for (BlockPos p : chain) {
                    worldIn.destroyBlock(p, true);
                }
            }
        }
    }

    @Override
    public void create$addStickyLocations(World world, BlockPos pos, IBlockState state, List<BlockPos> positions) {
        EnumBeltPart part = state.getValue(PART);

        if (state.getValue(VERTICAL)) {
            if (part != EnumBeltPart.END) positions.add(pos.add(0, 1, 0));
            if (part != EnumBeltPart.START) positions.add(pos.add(0, -1, 0));
        } else {
            if (state.getValue(AXIS) == EnumFacing.Axis.X) {
                if (part != EnumBeltPart.END) positions.add(pos.add(1, 0, 0));
                if (part != EnumBeltPart.START) positions.add(pos.add(-1, 0, 0));
            } else {
                if (part != EnumBeltPart.END) positions.add(pos.add(0, 0, 1));
                if (part != EnumBeltPart.START) positions.add(pos.add(0, 0, -1));
            }
        }
    }
}
