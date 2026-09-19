package nl.melonstudios.create.block.actor;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticHorizontalBase;
import nl.melonstudios.create.block.state.CreateStateProperties;
import nl.melonstudios.create.block.state.EnumDirection;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.actor.TileEntityCrafter;
import nl.melonstudios.create.util.Utils;
import nl.melonstudios.create.util.interfaces.ICogwheel;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockCrafter extends BlockKineticHorizontalBase implements ICogwheel, ITileEntityProvider {
    public static final PropertyEnum<EnumDirection> DIRECTION = CreateStateProperties.DIRECTION;

    public BlockCrafter() {
        super(Material.ROCK, MapColor.WOOD);
        this.setSoundType(SoundType.METAL);
        this.setHardness(3.0F);
        this.setResistance(6.0F);

        this.setDefaultState(this.getDefaultState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(DIRECTION, EnumDirection.DOWN)
        );
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, DIRECTION);
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return false;
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityCrafter();
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add("Place in a grid, then click a filled Crafter to start");
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing placedFacing = placer.getHorizontalFacing().getOpposite();
        IBlockState placed = this.getDefaultState().withProperty(FACING, placedFacing);
        if (facing.getAxis() == placedFacing.getAxis()) {
            return placed;
        }
        return placed.withProperty(DIRECTION, EnumDirection.getRelativeMirror(placedFacing, facing.getOpposite()));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(DIRECTION).getId() << 2;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.HORIZONTALS[meta & 3];
        EnumDirection direction = EnumDirection.byId((meta >> 2) & 3);
        return this.getDefaultState().withProperty(FACING, facing).withProperty(DIRECTION, direction);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (facing == state.getValue(FACING) && hand == EnumHand.MAIN_HAND) {
            ItemStack held = playerIn.getHeldItem(EnumHand.MAIN_HAND);
            if (held.getItem() == ItemInit.WRENCH) return false;
            return Boolean.TRUE.equals(withTEDo(worldIn, pos, TileEntityCrafter.class, (te) -> {
                if (te.crafterContext != null) return false;
                if (held.isEmpty()) {
                    if (te.containedItem.isEmpty()) return false;
                    if (!worldIn.isRemote) {
                        playerIn.addItemStackToInventory(te.containedItem.copy());
                        te.containedItem = ItemStack.EMPTY;
                        te.sync();
                        worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    }
                } else {
                    if (!te.containedItem.isEmpty()) return false;
                    if (!worldIn.isRemote) {
                        te.containedItem = held.splitStack(1);
                        te.startCraftingIfReady(false);
                        te.sync();
                        playerIn.setHeldItem(EnumHand.MAIN_HAND, held);
                        worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    }
                }
                return true;
            }));
        }
        return false;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (side.getAxis() == EnumFacing.Axis.Y) {
            Utils.setBlockKineticTESafe(world, pos, state.withRotation(Rotation.CLOCKWISE_90), 3);
            return true;
        }
        EnumFacing facing = state.getValue(FACING);
        if (side == facing) {
            Utils.setBlockKineticTESafe(world, pos, state.cycleProperty(DIRECTION), 3);
            return true;
        } else if (side == facing.getOpposite()) {
            //TODO: connected inventories
        }
        return false;
    }
}
