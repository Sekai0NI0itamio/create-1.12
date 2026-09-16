package nl.melonstudios.create.block.redstone;

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
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.tileentity.redstone.TileEntityDisplayLink;

import javax.annotation.Nullable;

/**
 * Display link: reads a value from the block behind it (speed gauge,
 * stress gauge, tank, depot, basin, boiler) and shows it on the attached
 * display (nixie tube / display board / sign in front). Right-click cycles
 * the source mode; sneak-click toggles label.
 */
@SuppressWarnings("deprecation")
public class BlockDisplayLink extends BlockKineticBase implements ITileEntityProvider {
    public BlockDisplayLink() {
        super(Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityDisplayLink();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityDisplayLink) {
            TileEntityDisplayLink link = (TileEntityDisplayLink) te;
            if (player.isSneaking()) link.showLabel = !link.showLabel;
            else link.mode = (link.mode + 1) % TileEntityDisplayLink.MODENAMES.length;
            link.sync();
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    "Display: " + TileEntityDisplayLink.MODENAMES[link.mode] + (link.showLabel ? " (labeled)" : "")), true);
        }
        return true;
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return EnumFacing.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return false;
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
