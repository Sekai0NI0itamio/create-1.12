package nl.melonstudios.create.block.deco;

import com.melonstudios.melonlib.misc.AABB;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.deco.TileEntityCopycat;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Copycat panel: a 3-pixel-thick sheet mounted on any face, storing a mimic
 * like the base block.
 *
 * Reference: CopycatPanelBlock (FACING on all six sides, 3px casing voxels,
 * trapdoor/bars special-cased materials). 1.12 simplification: plain 3px
 * panel voxels per face; only solid full-cube mimic targets are accepted, so
 * the trapdoor/bars special cases are NEEDS-LEAD.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockCopycatPanel extends Block implements ITileEntityProvider {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    private static final AxisAlignedBB DOWN_AABB = AABB.create(0, 0, 0, 16, 3, 16);
    private static final AxisAlignedBB UP_AABB = AABB.create(0, 13, 0, 16, 16, 16);
    private static final AxisAlignedBB NORTH_AABB = AABB.create(0, 0, 0, 16, 16, 3);
    private static final AxisAlignedBB SOUTH_AABB = AABB.create(0, 0, 13, 16, 16, 16);
    private static final AxisAlignedBB WEST_AABB = AABB.create(0, 0, 0, 3, 16, 16);
    private static final AxisAlignedBB EAST_AABB = AABB.create(13, 0, 0, 16, 16, 16);

    public BlockCopycatPanel() {
        super(Material.IRON, MapColor.LIGHT_BLUE);
        this.setRegistryName("copycat_panel");
        this.setUnlocalizedName("create.copycat_panel");
        this.setSoundType(SoundType.METAL);
        this.setHardness(1.5F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
        this.setCreativeTab(CreateTabs.TAB_CREATE_DECORATIONS);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.getFront(meta);
        return this.getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, facing);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case DOWN: return DOWN_AABB;
            case NORTH: return NORTH_AABB;
            case SOUTH: return SOUTH_AABB;
            case WEST: return WEST_AABB;
            case EAST: return EAST_AABB;
            case UP:
            default: return UP_AABB;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityCopycat();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntityCopycat copycat = CopycatMimicHelper.getCopycat(world, pos);
        if (copycat == null) return false;
        if (world.isRemote) return copycat.hasMimic() || CopycatMimicHelper.isValidMimicTarget(player.getHeldItem(hand));
        return CopycatMimicHelper.onActivated(world, pos, player, hand);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        CopycatMimicHelper.dropMimic(world, pos, state);
        super.breakBlock(world, pos, state);
    }
}
