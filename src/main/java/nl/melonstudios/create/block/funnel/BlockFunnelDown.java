package nl.melonstudios.create.block.funnel;

import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.state.EnumFunnelState;
import nl.melonstudios.create.tileentity.funnel.TileEntityFunnelBase;
import nl.melonstudios.create.tileentity.funnel.TileEntityFunnelDown;
import nl.melonstudios.create.tileentity.funnel.TileEntityFunnelDownAdvanced;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Standard down-facing funnel (the original mod's andesite_funnel/brass_funnel
 * with FACING=DOWN): hangs under a source inventory and outputs downward.
 * Facing is fixed DOWN, so the source side is always UP and the output side
 * is always DOWN. Wall mounting stays on BlockFunnelWall, untouched.
 */
@SuppressWarnings("deprecation")
public class BlockFunnelDown extends BlockFunnelBase {
    public static final PropertyEnum<EnumFunnelState> FUNNEL_STATE = EnumFunnelState.STATE_PROPERTY;

    public BlockFunnelDown(String set, boolean advanced) {
        super(set, advanced);
    }

    @Override
    protected void addStateProperties(List<IProperty<?>> properties) {
        super.addStateProperties(properties);
        properties.add(FUNNEL_STATE);
    }

    public static final AxisAlignedBB RIM = AABB.create(0, 8, 0, 16, 16, 16);
    public static final AxisAlignedBB SPOUT = AABB.create(6, 0, 6, 10, 8, 10);

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox,
                                      List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, RIM);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, SPOUT);
    }

    @Nullable
    @Override
    public TileEntityFunnelBase createNewTileEntity(World worldIn, int meta) {
        return this.isAdvanced ? new TileEntityFunnelDownAdvanced() : new TileEntityFunnelDown();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FUNNEL_STATE, (meta & 0b0100) != 0 ? EnumFunnelState.EXTRACTING : EnumFunnelState.INSERTING)
                .withProperty(POWERED, (meta & 0b1000) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(FUNNEL_STATE).getId() << 2) |
                (state.getValue(POWERED) ? 0b1000 : 0b0000);
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        // Reference canSurvive: a funnel may not hang off another funnel.
        return super.canPlaceBlockAt(worldIn, pos)
                && !(worldIn.getBlockState(pos.up()).getBlock() instanceof BlockFunnelBase);
    }
}
