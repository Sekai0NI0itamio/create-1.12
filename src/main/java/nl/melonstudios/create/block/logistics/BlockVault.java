package nl.melonstudios.create.block.logistics;

import com.melonstudios.melonlib.misc.BlockStateProperties;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.logistics.TileEntityVault;
import nl.melonstudios.create.tileentity.logistics.VaultConnectivity;

/**
 * Item vault storage block. Forms multiblocks sharing one inventory,
 * mirroring the 1.20.1 ItemVaultBlock behaviour through 1.12 idioms.
 */
public class BlockVault extends Block implements ITileEntityProvider, IWrenchable {
    public static final PropertyEnum<EnumFacing.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final PropertyBool LARGE = PropertyBool.create("large");

    public BlockVault() {
        super(Material.IRON, MapColor.GRAY);
        this.blockSoundType = SoundType.METAL;

        this.setRegistryName("vault");
        this.setUnlocalizedName("create.vault");

        this.setHardness(3.5F);
        this.setResistance(8.0F);
        this.setHarvestLevel("pickaxe", 1);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(AXIS, EnumFacing.Axis.X)
                .withProperty(LARGE, false));

        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS, LARGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(AXIS, (meta & 1) != 0 ? EnumFacing.Axis.Z : EnumFacing.Axis.X)
                .withProperty(LARGE, (meta & 2) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(AXIS) == EnumFacing.Axis.Z ? 1 : 0;
        if (state.getValue(LARGE)) {
            meta |= 2;
        }
        return meta;
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        EnumFacing.Axis axis = placer.getHorizontalFacing().getAxis();
        if (!placer.isSneaking()) {
            IBlockState against = worldIn.getBlockState(pos.offset(facing.getOpposite()));
            if (against.getBlock() instanceof BlockVault) {
                axis = against.getValue(AXIS);
            }
        }
        return this.getDefaultState().withProperty(AXIS, axis);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityVault) {
            ((TileEntityVault) te).requestForm();
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityVault) {
            ((TileEntityVault) te).requestForm();
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntityVault.dropContents(worldIn, pos);
        super.breakBlock(worldIn, pos, state);
        VaultConnectivity.requestFormAround(worldIn, pos);
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (side.getAxis().isVertical()) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityVault) {
                ((TileEntityVault) te).setMultiblock(null, 1, 1);
            }
            VaultConnectivity.requestFormAround(world, pos);
            return true;
        }
        return false;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityVault) {
            return ((TileEntityVault) te).getComparatorSignal();
        }
        return 0;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityVault();
    }

    public static void updateLargeFlag(World world, BlockPos pos, boolean large) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockVault)) {
            return;
        }
        if (state.getValue(LARGE) != large) {
            world.setBlockState(pos, state.withProperty(LARGE, large), 3);
        }
    }
}
