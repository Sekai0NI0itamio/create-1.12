package nl.melonstudios.create.item;

import com.melonstudios.melonlib.item.IMetaName;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;

public class ItemBlockOrestonePillar extends ItemBlock {
    public ItemBlockOrestonePillar() {
        super(BlockInit.ORESTONE_PILLAR_Y);

        this.setRegistryName("orestone_pillar");
        this.setCreativeTab(CreateTabs.TAB_CREATE_DECORATIONS);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return ((IMetaName)this.block).getUnlocalizedName(stack);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public CreativeTabs getCreativeTab() {
        return CreateTabs.TAB_CREATE_DECORATIONS;
    }
}
