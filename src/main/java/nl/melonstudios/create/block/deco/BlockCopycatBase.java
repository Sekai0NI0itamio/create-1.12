package nl.melonstudios.create.block.deco;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.deco.TileEntityCopycat;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Copycat base block: a plain full cube that adopts the appearance of any
 * solid block applied with right-click.
 *
 * Reference: AllBlocks COPYCAT_BASE (soft metal, pickaxe, cutout layer) plus
 * the CopycatBlock mimic store/consume/clear interaction. Rendering the
 * stored mimic needs a custom baked model; this port stores the data in
 * TileEntityCopycat and renders the neutral copycat_base texture until that
 * client work lands (see NEEDS-LEAD).
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockCopycatBase extends Block implements ITileEntityProvider {
    public BlockCopycatBase() {
        super(Material.IRON, MapColor.LIGHT_BLUE);
        this.setRegistryName("copycat_base");
        this.setUnlocalizedName("create.copycat_base");
        this.setSoundType(SoundType.METAL);
        this.setHardness(1.5F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreateTabs.TAB_CREATE_DECORATIONS);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityCopycat();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntityCopycat copycat = CopycatMimicHelper.getCopycat(world, pos);
        if (copycat == null) return false;
        if (world.isRemote) return copycat.hasMimic() || CopycatMimicHelper.isValidMimicTarget(player.getHeldItem(hand));
        return CopycatMimicHelper.onActivated(world, pos, player, hand);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        CopycatMimicHelper.dropMimic(world, pos, state);
        super.breakBlock(world, pos, state);
    }
}
