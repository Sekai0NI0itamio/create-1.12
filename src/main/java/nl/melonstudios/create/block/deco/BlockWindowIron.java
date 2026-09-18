package nl.melonstudios.create.block.deco;

import com.melonstudios.melonlib.item.IMetaName;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import nl.melonstudios.create.init.ItemInit;

import java.util.Random;

@SuppressWarnings("deprecation")
public class BlockWindowIron extends BlockGlass implements IMetaName {
    public enum Variant implements IStringSerializable {
        CLASSIC("iron", 0),
        INDUSTRIAL("industrial", 1),
        INDUSTRIAL_RUSTY("industrial_rusty", 2);

        private final String name;
        private final int id;

        Variant(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String getName() {
            return this.name;
        }
        public int getId() {
            return this.id;
        }

        private static final Variant[] VALUES = {
                CLASSIC, INDUSTRIAL, INDUSTRIAL_RUSTY
        };

        public static Variant byId(int id) {
            if (id < 0 || id >= VALUES.length) return CLASSIC;
            return VALUES[id];
        }
    }

    public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

    public BlockWindowIron() {
        super(Material.GLASS, false);

        this.setRegistryName("window_iron");
        this.setUnlocalizedName("create.window_iron");

        // Reference windows copy vanilla glass strength/sound, no tool required.
        this.setHardness(0.3F);
        this.setResistance(1.5F);
        this.setSoundType(SoundType.GLASS);

        this.setCreativeTab(ItemInit.TAB_CREATE_DECORATIONS);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "tile.create.window_" + Variant.byId(stack.getMetadata()).getName();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (int i = 0; i < 3; i++) items.add(new ItemStack(this, 1, i));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getId();
    }
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, Variant.byId(meta));
    }

    // Reference loot: dropWhenSilkTouch — nothing without silk touch.
    @Override
    public int quantityDropped(Random random) {
        return 0;
    }
    @Override
    protected boolean canSilkHarvest() {
        return true;
    }
    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getId();
    }

    // Reference WindowBlock.skipRendering: same block culls; window against
    // connected glass culls on horizontal faces.
    @Override
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        Block neighbor = world.getBlockState(pos.offset(side)).getBlock();
        if (neighbor instanceof BlockWindowIron || neighbor instanceof BlockWindowWood) {
            return false;
        }
        if (side.getAxis().isHorizontal() && neighbor instanceof BlockFramedGlass) {
            return false;
        }
        return super.shouldSideBeRendered(state, world, pos, side);
    }
}
