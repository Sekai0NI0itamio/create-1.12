package nl.melonstudios.create.block;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.interfaces.IHeatProvider;

/**
 * Lit blaze burner: the burner as it looks right after flint-and-steel, sold
 * as its own block. Matches the backport burner's LIT values (heat 0 like the
 * LIT variant, light 15) without needing the lighting step.
 */
@SuppressWarnings("deprecation")
public class BlockLitBlazeBurner extends Block implements IHeatProvider {
    public BlockLitBlazeBurner() {
        super(Material.IRON, MapColor.NETHERRACK);
        this.setSoundType(SoundType.METAL);
        this.setHardness(3.0F);
        this.setResistance(BlockProperties.IRON_RESISTANCE);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("lit_blaze_burner");
        this.setUnlocalizedName("create.lit_blaze_burner");
    }

    @Override
    public int getHeat(World world, BlockPos pos, IBlockState state) {
        return 0;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 15;
    }

    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
}
