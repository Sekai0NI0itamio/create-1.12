package nl.melonstudios.create.block.generator;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticDirectionalBase;
import nl.melonstudios.create.tileentity.generator.TileEntityCreativeMotor;

import javax.annotation.Nullable;

public class BlockCreativeMotor extends BlockKineticDirectionalBase implements ITileEntityProvider {
    public BlockCreativeMotor() {
        super(Material.IRON, MapColor.MAGENTA);
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side == state.getValue(FACING);
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (facing == state.getValue(FACING).getOpposite()) {
            // Mutate on the server only; the TE sync carries it to clients.
            if (!worldIn.isRemote && !playerIn.isSpectator()) {
                TileEntity te = worldIn.getTileEntity(pos);
                if (te instanceof TileEntityCreativeMotor) {
                    TileEntityCreativeMotor motor = (TileEntityCreativeMotor) te;
                    motor.cycleSpeedIndex(playerIn.isSneaking() ? -1 : 1);
                    motor.updateGeneratedRotation();
                    motor.sync();
                }
            }
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityCreativeMotor();
    }

    @Nullable
    @Override
    public EnumFacing getPreferredFacing(World world, BlockPos pos) {
        EnumFacing ret = super.getPreferredFacing(world, pos);
        return ret != null ? ret.getOpposite() : null;
    }
}
