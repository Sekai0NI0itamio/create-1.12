package nl.melonstudios.create.block.train;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import nl.melonstudios.create.tileentity.train.TileEntityStation;

import javax.annotation.Nullable;

/**
 * Train station: right-click assembles the bogeys + frames above it into a
 * train; sneak-click disassembles. Trains run the track loop (schedule:
 * loop with timed stops, set by clicking with stations named in order).
 */
@SuppressWarnings("deprecation")
public class BlockStation extends Block implements ITileEntityProvider {
    public BlockStation() {
        super(Material.IRON, MapColor.IRON);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(3.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 1);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityStation();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityStation) {
            TileEntityStation station = (TileEntityStation) te;
            if (player.isSneaking()) station.disassemble();
            else station.assemble();
            station.sync();
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(station.status()), true);
        }
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        // Reference STATION shape: base plate (0,0,0,16,2,16) + body (1,0,1,15,13,15);
        // 1.12 takes a single box, so return their union (height 13/16).
        return new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 13.0D / 16.0D, 1.0D);
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
