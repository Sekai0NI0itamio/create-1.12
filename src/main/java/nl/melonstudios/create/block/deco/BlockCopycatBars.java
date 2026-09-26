package nl.melonstudios.create.block.deco;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockPane;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
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
 * Copycat bars: connecting iron-bars-style lattice that stores a mimic like
 * the base block.
 *
 * Reference: COPYCAT_BARS (wrenchable directional block with the copycat
 * bars baked model). 1.12 simplification: vanilla BlockPane connection
 * visuals with the neutral copycat texture; mimic storage works, mimic
 * rendering is NEEDS-LEAD client work like the rest of the family.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockCopycatBars extends BlockPane implements ITileEntityProvider {
    public BlockCopycatBars() {
        super(Material.IRON, true);
        this.setRegistryName("copycat_bars");
        this.setUnlocalizedName("create.copycat_bars");
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
