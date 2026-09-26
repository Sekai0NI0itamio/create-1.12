package nl.melonstudios.create.block.deco;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockPane;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Metal bars for andesite / brass / copper.
 *
 * Reference: MetalBarsGen bar entries (iron-bars behavior with per-metal
 * textures). 1.12 port: vanilla BlockPane connection visuals inherited
 * unchanged, per-metal textures wired through the blockstate-referenced
 * post/side models.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockMetalBars extends BlockPane {
    private final String metal;

    public BlockMetalBars(String metal) {
        super(Material.IRON, true);
        this.metal = metal;
        this.setRegistryName(metal + "_bars");
        this.setUnlocalizedName("create." + metal + "_bars");
        this.setSoundType(metal.equals("andesite") ? SoundType.STONE : SoundType.METAL);
        this.setHardness(3.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreateTabs.TAB_CREATE_DECORATIONS);
    }

    public String getMetal() {
        return this.metal;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }
}
