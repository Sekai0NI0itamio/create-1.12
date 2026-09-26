package nl.melonstudios.create.block;

import com.melonstudios.melonlib.item.IMetaName;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BlockMetal extends Block implements IMetaName {
    public enum Variant implements IStringSerializable {
        ANDESITE_ALLOY("andesite_alloy"),
        COPPER("copper"),
        ZINC("zinc"),
        BRASS("brass");

        private final String name;

        Variant(String name) {
            this.name = name;
        }
        @Override
        public String getName() {
            return this.name;
        }

        public static Variant byID(int id) {
            return values()[id & 3];
        }
    }

    public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

    private static final BlockMetal get = new BlockMetal();
    public static BlockMetal get() {
        return get;
    }

    private BlockMetal() {
        super(Material.IRON);

        this.blockSoundType = SoundType.METAL;

        this.setRegistryName("metal");

        this.setHarvestLevel("pickaxe", 0, this.getStateFromMeta(0));
        this.setHarvestLevel("pickaxe", 1, this.getStateFromMeta(1));
        this.setHarvestLevel("pickaxe", 2, this.getStateFromMeta(2));
        this.setHarvestLevel("pickaxe", 2, this.getStateFromMeta(3));

        this.setCreativeTab(CreateTabs.TAB_CREATE);

        this.setHardness(BlockProperties.IRON_HARDNESS);
        this.setResistance(BlockProperties.IRON_RESISTANCE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "tile.create.block_" + Variant.byID(stack.getMetadata()).getName();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, Variant.byID(meta));
    }
    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).ordinal();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, 0));
        items.add(new ItemStack(this, 1, 1));
        items.add(new ItemStack(this, 1, 2));
        items.add(new ItemStack(this, 1, 3));
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).ordinal();
    }

    @Override
    public float getBlockHardness(IBlockState state, World worldIn, BlockPos pos) {
        // Reference andesite-alloy block copies vanilla andesite (1.5); other metals copy iron block.
        return state.getValue(VARIANT) == Variant.ANDESITE_ALLOY
                ? BlockProperties.STONE_HARDNESS
                : super.getBlockHardness(state, worldIn, pos);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        // Reference colors: andesite STONE, copper orange, zinc pale, brass yellow.
        switch (state.getValue(VARIANT)) {
            case ANDESITE_ALLOY:
                return MapColor.STONE;
            case COPPER:
                return MapColor.ADOBE;
            case ZINC:
                return MapColor.SILVER;
            case BRASS:
            default:
                return MapColor.GOLD;
        }
    }
}
