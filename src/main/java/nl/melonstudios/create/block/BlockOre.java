package nl.melonstudios.create.block;

import nl.melonstudios.create.init.CreateTabs;
import com.melonstudios.melonlib.item.IMetaName;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
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
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public final class BlockOre extends Block implements IMetaName {
    public enum Variant implements IStringSerializable {
        COPPER("copper"),
        ZINC("zinc");
        private final String name;

        Variant(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", BlockOre.Variant.class);

    public static IBlockState copper() {
        return BlockInit.ORE.getStateFromMeta(0);
    }
    public static IBlockState zinc() {
        return BlockInit.ORE.getStateFromMeta(1);
    }

    public BlockOre() {
        super(Material.ROCK);

        this.setRegistryName("ore");
        this.setUnlocalizedName("create.ore");

        // Reference zinc ores copy vanilla gold-ore strength (3.0 hardness).
        this.setHardness(BlockProperties.ORE_HARDNESS);
        this.setResistance(3.0F);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(VARIANT, Variant.COPPER));

        this.setHarvestLevel("pickaxe", 1, this.getStateFromMeta(0));
        this.setHarvestLevel("pickaxe", 2, this.getStateFromMeta(1));

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return stack.getMetadata() != 0 ? "tile.create.ore_zinc" : "tile.create.ore_copper";
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).ordinal();
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        // Reference zinc ore uses METAL; copper keeps the stone default.
        return state.getValue(VARIANT) == Variant.ZINC
                ? MapColor.IRON
                : super.getMapColor(state, worldIn, pos);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).ordinal();
    }
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, Variant.values()[meta & 1]);
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, 0));
        items.add(new ItemStack(this, 1, 1));
    }
}
