package nl.melonstudios.create.block.actor;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityGantryCarriage;

import javax.annotation.Nullable;

/**
 * Gantry carriage: moves along a shaft line when powered, carrying glued
 * blocks/riders. Right-click toggles direction; speed follows the network.
 */
@SuppressWarnings("deprecation")
public class BlockGantryCarriage extends BlockKineticBase implements ITileEntityProvider {
    public enum Mode implements IStringSerializable {
        FORWARD, BACKWARD;
        @Override
        public String getName() {
            return this.name().toLowerCase();
        }
    }

    public static final PropertyEnum<Mode> MODE = PropertyEnum.create("mode", Mode.class);
    public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class);

    public BlockGantryCarriage() {
        super(net.minecraft.block.material.Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;
        this.setDefaultState(this.blockState.getBaseState().withProperty(MODE, Mode.FORWARD).withProperty(AXIS, EnumFacing.Axis.X));
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityGantryCarriage();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, MODE, AXIS);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        world.setBlockState(pos, state.cycleProperty(MODE), 2);
        return true;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AXIS).ordinal() + (state.getValue(MODE) == Mode.BACKWARD ? 4 : 0);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing.Axis axis = EnumFacing.Axis.values()[meta % 4 % 3];
        return this.getDefaultState().withProperty(AXIS, axis).withProperty(MODE, meta >= 4 ? Mode.BACKWARD : Mode.FORWARD);
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side.getAxis() == state.getValue(AXIS);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
