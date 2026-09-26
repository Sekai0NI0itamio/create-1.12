package nl.melonstudios.create.block.deco;

import nl.melonstudios.create.init.CreateTabs;
import com.melonstudios.melonlib.item.IMetaName;
import com.melonstudios.melonlib.misc.AABB;
import net.minecraft.block.BlockColored;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.oredict.OreDictionary;
import nl.melonstudios.create.entity.EntityPouf;
import nl.melonstudios.create.init.ItemInit;

import java.util.List;

@SuppressWarnings("deprecation")
public class BlockPouf extends BlockColored implements IMetaName {
    private static final String[] dyes = {
            "dyeBlack",
            "dyeRed",
            "dyeGreen",
            "dyeBrown",
            "dyeBlue",
            "dyePurple",
            "dyeCyan",
            "dyeLightGray",
            "dyeGray",
            "dyePink",
            "dyeLime",
            "dyeYellow",
            "dyeLightBlue",
            "dyeMagenta",
            "dyeOrange",
            "dyeWhite"
    };

    public BlockPouf() {
        super(Material.WOOD);
        // Reference seats copy wooden (log) strength, axe-only.
        this.setHardness(2.0F);
        this.setResistance(2.0F);
        this.setHarvestLevel("axe", 0);
        this.setSoundType(SoundType.WOOD);
        this.setCreativeTab(CreateTabs.TAB_CREATE_DECORATIONS);

        this.fullBlock = false;
        this.translucent = true;
        this.lightOpacity = 0;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.DOWN;
    }

    @Override
    public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return true;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB.SLAB_BOTTOM_AABB;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // Reference SeatBlock: sneaking or FakePlayer (deployer) never sits/recolors.
        if (playerIn.isSneaking() || playerIn instanceof FakePlayer)
            return false;
        ItemStack held = playerIn.getHeldItem(hand);
        if (held.isEmpty()) {
            if (!worldIn.isRemote) {
                final List<EntityPouf> poufs = worldIn.getEntitiesWithinAABB(EntityPouf.class, new AxisAlignedBB(pos));
                poufs.removeIf(pouf -> !pouf.blockBased);
                final EntityPouf pouf;
                if (poufs.isEmpty()) {
                    pouf = new EntityPouf(worldIn, pos);
                    worldIn.spawnEntity(pouf);
                } else {
                    pouf = poufs.get(0);
                }
                // Reference: never steal a seat occupied by a player; eject anything else first.
                if (!pouf.getPassengers().isEmpty()) {
                    if (pouf.getPassengers().get(0) instanceof EntityPlayer)
                        return false;
                    pouf.removePassengers();
                }
                playerIn.startRiding(pouf);
            }
            return true;
        }
        if (hand == EnumHand.MAIN_HAND) {
            for (int i = 0; i < 16; i++) {
                EnumDyeColor color = EnumDyeColor.byDyeDamage(i);
                String ore = dyes[i];
                int[] oreIDs = OreDictionary.getOreIDs(held);
                int oreID = OreDictionary.getOreID(ore);
                for (int id : oreIDs) {
                    if (oreID == id) {
                        worldIn.setBlockState(pos, state.withProperty(COLOR, color));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        final List<EntityPouf> poufs = worldIn.getEntitiesWithinAABB(EntityPouf.class, new AxisAlignedBB(pos));
        poufs.removeIf(pouf -> !pouf.blockBased);
        poufs.forEach(Entity::setDead);
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return this.getUnlocalizedName() + "_" + EnumDyeColor.byMetadata(stack.getMetadata()).getUnlocalizedName();
    }
}
