package nl.melonstudios.create.block.generator;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.generator.TileEntityValveHandle;

import javax.annotation.Nullable;

/**
 * Copper valve handle, plus one dyed variant per dye colour.
 *
 * <p>Translated from the reference ValveHandleBlock (never pasted): a
 * hand-crank descendant that spins at 32 RPM while cranked, with the same
 * click-to-turn interaction. The reference registers one block per colour
 * ({@code copper_valve_handle} + {@code <dye>_valve_handle}); this port
 * keeps that shape with a single class taking the colour in its
 * constructor — null means plain copper — so each registered instance only
 * differs by name, map colour and handle texture.</p>
 *
 * <p>Dyeing a placed handle in-world needs the block registry and lives
 * outside this block's files (NEEDS-LEAD); the tile interaction is the
 * crank turn, with hunger cost copied from the hand crank
 * (32 * 0.01 exhaustion).</p>
 */
public class BlockValveHandle extends BlockHandCrank {
    /** Dye colour, or null for plain copper. */
    @Nullable
    public final EnumDyeColor color;

    public BlockValveHandle(@Nullable EnumDyeColor color) {
        super(color == null ? MapColor.WOOD : MapColor.getBlockColor(color), SoundType.WOOD);
        this.color = color;
        String name = color == null ? "copper_valve_handle" : color.getName() + "_valve_handle";
        this.setRegistryName(name);
        this.setUnlocalizedName("create." + name);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    /** Texture file stem for this handle's head, e.g. {@code light_gray}. */
    public String textureStem() {
        if (this.color == null) return "copper";
        if (this.color == EnumDyeColor.SILVER) return "light_gray";
        return this.color.getName();
    }

    /** Reference rotation speed: 32 RPM, same as the hand crank. */
    @Override
    public int getRotationSpeed() {
        return 32;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityValveHandle();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (playerIn.isSpectator()) return false;
        ItemStack held = playerIn.getHeldItem(hand);
        // Dye in hand with a matching recolour target is lead territory
        // (block swap); plain clicks always crank.
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityValveHandle) {
            if (!worldIn.isRemote) {
                ((TileEntityValveHandle) te).activate(playerIn.isSneaking());
            }
        }
        if (!held.isEmpty()) {
            EnumDyeColor dye = dyeOf(held);
            if (dye != null && dye != this.color) {
                // Recolour swap handled at registration level (NEEDS-LEAD).
                return true;
            }
        }
        playerIn.addExhaustion(0.32F);
        return true;
    }

    @Nullable
    private static EnumDyeColor dyeOf(ItemStack stack) {
        // Ore-dict-free dye check: vanilla dye damage values double as the
        // dye list, avoiding an ItemDye dependency in the block class.
        for (EnumDyeColor dye : EnumDyeColor.values()) {
            ItemStack probe = new ItemStack(net.minecraft.init.Items.DYE, 1, dye.getDyeDamage());
            if (net.minecraftforge.oredict.OreDictionary.itemMatches(probe, stack, false)) return dye;
        }
        return null;
    }
}
