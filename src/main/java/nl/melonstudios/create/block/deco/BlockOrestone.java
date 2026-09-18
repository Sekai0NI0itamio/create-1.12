package nl.melonstudios.create.block.deco;

import com.melonstudios.melonlib.item.IMetaName;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.state.CreateStateProperties;
import nl.melonstudios.create.block.state.EnumOrestoneVariant;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.BlockProperties;

public class BlockOrestone extends Block implements IMetaName {
    public static final PropertyEnum<EnumOrestoneVariant> VARIANT = CreateStateProperties.ORESTONE_VARIANT;

    private final String type;
    public BlockOrestone(String type) {
        super(Material.ROCK);
        this.type = type;
        this.setRegistryName(this.type + "_orestone");

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(VARIANT, EnumOrestoneVariant.ASURINE)
        );
        this.setHarvestLevel("pickaxe", 0);
        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);

        this.setCreativeTab(ItemInit.TAB_CREATE_DECORATIONS);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

    @Override
    public String getUnlocalizedName(ItemStack itemStack) {
        return "tile.create." + this.type + "_" + EnumOrestoneVariant.byId(itemStack.getMetadata()).getName();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (int i = 0; i < 7; i++) {
            items.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getId();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, EnumOrestoneVariant.byId(meta));
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getId();
    }

    // Reference: asurine/crimsite/limestone/ochrum/veridium use destroyTime 1.25,
    // scoria/scorchia inherit blackstone hardness 1.5.
    @Override
    public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
        switch (state.getValue(VARIANT)) {
            case SCORIA:
            case SCORCHIA:
                return 1.5F;
            default:
                return 1.25F;
        }
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        switch (state.getValue(VARIANT)) {
            case ASURINE:
                return MapColor.BLUE;
            case CRIMSITE:
                return MapColor.NETHERRACK;
            case LIMESTONE:
                return MapColor.SAND;
            case OCHRUM:
                return MapColor.ORANGE_STAINED_HARDENED_CLAY;
            case SCORCHIA:
                return MapColor.GRAY;
            case SCORIA:
                return MapColor.BROWN;
            case VERIDIUM:
                return MapColor.GREEN;
            default:
                return MapColor.STONE;
        }
    }
}
