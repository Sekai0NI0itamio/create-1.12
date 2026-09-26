package nl.melonstudios.create.block.actor;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.actor.TileEntitySchematicTable;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;

/**
 * Schematic table: a two-slot workstation for staging schematic drafts.
 * Right-click inserts the held stack into the first free slot, empty-hand
 * click takes the last filled slot back, and sneak-click lists the staged
 * contents in chat. Hoppers can feed both slots and the comparator reads
 * the fill level. The reference upload GUI has no backport equivalent, so
 * the table keeps the persistent part: staged storage plus automation.
 */
@SuppressWarnings("deprecation")
public class BlockSchematicTable extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.9375, 1.0);

    public BlockSchematicTable() {
        super(Material.WOOD, MapColor.WOOD);
        this.blockSoundType = SoundType.WOOD;
        this.setHardness(2.0F);
        this.setResistance(4.0F);
        this.setHarvestLevel("axe", 0);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("schematic_table");
        this.setUnlocalizedName("create.schematic_table");
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess access, BlockPos pos) {
        return AABB;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                   EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntitySchematicTable table =
                Utils.cast(world.getTileEntity(pos), TileEntitySchematicTable.class);
        if (table == null) return false;
        if (world.isRemote) return true;
        if (!player.capabilities.allowEdit) return false;

        if (player.isSneaking()) {
            StringBuilder sb = new StringBuilder("Staged:");
            boolean empty = true;
            for (int i = 0; i < table.inventory.getSlots(); i++) {
                ItemStack stack = table.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    empty = false;
                    sb.append(" [").append(stack.getCount()).append("x ")
                            .append(stack.getDisplayName()).append("]");
                }
            }
            if (empty) sb.append(" nothing");
            player.sendMessage(new TextComponentString(sb.toString()));
            return true;
        }

        ItemStack inHand = player.getHeldItem(hand);
        if (inHand.isEmpty()) {
            for (int i = table.inventory.getSlots() - 1; i >= 0; i--) {
                ItemStack staged = table.inventory.getStackInSlot(i);
                if (!staged.isEmpty()) {
                    player.setHeldItem(hand, table.inventory.extractItem(i, staged.getCount(), false));
                    world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5F, 0.8F);
                    return true;
                }
            }
            return false;
        }

        ItemStack rest = inHand.copy();
        for (int i = 0; i < table.inventory.getSlots() && !rest.isEmpty(); i++) {
            rest = table.inventory.insertItem(i, rest, false);
        }
        player.setHeldItem(hand, rest);
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5F, 1.2F);
        return true;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side,
                             float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        Utils.setBlockTESafe(world, pos, state.withProperty(FACING, state.getValue(FACING).rotateY()), 3);
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntitySchematicTable();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntitySchematicTable table =
                Utils.cast(world.getTileEntity(pos), TileEntitySchematicTable.class);
        if (table != null && !world.isRemote) {
            for (int i = 0; i < table.inventory.getSlots(); i++) {
                ItemStack stack = table.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
                }
            }
        }
        world.removeTileEntity(pos);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntitySchematicTable table =
                Utils.cast(world.getTileEntity(pos), TileEntitySchematicTable.class);
        return table == null ? 0 : table.getFillLevel();
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "axe".equals(type);
    }
}
