package nl.melonstudios.create.block.deco;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockDoor;
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
 * Sliding-style glass doors for the five Create door variants.
 *
 * Reference: SlidingDoorBlock entries ANDESITE / BRASS / COPPER / TRAIN /
 * FRAMED_GLASS (folding animation, VISIBLE property, sliding-door block
 * entity, contraption movement). 1.12 simplification (documented): standard
 * vanilla hinge-door behavior — click and redstone open/close, upper + lower
 * halves — using each variant's door textures. The folding/sliding animation
 * and hide-when-open rendering are NEEDS-LEAD client work.
 *
 * One class, one instance per style (a 1.12 door already fills its 4-bit
 * metadata with half/facing/hinge/open/powered, so styles cannot share metas).
 * Valid styles: andesite, brass, copper, framed_glass, train.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockGlassDoor extends BlockDoor {
    private final String style;
    private final boolean glassy;

    public BlockGlassDoor(String style) {
        super(style.equals("framed_glass") ? Material.GLASS : Material.IRON);
        this.style = style;
        this.glassy = style.equals("framed_glass") || style.equals("train");
        this.setRegistryName(style + "_door");
        this.setUnlocalizedName("create." + style + "_door");
        if (style.equals("framed_glass")) {
            this.setSoundType(SoundType.GLASS);
        } else if (style.equals("andesite")) {
            this.setSoundType(SoundType.STONE);
        } else {
            this.setSoundType(SoundType.METAL);
        }
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
        switch (this.style) {
            case "brass": return MapColor.GOLD;
            case "copper": return MapColor.ADOBE;
            case "train": return MapColor.CYAN;
            case "framed_glass": return MapColor.AIR;
            case "andesite":
            default: return MapColor.STONE;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return this.glassy ? BlockRenderLayer.CUTOUT : BlockRenderLayer.SOLID;
    }
}
