package nl.melonstudios.create.block;

import com.melonstudios.melonlib.item.IMetaName;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public final class BlockCasing extends Block implements IMetaName {
    public enum Variant implements IStringSerializable {
        ANDESITE,
        COPPER,
        BRASS,
        TRAIN;

        private final String name = this.toString().toLowerCase();
        private final int id = this.ordinal();

        @Override
        public String getName() {
            return this.name;
        }
        public int getID() {
            return this.id;
        }

        public static final Variant[] VALUES = {
                ANDESITE, COPPER, BRASS, TRAIN
        };
        public static Variant byID(int id) {
            return VALUES[id & 3];
        }
    }

    public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

    public BlockCasing() {
        super(Material.ROCK, MapColor.DIRT);
        this.blockSoundType = SoundType.WOOD;

        this.setRegistryName("casing");

        // Reference casings are stone-based (SharedProperties::stone + casing() transform).
        this.setHardness(1.5F);
        this.setResistance(6.0F);

        this.setHarvestLevel("pickaxe", 0);

        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "tile.create.casing_" + Variant.byID(stack.getMetadata()).getName();
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getID();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getID();
    }
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, Variant.byID(meta));
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        switch (state.getValue(VARIANT)) {
            case ANDESITE:
                return MapColor.DIRT;
            case COPPER:
                return MapColor.SILVER;
            case BRASS:
                return MapColor.BROWN;
            case TRAIN:
                return MapColor.OBSIDIAN;
            default:
                return MapColor.STONE;
        }
    }

    @Override
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable Entity entity) {
        switch (state.getValue(VARIANT)) {
            case ANDESITE:
            case COPPER:
            case BRASS:
                return SoundType.WOOD;
            case TRAIN:
                return SoundType.METAL;
            default:
                return SoundType.STONE;
        }
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, 0));
        items.add(new ItemStack(this, 1, 1));
        items.add(new ItemStack(this, 1, 2));
        items.add(new ItemStack(this, 1, 3));
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || ("axe".equals(type) && state.getValue(VARIANT) != Variant.TRAIN);
    }
}
