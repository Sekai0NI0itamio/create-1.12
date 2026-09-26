package nl.melonstudios.create.block.actor;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import nl.melonstudios.create.block.BlockShaftBase;
import nl.melonstudios.create.init.ItemInit;

/**
 * Gantry shaft: the rail a gantry carriage slides on. Reference idea in own
 * words: a plain shaft that carries rotation along one axis (so pin-mounted
 * carriages can ride it) and renders as the toothed gantry rail. Kinetic
 * behavior is inherited from the shaft base with its standard shaft tile.
 */
public class BlockGantryShaft extends BlockShaftBase {
    public BlockGantryShaft() {
        super(Material.ROCK, MapColor.STONE);
        this.setRegistryName("gantry_shaft");
        this.setUnlocalizedName("create.gantry_shaft");
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }
}
