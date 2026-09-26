package nl.melonstudios.create.block.redstone;

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
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.redstone.TileEntityBrassDiode;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;

/**
 * Shared base for the brass diode family (pulse repeater, pulse extender,
 * pulse timer). Carries the reference output rules: the block emits full
 * power toward its facing only while the latched output is on, flipped by
 * the inverted flag, and only joins redstone along its own axis. Timing
 * lives in the tile entity; right-click steps the delay through presets
 * and sneak-click flips the output.
 */
@SuppressWarnings("deprecation")
public abstract class BlockPulseDiode extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyBool POWERING = PropertyBool.create("powering");
    public static final PropertyBool INVERTED = PropertyBool.create("inverted");
    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    protected BlockPulseDiode() {
        super(Material.CIRCUITS, MapColor.GOLD);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(0.5F);
        this.setResistance(2.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(POWERING, false)
                .withProperty(INVERTED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERING, INVERTED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(meta & 0b0011))
                .withProperty(POWERING, (meta & 0b0100) != 0)
                .withProperty(INVERTED, (meta & 0b1000) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex()
                | (state.getValue(POWERING) ? 0b0100 : 0)
                | (state.getValue(INVERTED) ? 0b1000 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                   EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!player.capabilities.allowEdit) return false;
        if (world.isRemote) return true;
        TileEntityBrassDiode diode = Utils.cast(world.getTileEntity(pos), TileEntityBrassDiode.class);
        if (player.isSneaking()) {
            Utils.setBlockTESafe(world, pos, state.cycleProperty(INVERTED), 3);
            world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, 0.5F);
            world.notifyNeighborsOfStateChange(pos, this, false);
        } else if (diode != null) {
            diode.cycleDelay();
            world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, 0.6F);
        }
        return true;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side,
                             float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        Utils.setBlockTESafe(world, pos, state.withProperty(FACING, state.getValue(FACING).rotateY()), 3);
        world.notifyNeighborsOfStateChange(pos, this, false);
        return true;
    }

    /** Back-side input level, mirroring the diode input check. */
    public static boolean readBackInput(World world, BlockPos pos, IBlockState state) {
        EnumFacing front = state.getValue(FACING);
        EnumFacing back = front.getOpposite();
        BlockPos backPos = pos.offset(back);
        return world.getRedstonePower(backPos, back) > 0;
    }

    public void setPowering(World world, BlockPos pos, IBlockState state, boolean powering) {
        if (state.getValue(POWERING) != powering) {
            Utils.setBlockTESafe(world, pos, state.withProperty(POWERING, powering), 3);
            world.notifyNeighborsOfStateChange(pos, this, false);
        }
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        boolean out = state.getValue(POWERING) ^ state.getValue(INVERTED);
        if (!out) return 0;
        return side == state.getValue(FACING) ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        return this.getWeakPower(state, access, pos, side);
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos,
                                      @Nullable EnumFacing side) {
        return side != null && side.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        world.removeTileEntity(pos);
        world.notifyNeighborsOfStateChange(pos, this, false);
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
