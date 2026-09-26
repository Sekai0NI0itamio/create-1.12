package nl.melonstudios.create.block.actor;

import com.melonstudios.melonlib.item.IMetaName;
import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.actor.TileEntityHarvester;
import nl.melonstudios.create.tileentity.actor.TileEntityPlough;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class BlockAutoFarm extends BlockHorizontal implements ITileEntityProvider, IMetaName {
    public enum Variant implements IStringSerializable {
        PLOUGH,
        HARVESTER;

        private final String name = this.toString().toLowerCase(Locale.ENGLISH);
        private final int id = this.ordinal();

        @Override
        public String getName() {
            return this.name;
        }
        public int getID() {
            return this.id;
        }

        public static final Variant[] VALUES = {
                PLOUGH, HARVESTER,
        };
        public static Variant byID(int id) {
            return VALUES[id];
        }
    }

    public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

    public BlockAutoFarm() {
        super(Material.ROCK, MapColor.IRON);
        this.setSoundType(SoundType.STONE);

        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);

        this.setHarvestLevel("pickaxe", 0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, VARIANT);
    }

    //region this is not a full block
    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isTranslucent(IBlockState state) {
        return true;
    }

    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
    //endregion

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return (meta & 1) == 0 ? new TileEntityPlough() : new TileEntityHarvester();
    }
    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "tile.create." + Variant.byID(stack.getMetadata() & 1).getName();
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getID();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getID() | (state.getValue(FACING).getHorizontalIndex() << 1);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, Variant.byID(meta & 1)).withProperty(FACING, EnumFacing.HORIZONTALS[(meta >> 1) & 3]);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        Variant variant = Variant.byID(meta & 1);
        if (facing.getAxis() == EnumFacing.Axis.Y) {
            return this.getDefaultState()
                    .withProperty(VARIANT, variant)
                    .withProperty(FACING, placer.getHorizontalFacing().getOpposite());
        }
        return this.getDefaultState()
                .withProperty(VARIANT, variant)
                .withProperty(FACING, facing);
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, 0));
        items.add(new ItemStack(this, 1, 1));
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public EnumFacing[] getValidRotations(World world, BlockPos pos) {
        return EnumFacing.HORIZONTALS;
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withProperty(FACING, mirrorIn.mirror(state.getValue(FACING)));
    }

    private static final AxisAlignedBB[] PLOUGH_AABB = {
            AABB.create(0.0, 0.0, 0.0, 16.0, 12.0, 15.0),
            AABB.create(1.0, 0.0, 0.0, 16.0, 12.0, 16.0),
            AABB.create(0.0, 0.0, 1.0, 16.0, 12.0, 16.0),
            AABB.create(0.0, 0.0, 0.0, 15.0, 12.0, 16.0)
    };
    private static final AxisAlignedBB[] HARVESTER_AABB = {
            AABB.create(0.0, 0.0, 0.0, 16.0, 14.0, 15.0),
            AABB.create(1.0, 0.0, 0.0, 16.0, 14.0, 16.0),
            AABB.create(0.0, 0.0, 1.0, 16.0, 14.0, 16.0),
            AABB.create(0.0, 0.0, 0.0, 15.0, 14.0, 16.0)
    };

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(VARIANT) == Variant.PLOUGH ?
                PLOUGH_AABB[state.getValue(FACING).getHorizontalIndex()] :
                HARVESTER_AABB[state.getValue(FACING).getHorizontalIndex()];
    }
}
