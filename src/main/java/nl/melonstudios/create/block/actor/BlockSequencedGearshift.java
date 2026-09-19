package nl.melonstudios.create.block.actor;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.PropertyBool;
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
import nl.melonstudios.create.block.BlockEncasedShaftBase;
import nl.melonstudios.create.kinetics.KineticPropagator;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.tileentity.actor.TileEntitySequencedGearshift;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Sequenced gearshift: runs a small program of rotate/stop/reverse steps,
 * advancing on redstone pulses. Right-click cycles program length display;
 * sneak-right-click resets. Programs are fixed patterns (official default:
 * turn 90° steps with pauses); redstone pulse advances one instruction.
 */
@SuppressWarnings("deprecation")
public class BlockSequencedGearshift extends BlockEncasedShaftBase implements ITileEntityProvider {
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockSequencedGearshift(MapColor color, SoundType soundType) {
        super(color, soundType);
        this.setDefaultState(this.getDefaultState().withProperty(POWERED, false));
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySequencedGearshift();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS, POWERED);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        IBlockState s = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand);
        try {
            return s.withProperty(POWERED, world.isBlockPowered(pos));
        } catch (Exception e) {
            return s;
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block block, BlockPos from) {
        if (world.isRemote) return;
        boolean powered = world.isBlockPowered(pos);
        if (powered != state.getValue(POWERED)) {
            world.setBlockState(pos, state.withProperty(POWERED, powered), 2);
            if (powered) {
                TileEntity te = world.getTileEntity(pos);
                if (te instanceof TileEntitySequencedGearshift) {
                    ((TileEntitySequencedGearshift) te).onPulse();
                    // Output modifier changed (holding -> running): re-propagate.
                    this.detachKinetics(world, pos, true);
                }
            }
        }
    }

    public void detachKinetics(World world, BlockPos pos, boolean reattachNextTick) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityKinetic)) return;
        KineticPropagator.handleRemoved(world, pos, (TileEntityKinetic) te);

        if (reattachNextTick) world.scheduleUpdate(pos, this, 1);
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityKinetic)) return;
        KineticPropagator.handleAdded(worldIn, pos, (TileEntityKinetic) te);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntitySequencedGearshift) {
            TileEntitySequencedGearshift seq = (TileEntitySequencedGearshift) te;
            if (player.isSneaking()) seq.reset();
            else seq.stepMode = (seq.stepMode + 1) % 3;
            seq.sync();
            String[] modes = {"90-degree steps", "half turns", "full turns"};
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    "Sequencer: " + modes[seq.stepMode] + " (pulse with redstone)"), true);
        }
        return true;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return super.getMetaFromState(state) + (state.getValue(POWERED) ? 4 : 0);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta % 4).withProperty(POWERED, meta >= 4);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }
}
