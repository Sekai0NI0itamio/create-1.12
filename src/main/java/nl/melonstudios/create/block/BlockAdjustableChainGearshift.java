package nl.melonstudios.create.block;

import com.melonstudios.melonlib.misc.BlockStateProperties;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.TileEntityAdjustableChainGearshift;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;

/**
 * Adjustable chain gearshift (translated from the reference
 * ChainGearshiftBlock, MIT). A chain drive whose bridge ratio follows the
 * analog redstone signal: unpowered 1:1, otherwise 1 + (signal + 1) / 16
 * (see TileEntityAdjustableChainGearshift). The POWERED flag mirrors
 * signal > 0 for the model; PART stays derived via getActualState.
 */
public class BlockAdjustableChainGearshift extends BlockChainDrive {
    public static final PropertyBool POWERED = BlockStateProperties.POWERED;

    public BlockAdjustableChainGearshift(Material blockMaterialIn, MapColor blockMapColorIn) {
        super(blockMaterialIn, blockMapColorIn);

        this.setRegistryName("adjustable_chain_gearshift");
        this.setUnlocalizedName("create.adjustable_chain_gearshift");

        this.setSoundType(SoundType.WOOD);
        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);

        this.setHarvestLevel("axe", 0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(AXIS, EnumFacing.Axis.Y)
                .withProperty(PART, Part.NONE)
                .withProperty(POWERED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS, PART, POWERED);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand);
        try {
            return state.withProperty(POWERED, world.isBlockPowered(pos));
        } catch (Exception e) {
            return state;
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (worldIn.isRemote) return;
        boolean powered = worldIn.isBlockPowered(pos);
        if (powered != state.getValue(POWERED)) {
            worldIn.setBlockState(pos, state.withProperty(POWERED, powered), 18);
        }
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityAdjustableChainGearshift) {
            ((TileEntityAdjustableChainGearshift) te).pollSignal();
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityAdjustableChainGearshift();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return super.getMetaFromState(state) | (state.getValue(POWERED) ? 0b0100 : 0);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta).withProperty(POWERED, (meta & 0b0100) != 0);
    }
}
