package nl.melonstudios.create.block.actor;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityElevatorPulley;

import javax.annotation.Nullable;

/**
 * Elevator pulley: carries the glued platform above/below it between contact
 * levels. Right-click calls it up/down; it stops at elevator contacts.
 */
@SuppressWarnings("deprecation")
public class BlockElevatorPulley extends BlockKineticBase implements ITileEntityProvider {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public BlockElevatorPulley() {
        super(Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityElevatorPulley();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, facing.getOpposite());
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getFront(meta));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityElevatorPulley) {
            ((TileEntityElevatorPulley) te).call(player.isSneaking() ? -1 : 1);
            return true;
        }
        return false;
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return EnumFacing.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side == EnumFacing.UP;
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
