package nl.melonstudios.create.init;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import nl.melonstudios.create.util.ModTabs;

/**
 * Creative tabs live here instead of ItemInit so that block constructors
 * referencing a tab do not trigger ItemInit static init (which builds every
 * item, including block-dependent ones) in the middle of BlockInit init.
 */
public final class CreateTabs {
    private CreateTabs() {
    }

    public static final CreativeTabs TAB_CREATE = new ModTabs("create", () -> new ItemStack(BlockInit.COG_SMALL));
    public static final CreativeTabs TAB_CREATE_DECORATIONS = new ModTabs("create.decorations", () -> new ItemStack(BlockInit.WINDOW_IRON));
}
