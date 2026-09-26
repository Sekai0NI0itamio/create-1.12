package nl.melonstudios.create.item;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

/**
 * Extendo grip. Reference: ExtendoGripItem (durability 200, +3 block reach
 * single-wield, +5 dual-wield via transient attribute modifiers keyed by
 * fixed UUIDs). 1.12 has no Forge reach attribute, so the bonus targets the
 * vanilla player reach attribute and is reconciled every tick in onUpdate
 * (no event-bus wiring needed).
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemExtendoGrip extends Item {
    /** Reference MAX_DAMAGE = 200. */
    public static final double SINGLE_BONUS = 3.0D;
    public static final double DUAL_BONUS = 5.0D;
    private static final UUID SINGLE_ID = UUID.fromString("7f7dbdb2-0d0d-458a-aa40-ac7633691f66");
    private static final UUID DUAL_ID = UUID.fromString("8f7dbdb2-0d0d-458a-aa40-ac7633691f66");

    public ItemExtendoGrip() {
        this.setMaxDamage(200);
        this.setMaxStackSize(1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    public static boolean isGrip(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemExtendoGrip;
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if (!(entityIn instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entityIn;
        IAttributeInstance reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE);
        if (reach == null) return;
        boolean main = isGrip(player.getHeldItemMainhand());
        boolean off = isGrip(player.getHeldItemOffhand());
        AttributeModifier single = new AttributeModifier(SINGLE_ID, "createExtendo", SINGLE_BONUS, 0);
        AttributeModifier dual = new AttributeModifier(DUAL_ID, "createDualExtendo", DUAL_BONUS, 0);
        reach.removeModifier(SINGLE_ID);
        reach.removeModifier(DUAL_ID);
        if (main && off) {
            reach.applyModifier(dual);
        } else if (main || off) {
            reach.applyModifier(single);
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "Hold: +3 reach. Hold two: +5 reach.");
    }
}
