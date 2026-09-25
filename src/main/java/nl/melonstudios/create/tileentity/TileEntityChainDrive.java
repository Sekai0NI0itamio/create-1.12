package nl.melonstudios.create.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockChainDrive;
import nl.melonstudios.create.util.interfaces.IRotate;

import java.util.LinkedList;

/**
 * Backport of the reference chain-drive rotation relay (translated, MIT).
 * Rules ported from ChainDriveBlock / its speed modifier:
 * - two chain drives with the same rotation axis relay rotation along the
 *   run direction while every block between them is a same-axis chain drive;
 * - maximum bridge length 16 blocks;
 * - ratio 1:1 with unchanged direction (chains preserve direction, unlike
 *   meshed gears which invert it).
 * Shaft-adjacent connections along the rotation axis keep the default
 * axis-propagation path (this override returns 0 for those so the
 * KineticPropagator's connectedByAxis branch handles them).
 */
public class TileEntityChainDrive extends TileEntityKinetic {
    /** Reference: a chain bridges two shafts up to 16 blocks apart. */
    public static final int MAX_CHAIN_LENGTH = 16;

    @SideOnly(Side.CLIENT)
    public EnumFacing.Axis getRenderAxis() {
        return this.getState().getValue(BlockChainDrive.AXIS);
    }

    private boolean isChainPartner(TileEntityKinetic target,
                                   IBlockState stateFrom, IBlockState stateTo, BlockPos diff) {
        if (!(stateFrom.getBlock() instanceof BlockChainDrive)) return false;
        if (!(stateTo.getBlock() instanceof BlockChainDrive)) return false;
        EnumFacing.Axis axisFrom = stateFrom.getValue(BlockChainDrive.AXIS);
        EnumFacing.Axis axisTo = stateTo.getValue(BlockChainDrive.AXIS);
        if (axisFrom != axisTo) return false;

        EnumFacing.Axis run = BlockChainDrive.getConnectionAxis(stateFrom);
        int distance;
        if (run == EnumFacing.Axis.X) {
            if (diff.getY() != 0 || diff.getZ() != 0) return false;
            distance = diff.getX();
        } else if (run == EnumFacing.Axis.Z) {
            if (diff.getX() != 0 || diff.getY() != 0) return false;
            distance = diff.getZ();
        } else {
            return false;
        }
        if (distance == 0 || Math.abs(distance) > MAX_CHAIN_LENGTH) return false;

        if (this.world == null) return false;
        int step = distance > 0 ? 1 : -1;
        for (int i = 1; i < Math.abs(distance); i++) {
            BlockPos mid;
            if (run == EnumFacing.Axis.X) mid = this.pos.add(step * i, 0, 0);
            else mid = this.pos.add(0, 0, step * i);
            if (!this.world.isBlockLoaded(mid)) return false;
            IBlockState midState = this.world.getBlockState(mid);
            if (!(midState.getBlock() instanceof BlockChainDrive)) return false;
            if (midState.getValue(BlockChainDrive.AXIS) != axisFrom) return false;
        }
        return true;
    }

    @Override
    public float propagateRotationTo(TileEntityKinetic target,
                                     IBlockState stateFrom, IBlockState stateTo, BlockPos diff,
                                     boolean connectedViaAxes, boolean connectedViaCogs) {
        if (target instanceof TileEntityChainDrive
                && this.isChainPartner(target, stateFrom, stateTo, diff)) {
            return 1.0F;
        }
        return 0.0F;
    }

    @Override
    public boolean isCustomConnection(TileEntityKinetic other,
                                      IBlockState state, IBlockState otherState) {
        if (!(other instanceof TileEntityChainDrive)) return false;
        return this.isChainPartner(other, state, otherState,
                other.getPos().subtract(this.getPos()));
    }

    @Override
    public LinkedList<BlockPos> addPropagationLocations(IRotate block, IBlockState state,
                                                         LinkedList<BlockPos> neighbours) {
        EnumFacing.Axis run = BlockChainDrive.getConnectionAxis(state);
        EnumFacing.Axis axis = state.getValue(BlockChainDrive.AXIS);
        for (EnumFacing.AxisDirection dir : EnumFacing.AxisDirection.values()) {
            EnumFacing facing = EnumFacing.getFacingFromAxis(dir, run);
            for (int i = 1; i <= MAX_CHAIN_LENGTH; i++) {
                BlockPos candidate = this.pos.offset(facing, i);
                neighbours.add(candidate);
                if (this.world == null || !this.world.isBlockLoaded(candidate)) break;
                IBlockState candidateState = this.world.getBlockState(candidate);
                if (!(candidateState.getBlock() instanceof BlockChainDrive)) break;
                if (candidateState.getValue(BlockChainDrive.AXIS) != axis) break;
            }
        }
        return neighbours;
    }
}
