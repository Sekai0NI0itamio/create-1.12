package nl.melonstudios.create.block.deco;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Metal ladders for andesite / brass / copper.
 *
 * Reference: MetalLadderBlock (vanilla ladder behavior + stacked placement
 * helper + hoop variant). 1.12 simplification: vanilla BlockLadder climbing,
 * wall-mounting and survival checks are inherited unchanged. The
 * click-to-extend placement helper is NEEDS-LEAD (needs client/server
 * placement-assist plumbing); hoop variants have no copied textures and are
 * listed under NEEDS-LEAD rather than invented.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockMetalLadder extends BlockLadder {
    private final String metal;

    public BlockMetalLadder(String metal) {
        super();
        this.metal = metal;
        this.setRegistryName(metal + "_ladder");
        this.setUnlocalizedName("create." + metal + "_ladder");
        this.setSoundType(metal.equals("andesite") ? SoundType.STONE : SoundType.METAL);
        this.setHardness(1.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(ItemInit.TAB_CREATE_DECORATIONS);
    }

    public String getMetal() {
        return this.metal;
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state) {
        switch (this.metal) {
            case "brass": return MapColor.GOLD;
            case "copper": return MapColor.ADOBE;
            case "andesite":
            default: return MapColor.STONE;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Material getMaterial(IBlockState state) {
        return Material.IRON;
    }
}
