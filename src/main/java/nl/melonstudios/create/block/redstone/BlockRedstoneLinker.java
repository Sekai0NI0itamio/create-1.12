package nl.melonstudios.create.block.redstone;

import com.melonstudios.melonlib.misc.BlockStateProperties;
import net.minecraft.block.*;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.savedata.WorldRedstoneSignals;
import nl.melonstudios.create.tileentity.redstone.TileEntityRedstoneLinker;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import java.util.Random;

@SuppressWarnings("deprecation")
public class BlockRedstoneLinker extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyDirection FACING = BlockStateProperties.FACING;
    public static final PropertyBool POWERED = BlockStateProperties.POWERED;

    public final boolean isReceiving;

    public BlockRedstoneLinker(boolean isReceiving) {
        super(Material.ROCK, MapColor.WOOD);
        this.blockSoundType = SoundType.WOOD;

        this.blockHardness = BlockProperties.WOOD_HARDNESS;
        this.blockResistance = BlockProperties.WOOD_RESISTANCE;

        this.setHarvestLevel("axe", -1);

        this.isReceiving = isReceiving;

        this.setDefaultState(this.getDefaultState()
                .withProperty(FACING, EnumFacing.UP)
                .withProperty(POWERED, false)
        );

        //this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED);
    }

    protected boolean canBlockStay(World world, BlockPos pos, EnumFacing facing) {
        BlockPos off = pos.offset(facing.getOpposite());
        return world.getBlockState(off).getBlockFaceShape(world, off, facing) == BlockFaceShape.SOLID;
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (this.canBlockStay(worldIn, pos, state.getValue(FACING))) {
            this.updateState(worldIn, pos, state);
        } else {
            this.dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);

            for (EnumFacing facing : EnumFacing.VALUES) {
                worldIn.notifyNeighborsOfStateChange(pos.offset(facing), this, false);
            }
        }
    }

    protected void updateState(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote || this.isReceiving) return;
        TileEntityRedstoneLinker linker = Utils.cast(world.getTileEntity(pos), TileEntityRedstoneLinker.class);
        if (linker != null && linker.frequency != WorldRedstoneSignals.LinkFrequency.EMPTY) {
            int old = linker.signal;
            int power = BlockKineticBase.getPosPower(world, pos);
            if (old != power) {
                linker.signal = power;
                Utils.setBlockTESafe(world, pos, state.withProperty(POWERED, power > 0), 2);
                WorldRedstoneSignals signals = WorldRedstoneSignals.get(world);
                signals.setSignal(linker.frequency, pos, power);
                WorldRedstoneSignals.updateAllLinksOfFreq(world, signals, linker.frequency);
                for (EnumFacing facing : EnumFacing.VALUES) {
                    world.notifyNeighborsOfStateChange(pos.offset(facing), this, false);
                }
            }
        }
    }

    protected IBlockState getReceiver(IBlockState state) {
        return BlockInit.REDSTONE_LINKER_RECEIVER.getDefaultState()
                .withProperty(FACING, state.getValue(FACING))
                .withProperty(POWERED, state.getValue(POWERED));
    }
    protected IBlockState getTransmitter(IBlockState state) {
        return BlockInit.REDSTONE_LINKER.getDefaultState()
                .withProperty(FACING, state.getValue(FACING))
                .withProperty(POWERED, state.getValue(POWERED));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntityRedstoneLinker te = Utils.cast(worldIn.getTileEntity(pos), TileEntityRedstoneLinker.class);
        worldIn.removeTileEntity(pos);
        if (te != null) {
            WorldRedstoneSignals signals = WorldRedstoneSignals.get(worldIn);
            synchronized (signals.receivers) {
                signals.receivers.remove(te);
            }
            signals.setSignal(te.frequency, pos, 0);
        }
    }

    @Override
    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side) {
        return this.canBlockStay(worldIn, pos, side);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() | (state.getValue(POWERED) ? 0b1000 : 0b0000);
    }
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.VALUES[meta & 0b0111])
                .withProperty(POWERED, (meta & 0b1000) != 0);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(BlockInit.REDSTONE_LINKER);
    }
    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(BlockInit.REDSTONE_LINKER));
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "axe".equals(type) || "pickaxe".equals(type);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityRedstoneLinker();
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EnumFacing side) {
        return side != null;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return this.isReceiving;
    }

    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return this.getWeakPower(blockState, blockAccess, pos, side);
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        TileEntityRedstoneLinker te = Utils.cast(blockAccess.getTileEntity(pos), TileEntityRedstoneLinker.class);
        return te != null ? te.signal : 0;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        IBlockState newState = this.isReceiving ?
                this.getTransmitter(state).withProperty(POWERED, false) :
                this.getReceiver(state).withProperty(POWERED, false);
        Utils.setBlockTESafe(world, pos, newState, 3);
        if (((BlockRedstoneLinker)newState.getBlock()).isReceiving) {
            TileEntityRedstoneLinker te = Utils.cast(world.getTileEntity(pos), TileEntityRedstoneLinker.class);
            if (te != null && te.frequency != WorldRedstoneSignals.LinkFrequency.EMPTY) {
                WorldRedstoneSignals signals = WorldRedstoneSignals.get(world);
                te.updateSignal(signals.getSignal(te.frequency, te.getPos()));
            }
        } else {
            ((BlockRedstoneLinker)newState.getBlock()).updateState(world, pos, newState);
        }
        return true;
    }
}
