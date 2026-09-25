package nl.melonstudios.create.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.BlockProperties;

/**
 * Cased cogwheel (translated from the reference EncasedCogwheelBlock, MIT).
 * Behaves like the matching base cogwheel (same ratios, same placement
 * validity, same diagonal-large rules) with a casing shell, so it renders
 * as a full cube. One class, four instances: andesite/brass crossed with
 * small/large. Shares TileEntityCogwheel with the bare cogs; no new TE.
 *
 * The reference top_shaft/bottom_shaft toggles are folded away for 1.12:
 * the encased cog always couples along its axis, like a shaft segment.
 */
public class BlockEncasedCogwheel extends BlockCogwheel {
    public BlockEncasedCogwheel(MapColor mapColor, SoundType soundType, boolean large, String casing) {
        super(mapColor, soundType, large);

        String size = large ? "large_cogwheel" : "cogwheel";
        this.setRegistryName(casing + "_encased_" + size);
        this.setUnlocalizedName("create." + casing + "_encased_" + size);

        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);

        this.setHarvestLevel("pickaxe", 0);

        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }
}
