package nl.melonstudios.create.block.actor;

import nl.melonstudios.create.init.CreateTabs;
import com.melonstudios.melonlib.item.IMetaName;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.actor.TileEntityContraptionInterfaceBase;
import nl.melonstudios.create.tileentity.actor.TileEntityPortableFluidInterface;
import nl.melonstudios.create.tileentity.actor.TileEntityStorageInterface;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class BlockContraptionInterface extends BlockDirectional implements ITileEntityProvider, IWrenchable, IMetaName {
    public enum Variant implements IStringSerializable {
        STORAGE, FLUID;

        private final String name = this.toString().toLowerCase(Locale.ENGLISH);
        private final int id = this.ordinal();

        @Override
        public String getName() {
            return this.name;
        }
        public int getID() {
            return this.id;
        }

        private static final Variant[] VARIANTS = {STORAGE, FLUID};
        public static Variant byID(int id) {
            return VARIANTS[id & 1];
        }
    }

    public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

    public BlockContraptionInterface() {
        super(Material.ROCK);
        this.setSoundType(SoundType.STONE);
        this.fullBlock = false;
        this.translucent = true;

        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);

        this.setHarvestLevel("pickaxe", 0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, VARIANT);
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || "axe".equals(type);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getID() | (state.getValue(FACING).getIndex() << 1);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(VARIANT, Variant.byID(meta))
                .withProperty(FACING, EnumFacing.VALUES[(meta >> 1) % 6]);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getID();
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "tile.create.interface_" + Variant.byID(stack.getMetadata()).getName();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, 0));
        items.add(new ItemStack(this, 1, 1));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        Variant variant = Variant.byID(meta);
        // Reference: direction = nearest looking direction (all 6 sides);
        // shift inverts; stored FACING is the opposite of that direction.
        Vec3d look = placer.getLookVec();
        EnumFacing direction = EnumFacing.getFacingFromVector((float) look.x, (float) look.y, (float) look.z);
        if (placer.isSneaking()) direction = direction.getOpposite();
        return this.getDefaultState().withProperty(VARIANT, variant)
                .withProperty(FACING, direction.getOpposite());
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return (meta & 1) != 0 ? new TileEntityPortableFluidInterface() : new TileEntityStorageInterface();
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityContraptionInterfaceBase) {
            ((TileEntityContraptionInterfaceBase) te).neighbourChanged();
        }
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityContraptionInterfaceBase) {
            return ((TileEntityContraptionInterfaceBase) te).isConnected() ? 15 : 0;
        }
        return 0;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BlockProperties.CASING_12PX_MAPPED[state.getValue(FACING).getIndex()];
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        EnumFacing facing = state.getValue(FACING);
        if (facing.getAxis() == side.getAxis()) return false;
        EnumFacing rotated = facing.rotateAround(side.getAxis());
        Utils.setBlockTESafe(world, pos, state.withProperty(FACING, rotated), 3);
        return true;
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
}
