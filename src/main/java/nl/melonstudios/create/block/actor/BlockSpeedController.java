package nl.melonstudios.create.block.actor;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.tileentity.actor.TileEntitySpeedController;

import javax.annotation.Nullable;

/**
 * Rotation speed controller: outputs the player-set RPM regardless of input
 * speed (needs any powered large cogwheel adjacent). Right-click cycles the
 * target through the standard speed ladder; sneak-click goes down.
 */
@SuppressWarnings("deprecation")
public class BlockSpeedController extends BlockKineticBase implements ITileEntityProvider {
    public BlockSpeedController() {
        super(Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySpeedController();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntitySpeedController) {
            TileEntitySpeedController sc = (TileEntitySpeedController) te;
            if (player.isSneaking()) sc.speedIndex = Math.max(sc.speedIndex - 1, 0);
            else sc.speedIndex = Math.min(sc.speedIndex + 1, TileEntitySpeedController.SPEEDS.length - 1);
            sc.updateGeneratedRotation();
            sc.sync();
            world.playSound(null, pos, SoundInit.scroll_value, SoundCategory.BLOCKS, 0.25F, 1.2F);
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    "Target speed: " + (int) TileEntitySpeedController.SPEEDS[sc.speedIndex] + " RPM"), true);
        }
        return true;
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return EnumFacing.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side != EnumFacing.DOWN;
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
