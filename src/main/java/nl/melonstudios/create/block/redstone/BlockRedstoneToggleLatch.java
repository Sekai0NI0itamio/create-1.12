package nl.melonstudios.create.block.redstone;

import nl.melonstudios.create.init.CreateTabs;
import com.melonstudios.melonlib.misc.BlockStateProperties;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;
import java.util.Random;

@SuppressWarnings("deprecation")
public class BlockRedstoneToggleLatch extends BlockRedstoneDiode {
    public static final PropertyBool POWERED = BlockStateProperties.POWERED;

    public BlockRedstoneToggleLatch(boolean powered) {
        super(powered);

        this.setDefaultState(this.getDefaultState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(POWERED, false)
        );

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!playerIn.capabilities.allowEdit) {
            return false;
        } else {
            IBlockState newState = (this.isRepeaterPowered ? BlockInit.TOGGLE_LATCH : BlockInit.TOGGLE_LATCH_POWERED)
                    .getDefaultState()
                    .withProperty(FACING, state.getValue(FACING))
                    .withProperty(POWERED, state.getValue(POWERED));
            worldIn.setBlockState(pos, newState);
            worldIn.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, this.isRepeaterPowered ? 0.5F : 0.6F);
            this.notifyNeighbors(worldIn, pos, newState);
            return true;
        }
    }

    @Override
    protected int getDelay(IBlockState state) {
        return 1;
    }

    @Override
    protected IBlockState getPoweredState(IBlockState unpoweredState) {
        return BlockInit.TOGGLE_LATCH_POWERED.getDefaultState()
                .withProperty(FACING, unpoweredState.getValue(FACING))
                .withProperty(POWERED, unpoweredState.getValue(POWERED));
    }

    @Override
    protected IBlockState getUnpoweredState(IBlockState poweredState) {
        return BlockInit.TOGGLE_LATCH.getDefaultState()
                .withProperty(FACING, poweredState.getValue(FACING))
                .withProperty(POWERED, poweredState.getValue(POWERED));
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        boolean input = this.shouldBePowered(worldIn, pos, state);
        boolean memorized = state.getValue(POWERED);
        if (input && !memorized) {
            // Rising edge of the back input: latch it and toggle the output.
            IBlockState toggled = (this.isRepeaterPowered ? BlockInit.TOGGLE_LATCH : BlockInit.TOGGLE_LATCH_POWERED)
                    .getDefaultState()
                    .withProperty(FACING, state.getValue(FACING))
                    .withProperty(POWERED, true);
            worldIn.setBlockState(pos, toggled, 2);
        } else if (!input && memorized) {
            worldIn.setBlockState(pos, state.withProperty(POWERED, false), 2);
        }
    }

    @Override
    protected void updateState(World worldIn, BlockPos pos, IBlockState state) {
        // Gated on the input MEMORY (POWERED prop), not on the output variant:
        // the output block intentionally diverges from the back input, so the
        // vanilla diode check (input vs isRepeaterPowered) would stop scheduling
        // ticks while input == output and swallow every second rising edge.
        boolean input = this.shouldBePowered(worldIn, pos, state);
        boolean memorized = state.getValue(POWERED);

        if (input != memorized && !worldIn.isBlockTickPending(pos, this)) {
            worldIn.updateBlockTick(pos, this, this.getDelay(state), -1);
        }
    }

    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        if (this.isRepeaterPowered) {
            EnumFacing enumfacing = stateIn.getValue(FACING);
            double d0 = (double)((float)pos.getX() + 0.5F) + (double)(rand.nextFloat() - 0.5F) * 0.2D;
            double d1 = (double)((float)pos.getY() + 0.4F) + (double)(rand.nextFloat() - 0.5F) * 0.2D;
            double d2 = (double)((float)pos.getZ() + 0.5F) + (double)(rand.nextFloat() - 0.5F) * 0.2D;
            float f = -6.0F;

            f = f / 16.0F;
            double d3 = f * enumfacing.getFrontOffsetX();
            double d4 = f * enumfacing.getFrontOffsetZ();
            worldIn.spawnParticle(EnumParticleTypes.REDSTONE, d0 + d3, d1, d2 + d4, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        if (!this.isRepeaterPowered) {
            return 0;
        } else {
            return blockState.getValue(FACING) == side ? this.getActiveSignal(blockAccess, pos, blockState) : 0;
        }
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EnumFacing side) {
        return side != null && state.getValue(FACING).getAxis() == side.getAxis();
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(BlockInit.TOGGLE_LATCH);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(BlockInit.TOGGLE_LATCH));
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        this.notifyNeighbors(worldIn, pos, state);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(meta & 0b0011))
                .withProperty(POWERED, (meta & 0b0100) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(POWERED) ? 0b0100 : 0b0000);
    }
}
