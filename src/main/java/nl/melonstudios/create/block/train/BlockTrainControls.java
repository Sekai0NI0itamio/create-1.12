package nl.melonstudios.create.block.train;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
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
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.train.TileEntityTrainControls;

import javax.annotation.Nullable;

/**
 * Train controls: driver's lever stand. Right-click cycles the throttle
 * (-100..100 in 10 steps, sneak-click resets to 0); the linked station's
 * train reads it as its target speed. OPEN marks a panel mounted inside a
 * moving contraption (reference open/closed/virtual states, virtual folded
 * into open here). Wrench rotates.
 */
@SuppressWarnings("deprecation")
public class BlockTrainControls extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool OPEN = PropertyBool.create("open");

    public BlockTrainControls() {
        super(Material.IRON, MapColor.BROWN);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(2.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(ItemInit.TAB_CREATE);
        this.setRegistryName("train_controls");
        this.setUnlocalizedName("create.train_controls");
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(OPEN, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, OPEN);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing look = placer.getHorizontalFacing();
        return this.getDefaultState().withProperty(FACING, placer.isSneaking() ? look : look.getOpposite());
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(OPEN) ? 4 : 0);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
                .withProperty(OPEN, (meta & 4) != 0);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityTrainControls();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                   EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityTrainControls) {
            TileEntityTrainControls controls = (TileEntityTrainControls) te;
            if (player.isSneaking()) controls.setThrottle(0);
            else controls.setThrottle(controls.getThrottle() + 10 > 100 ? -100 : controls.getThrottle() + 10);
            controls.sync();
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(controls.status()), true);
        }
        return true;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY,
                             float hitZ) {
        world.setBlockState(pos, state.withProperty(FACING, state.getValue(FACING).rotateY()), 3);
        return true;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0.125, 0, 0.125, 0.875, 1.0, 0.875);
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
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
