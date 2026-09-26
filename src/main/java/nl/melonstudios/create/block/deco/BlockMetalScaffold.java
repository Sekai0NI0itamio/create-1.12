package nl.melonstudios.create.block.deco;

import nl.melonstudios.create.init.CreateTabs;
import com.melonstudios.melonlib.misc.AABB;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Metal scaffolding for andesite / brass / copper.
 *
 * Reference: MetalScaffoldingBlock (vanilla ScaffoldingBlock behavior:
 * DISTANCE support checks, BOTTOM half-collision, climbable, connected
 * textures). 1.12 has no vanilla scaffolding, so this port keeps the
 * load-bearing behaviors in 1.12 idioms: climbable via isLadder, BOTTOM flag
 * recomputed from the block below (half-height collision when the platform
 * sits on solid ground, full frame otherwise), and it never collapses from
 * distance — distance-based decay is NEEDS-LEAD. Connected-texture merging is
 * NEEDS-LEAD client work.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockMetalScaffold extends Block {
    public static final PropertyBool BOTTOM = PropertyBool.create("bottom");

    private static final AxisAlignedBB FULL_AABB = AABB.create(0, 0, 0, 16, 16, 16);
    private static final AxisAlignedBB BOTTOM_AABB = AABB.create(0, 8, 0, 16, 16, 16);

    private final String metal;

    public BlockMetalScaffold(String metal) {
        super(Material.IRON);
        this.metal = metal;
        this.setRegistryName(metal + "_scaffolding");
        this.setUnlocalizedName("create." + metal + "_scaffolding");
        this.setSoundType(metal.equals("andesite") ? SoundType.STONE : SoundType.METAL);
        this.setHardness(1.0F);
        this.setResistance(4.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setDefaultState(this.blockState.getBaseState().withProperty(BOTTOM, false));
        this.setCreativeTab(CreateTabs.TAB_CREATE_DECORATIONS);
    }

    public String getMetal() {
        return this.metal;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BOTTOM);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(BOTTOM) ? 1 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(BOTTOM, (meta & 1) != 0);
    }

    private static boolean sitsOnSupport(World world, BlockPos pos) {
        IBlockState below = world.getBlockState(pos.down());
        if (below.getBlock() instanceof BlockMetalScaffold) return false;
        return below.isTopSolid();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(BOTTOM, sitsOnSupport(world, pos));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        boolean bottom = sitsOnSupport(world, pos);
        if (state.getValue(BOTTOM) != bottom) {
            world.setBlockState(pos, state.withProperty(BOTTOM, bottom), 3);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(BOTTOM) ? BOTTOM_AABB : FULL_AABB;
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(BOTTOM) ? BOTTOM_AABB : FULL_AABB;
    }

    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return true;
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
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        switch (this.metal) {
            case "brass": return MapColor.GOLD;
            case "copper": return MapColor.ADOBE;
            case "andesite":
            default: return MapColor.STONE;
        }
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
