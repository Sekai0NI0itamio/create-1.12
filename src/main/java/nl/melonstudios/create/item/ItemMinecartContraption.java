package nl.melonstudios.create.item;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.item.EntityMinecartFurnace;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Minecart that carries an assembled contraption.
 * Reference: three items (minecart_contraption, furnace_minecart_contraption,
 * chest_minecart_contraption), one per cart type. This port folds them into
 * one item with three metas (0 rideable / 1 furnace / 2 chest), matching the
 * ItemSchematic/ItemHat subtype idiom used elsewhere in the backport.
 * Right-clicking rails places the matching vanilla cart; assembling a real
 * contraption onto it waits on the contraption system (see NEEDS-LEAD).
 */
public class ItemMinecartContraption extends Item {
    public static final String[] NAME_LOOKUP = {
            "minecart_contraption", "furnace_minecart_contraption", "chest_minecart_contraption"
    };

    public ItemMinecartContraption() {
        super();
        this.setRegistryName("minecart_contraption");
        this.setUnlocalizedName("create.minecart_contraption");
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
        this.setMaxStackSize(1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int meta = stack.getMetadata();
        return meta >= 0 && meta < NAME_LOOKUP.length
                ? "item.create." + NAME_LOOKUP[meta]
                : "item.create.minecart_contraption";
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            for (int i = 0; i < NAME_LOOKUP.length; i++) {
                items.add(new ItemStack(this, 1, i));
            }
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "Place on rails, then assemble a contraption onto it.");
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return EnumActionResult.PASS;
        }
        net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof net.minecraft.block.BlockRailBase)) {
            return EnumActionResult.PASS;
        }
        net.minecraft.entity.item.EntityMinecart cart;
        switch (stack.getMetadata()) {
            case 1:
                cart = new EntityMinecartFurnace(world);
                break;
            case 2:
                cart = new EntityMinecartChest(world);
                break;
            default:
                cart = new EntityMinecartEmpty(world);
                break;
        }
        cart.setPosition(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        world.spawnEntity(cart);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }
}
