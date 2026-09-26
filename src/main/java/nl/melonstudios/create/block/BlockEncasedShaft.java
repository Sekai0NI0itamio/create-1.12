package nl.melonstudios.create.block;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.TileEntityShaft;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;

/**
 * Cased shaft relay (translated from the reference EncasedShaftBlock, MIT).
 * Behaves exactly like a base shaft (same axis propagation, same stress)
 * with a casing shell around it. One class, two casings: the lead registers
 * an "andesite_encased_shaft" and a "brass_encased_shaft" instance.
 * Shares TileEntityShaft with the bare shaft; no new TE.
 */
public class BlockEncasedShaft extends BlockEncasedShaftBase implements ITileEntityProvider {
    public BlockEncasedShaft(MapColor mapColor, SoundType soundType, String casing) {
        super(mapColor, soundType);

        this.setRegistryName(casing + "_encased_shaft");
        this.setUnlocalizedName("create." + casing + "_encased_shaft");

        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);

        this.setHarvestLevel("pickaxe", 0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityShaft();
    }
}
