package nl.melonstudios.create.block.actor;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticDirectionalBase;
import nl.melonstudios.create.block.state.CreateStateProperties;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.tileentity.actor.TileEntityMechanicalPiston;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.TextBuilder;
import nl.melonstudios.create.util.Utils;
import nl.melonstudios.create.util.interfaces.ISelectiveImmovable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@SuppressWarnings("deprecation")
public class BlockMechanicalPiston extends BlockKineticDirectionalBase implements ITileEntityProvider, ISelectiveImmovable {
    public static final PropertyBool ROTATED = CreateStateProperties.ROTATED;

    public final boolean sticky;
    public final boolean extended;
    public BlockMechanicalPiston(boolean sticky, boolean extended) {
        super(Material.ROCK, MapColor.WOOD);
        this.sticky = sticky;
        this.extended = extended;

        this.setDefaultState(this.getDefaultState()
                .withProperty(FACING, EnumFacing.UP)
                .withProperty(ROTATED, false)
        );
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ROTATED);
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return this.getRotationAxis(state) == side.getAxis();
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityMechanicalPiston();
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return getShaftAxis(state.getValue(FACING), state.getValue(ROTATED));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() | (state.getValue(ROTATED) ? 0b1000 : 0b0000);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.VALUES[meta & 0b0111]).withProperty(ROTATED, (meta & 0b1000) != 0);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = playerIn.getHeldItem(hand);
        if (!this.sticky && !this.extended && facing == state.getValue(FACING) && hand == EnumHand.MAIN_HAND
                && !held.isEmpty() && held.getItem() == Items.SLIME_BALL) {
            if (worldIn.isRemote) {
                for (int i = 0; i < 8; i++) {
                    worldIn.spawnParticle(EnumParticleTypes.SLIME,
                            pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ,
                            (worldIn.rand.nextFloat() - 0.5) * 0.2, worldIn.rand.nextFloat() * 0.2, (worldIn.rand.nextFloat() - 0.5) * 0.2);
                }
                return true;
            }
            if (!playerIn.isCreative()) held.shrink(1);
            Utils.setBlockKineticTESafe(worldIn, pos, BlockInit.MECHANICAL_PISTON_STICKY.copyState(state), 3);
            worldIn.playSound(null, pos, SoundInit.slime_added, SoundCategory.BLOCKS, 0.5F, 1.0F);
            return true;
        }
        if (facing == state.getValue(FACING) || hand == EnumHand.OFF_HAND) return false;
        if (held.isEmpty()) {
            return Boolean.TRUE.equals(withTEDo(worldIn, pos, TileEntityMechanicalPiston.class, TileEntityMechanicalPiston::tryAssemble));
        }
        return false;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        withTEDo(world, pos, TileEntityMechanicalPiston.class, TileEntityMechanicalPiston::emergencyDisassemble);
        if (!super.onWrenched(world, pos, state, side, hitX, hitY, hitZ)) {
            Utils.setBlockKineticTESafe(world, pos, state.cycleProperty(ROTATED), 3);
        }
        return true;
    }

    @Override
    public List<String> getGoggleInfo(World world, BlockPos pos, IBlockState state) {
        List<String> list = super.getGoggleInfo(world, pos, state);
        List<String> realList = list == Collections.EMPTY_LIST ? new ArrayList<>() : list;
        withTEDo(world, pos, TileEntityMechanicalPiston.class, (te) -> {
            if (te.lastFailure != null) {
                TextBuilder builder = new TextBuilder();
                if (!list.isEmpty()) builder.enter();
                builder.formatting(TextFormatting.GOLD);
                builder.translate("assembly_failure.header");
                builder.enter().space().space();
                builder.formatting(TextFormatting.GRAY);
                builder.translate(te.lastFailure.error);
                realList.addAll(builder.build());
            }
        });
        return realList;
    }

    @Override
    public boolean isAssociatedBlock(Block other) {
        return other instanceof BlockMechanicalPiston && ((BlockMechanicalPiston)other).sticky == this.sticky;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(setAssembled(state, false).getBlock());
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(setAssembled(state, false).getBlock()));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return this.extended ? BlockProperties.CASING_12PX_MAPPED[state.getValue(FACING).getIndex()] : FULL_BLOCK_AABB;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || "axe".equals(type);
    }

    public IBlockState copyState(IBlockState state) {
        return this.getDefaultState()
                .withProperty(FACING, state.getValue(FACING))
                .withProperty(ROTATED, state.getValue(ROTATED));
    }
    public static IBlockState setAssembled(IBlockState state, boolean assembled) {
        BlockMechanicalPiston block = (BlockMechanicalPiston) state.getBlock();
        if (block.sticky) {
            if (assembled) return BlockInit.MECHANICAL_PISTON_STICKY_EXTENDED.copyState(state);
            else return BlockInit.MECHANICAL_PISTON_STICKY.copyState(state);
        } else {
            if (assembled) return BlockInit.MECHANICAL_PISTON_EXTENDED.copyState(state);
            else return BlockInit.MECHANICAL_PISTON.copyState(state);
        }
    }

    public static EnumFacing.Axis getShaftAxis(EnumFacing facing, boolean rotated) {
        return STATE_TO_AXIS_LOOKUP[facing.getIndex()*2 + (rotated ? 1 : 0)];
    }

    private static final EnumFacing.Axis[] STATE_TO_AXIS_LOOKUP = new EnumFacing.Axis[12];
    static {
        STATE_TO_AXIS_LOOKUP[0] = EnumFacing.Axis.X;
        STATE_TO_AXIS_LOOKUP[1] = EnumFacing.Axis.Z;
        STATE_TO_AXIS_LOOKUP[2] = EnumFacing.Axis.X;
        STATE_TO_AXIS_LOOKUP[3] = EnumFacing.Axis.Z;
        STATE_TO_AXIS_LOOKUP[4] = EnumFacing.Axis.X;
        STATE_TO_AXIS_LOOKUP[5] = EnumFacing.Axis.Y;
        STATE_TO_AXIS_LOOKUP[6] = EnumFacing.Axis.X;
        STATE_TO_AXIS_LOOKUP[7] = EnumFacing.Axis.Y;
        STATE_TO_AXIS_LOOKUP[8] = EnumFacing.Axis.Z;
        STATE_TO_AXIS_LOOKUP[9] = EnumFacing.Axis.Y;
        STATE_TO_AXIS_LOOKUP[10] = EnumFacing.Axis.Z;
        STATE_TO_AXIS_LOOKUP[11] = EnumFacing.Axis.Y;
    }

    @Override
    public boolean isImmovable(World world, BlockPos pos, IBlockState state) {
        TileEntityMechanicalPiston te = Utils.cast(world.getTileEntity(pos), TileEntityMechanicalPiston.class);
        return te != null && te.isAssembled();
    }
}
