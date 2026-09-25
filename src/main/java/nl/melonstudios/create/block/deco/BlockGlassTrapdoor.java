package nl.melonstudios.create.block.deco;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockTrapDoor;
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
 * Trapdoors matching the Create door family.
 *
 * Reference: TRAIN_TRAPDOOR (metal set type) and FRAMED_GLASS_TRAPDOOR
 * (glass set type, connected textures). The reference has no
 * andesite/brass/copper trapdoors, so only train + framed_glass exist here.
 * 1.12 simplification: vanilla trapdoor behavior — click and redstone
 * open/close with top/bottom halves. Glass connected-texture culling is
 * NEEDS-LEAD client work.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockGlassTrapdoor extends BlockTrapDoor {
    private final String style;

    public BlockGlassTrapdoor(String style) {
        super(style.equals("framed_glass") ? Material.GLASS : Material.IRON);
        this.style = style;
        this.setRegistryName(style + "_trapdoor");
        this.setUnlocalizedName("create." + style + "_trapdoor");
        this.setSoundType(style.equals("framed_glass") ? SoundType.GLASS : SoundType.METAL);
        this.setHardness(3.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(ItemInit.TAB_CREATE_DECORATIONS);
    }

    public String getStyle() {
        return this.style;
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state) {
        if (this.style.equals("framed_glass")) return MapColor.AIR;
        return MapColor.CYAN;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return this.style.equals("framed_glass") ? BlockRenderLayer.CUTOUT : BlockRenderLayer.SOLID;
    }
}
