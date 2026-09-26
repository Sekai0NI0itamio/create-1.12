package nl.melonstudios.create.block.deco;

import nl.melonstudios.create.init.CreateTabs;
import com.melonstudios.melonlib.misc.AABB;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.deco.TileEntityCopycat;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Copycat step: a half-height slab with a horizontal facing, storing a mimic
 * like the base block.
 *
 * Reference: CopycatStepBlock (HALF + HORIZONTAL_FACING, stair-step voxels,
 * placement-helper chaining). 1.12 simplification: the voxel is a plain
 * bottom/top half slab; the facing is kept for placement chaining parity and
 * for the future baked mimic model. Full stair geometry is NEEDS-LEAD.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockCopycatStep extends Block implements ITileEntityProvider {
    public enum StepHalf implements IStringSerializable {
        BOTTOM("bottom"),
        TOP("top");

        private final String name;
        StepHalf(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyEnum<StepHalf> HALF = PropertyEnum.create("half", StepHalf.class);

    private static final AxisAlignedBB BOTTOM_AABB = AABB.create(0, 0, 0, 16, 8, 16);
    private static final AxisAlignedBB TOP_AABB = AABB.create(0, 8, 0, 16, 16, 16);

    public BlockCopycatStep() {
        super(Material.IRON, MapColor.LIGHT_BLUE);
        this.setRegistryName("copycat_step");
        this.setUnlocalizedName("create.copycat_step");
        this.setSoundType(SoundType.METAL);
        this.setHardness(1.5F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(HALF, StepHalf.BOTTOM));
        this.setCreativeTab(CreateTabs.TAB_CREATE_DECORATIONS);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, HALF);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(HALF) == StepHalf.TOP) meta |= 4;
        return meta;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
                .withProperty(HALF, (meta & 4) != 0 ? StepHalf.TOP : StepHalf.BOTTOM);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        StepHalf half = (facing == EnumFacing.DOWN || (hitY > 0.5 && facing != EnumFacing.UP))
                ? StepHalf.TOP : StepHalf.BOTTOM;
        EnumFacing forward = placer.getHorizontalFacing().getOpposite();
        return this.getDefaultState().withProperty(FACING, forward).withProperty(HALF, half);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(HALF) == StepHalf.TOP ? TOP_AABB : BOTTOM_AABB;
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
