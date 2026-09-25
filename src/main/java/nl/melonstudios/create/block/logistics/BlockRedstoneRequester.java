package nl.melonstudios.create.block.logistics;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.logistics.TileEntityRedstoneRequester;

import javax.annotation.Nullable;

/**
 * Redstone requester: sneak-use copies the stock below into its encoded order,
 * and any redstone pulse files that order with the addressed packager network.
 */
@SuppressWarnings("deprecation")
public class BlockRedstoneRequester extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockRedstoneRequester() {
        super(Material.IRON, MapColor.IRON);
        this.setRegistryName("redstone_requester");
        this.setUnlocalizedName("create.redstone_requester");
        this.blockSoundType = SoundType.METAL;
        this.setHardness(2.5F);
        this.setResistance(5.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH).withProperty(POWERED, false));
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityRedstoneRequester();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                   EnumHand hand, EnumFacing facing,
                                   float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityRedstoneRequester)) return true;
        TileEntityRedstoneRequester requester = (TileEntityRedstoneRequester) te;
        if (player.isSneaking()) {
            int n = requester.captureFromBelow();
            if (n == 0) {
                world.playSound(null, pos, SoundInit.deny, SoundCategory.BLOCKS, 1.0F, 1.0F);
                player.sendStatusMessage(new TextComponentString(
                        "No stocked inventory below to copy an order from."), true);
            } else {
                world.playSound(null, pos, SoundInit.confirm, SoundCategory.BLOCKS, 1.0F, 1.0F);
                String address = requester.address.trim().isEmpty() ? "<no address>" : requester.address;
                player.sendStatusMessage(new TextComponentString(
                        "Encoded " + n + " request(s) for " + address + "; pulse redstone to order."), true);
            }
            return true;
        }
        String address = requester.address.trim().isEmpty() ? "<no address>" : requester.address;
        String order = requester.hasOrder()
                ? requester.encodedCount() + " encoded request(s)" : "no encoded order";
        String last = requester.hasOrder()
                ? (requester.lastSuccess ? ", last pulse ordered." : ", last pulse found no packager.")
                : "";
        player.sendStatusMessage(new TextComponentString(
                "Requester for " + address + ": " + order + last
                        + " (sneak-use copies stock below)."), false);
        return true;
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        worldIn.setBlockState(pos, state.withProperty(POWERED, worldIn.isBlockPowered(pos)), 2);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        boolean powered = worldIn.isBlockPowered(pos);
        if (state.getValue(POWERED) == powered) return;
        worldIn.setBlockState(pos, state.withProperty(POWERED, powered), 3);
        if (!worldIn.isRemote && powered) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityRedstoneRequester) {
                ((TileEntityRedstoneRequester) te).triggerRequest();
            }
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(POWERED)) meta |= 4;
        return meta;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
                .withProperty(POWERED, (meta & 4) != 0);
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
        if (te instanceof TileEntityRedstoneRequester) return ((TileEntityRedstoneRequester) te).comparatorLevel();
        return 0;
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, net.minecraft.world.IBlockAccess world,
                                      BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
