package nl.melonstudios.create.block.logistics;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.logistics.TileEntityPackager;
import nl.melonstudios.create.tileentity.logistics.TileEntityPackagerLink;

import javax.annotation.Nullable;

/**
 * Packager link block: a small plaque mounted facing the player that must
 * stand next to a packager to join it to the addressed order network.
 * Wrenching turns it; right-click reports the joined packager.
 */
@SuppressWarnings("deprecation")
public class BlockPackagerLink extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    public BlockPackagerLink() {
        super(Material.IRON, MapColor.IRON);
        this.setRegistryName("packager_link");
        this.setUnlocalizedName("create.packager_link");
        this.blockSoundType = SoundType.METAL;
        this.setHardness(2.5F);
        this.setResistance(5.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityPackagerLink();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                   EnumHand hand, EnumFacing facing,
                                   float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityPackagerLink)) return true;
        TileEntityPackagerLink link = (TileEntityPackagerLink) te;
        TileEntityPackager packager = link.packager();
        if (packager == null) {
            player.sendStatusMessage(new TextComponentString(
                    world.isBlockPowered(pos)
                            ? "Link muted by redstone; remove the signal."
                            : "No packager adjacent; place this link against one."), true);
        } else {
            String address = packager.address.trim().isEmpty() ? "<no address>" : packager.address;
            player.sendStatusMessage(new TextComponentString(
                    "Linked to packager at " + packager.getPos().toString()
                            + " serving " + address + "."), false);
        }
        return true;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3));
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side,
                              float hitX, float hitY, float hitZ) {
        world.setBlockState(pos, state.withProperty(FACING, state.getValue(FACING).rotateY()), 3);
        return true;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityPackagerLink) return ((TileEntityPackagerLink) te).comparatorLevel();
        return 0;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
