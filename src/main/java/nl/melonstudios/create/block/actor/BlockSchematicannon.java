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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntitySchematicannon;

import javax.annotation.Nullable;

/**
 * Schematicannon: prints the loaded schematic with gunpowder + materials
 * from adjacent inventories. Right-click toggles run/pause; sneak-click
 * stops and clears.
 */
@SuppressWarnings("deprecation")
public class BlockSchematicannon extends BlockKineticBase implements ITileEntityProvider {
    public BlockSchematicannon() {
        super(Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;
        this.setHardness(3.5F);
        this.setResistance(6.0F);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySchematicannon();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntitySchematicannon) {
            TileEntitySchematicannon cannon = (TileEntitySchematicannon) te;
            if (player.isSneaking()) cannon.stop();
            else cannon.toggle();
            cannon.sync();
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString("Cannon: " + cannon.status()), true);
        }
        return true;
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
