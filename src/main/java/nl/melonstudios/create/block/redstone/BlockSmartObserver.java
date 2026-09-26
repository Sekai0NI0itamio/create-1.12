package nl.melonstudios.create.block.redstone;

import nl.melonstudios.create.init.CreateTabs;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.redstone.TileEntitySmartObserver;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Smart observer: watches the inventory or tank on its facing side and pulses
 * full power for 6 ticks (sustained while stock is present) whenever the
 * observed contents change. Power is emitted on every side except the back
 * face, translated from the reference rules.
 */
@SuppressWarnings("deprecation")
public class BlockSmartObserver extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockSmartObserver() {
        super(Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;
        this.setHardness(3.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("smart_observer");
        this.setUnlocalizedName("create.smart_observer");
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(POWERED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getFront(meta & 7))
                .withProperty(POWERED, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() | (state.getValue(POWERED) ? 8 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing preferred = this.findInventorySide(world, pos);
        if (preferred != null) return this.getDefaultState().withProperty(FACING, preferred);
        EnumFacing look = placer.getHorizontalFacing();
        boolean sneak = placer.isSneaking();
        return this.getDefaultState().withProperty(FACING, sneak ? look : look.getOpposite());
    }

    @Nullable
    private EnumFacing findInventorySide(World world, BlockPos pos) {
        for (EnumFacing side : EnumFacing.VALUES) {
            TileEntity te = world.getTileEntity(pos.offset(side));
            if (te == null) continue;
            if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) return side;
            if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite())) return side;
        }
        return null;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                   EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return false;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side,
                             float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        EnumFacing next = EnumFacing.getFront((state.getValue(FACING).getIndex() + 1) % 6);
        Utils.setBlockTESafe(world, pos, state.withProperty(FACING, next), 3);
        TileEntitySmartObserver observer =
                Utils.cast(world.getTileEntity(pos), TileEntitySmartObserver.class);
        if (observer != null) observer.observe();
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntitySmartObserver();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        world.removeTileEntity(pos);
        if (state.getValue(POWERED)) {
            world.notifyNeighborsOfStateChange(pos, this, false);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileEntitySmartObserver observer =
                Utils.cast(world.getTileEntity(pos), TileEntitySmartObserver.class);
        if (observer != null) observer.observe();
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (state.getValue(POWERED)) {
            Utils.setBlockTESafe(world, pos, state.withProperty(POWERED, false), 2);
            world.notifyNeighborsOfStateChange(pos, this, false);
        }
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return state.getValue(POWERED);
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        if (!state.getValue(POWERED)) return 0;
        if (side != null && side == state.getValue(FACING).getOpposite()) return 0;
        return 15;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        return this.getWeakPower(state, access, pos, side);
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos,
                                      @Nullable EnumFacing side) {
        return side != null && side != state.getValue(FACING).getOpposite();
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
