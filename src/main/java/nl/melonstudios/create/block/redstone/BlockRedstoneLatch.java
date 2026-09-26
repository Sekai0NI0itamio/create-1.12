package nl.melonstudios.create.block.redstone;

import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;

import java.util.Random;

@SuppressWarnings("deprecation")
public class BlockRedstoneLatch extends BlockRedstoneDiode {
    public static final PropertyBool SET = PropertyBool.create("set");
    public static final PropertyBool RESET = PropertyBool.create("reset");

    public BlockRedstoneLatch(boolean powered) {
        super(powered);

        this.setDefaultState(this.getDefaultState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(SET, false)
                .withProperty(RESET, false)
        );

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, SET, RESET);
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
            IBlockState newState = (this.isRepeaterPowered ? BlockInit.LATCH : BlockInit.LATCH_POWERED)
                    .getDefaultState()
                    .withProperty(FACING, state.getValue(FACING))
                    .withProperty(SET, state.getValue(SET))
                    .withProperty(RESET, state.getValue(RESET));
            worldIn.setBlockState(pos, newState);
            worldIn.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, this.isRepeaterPowered ? 0.5F : 0.6F);
            this.notifyNeighbors(worldIn, pos, newState);
            return true;
        }
    }

    @Override
    protected int getDelay(IBlockState state) {
        return 2;
    }

    @Override
    protected IBlockState getPoweredState(IBlockState unpoweredState) {
        return BlockInit.LATCH_POWERED.getDefaultState()
                .withProperty(FACING, unpoweredState.getValue(FACING))
                .withProperty(SET, unpoweredState.getValue(SET))
                .withProperty(RESET, unpoweredState.getValue(RESET));
    }

    @Override
    protected IBlockState getUnpoweredState(IBlockState poweredState) {
        return BlockInit.LATCH.getDefaultState()
                .withProperty(FACING, poweredState.getValue(FACING))
                .withProperty(SET, poweredState.getValue(SET))
                .withProperty(RESET, poweredState.getValue(RESET));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(BlockInit.LATCH);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(BlockInit.LATCH));
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        this.notifyNeighbors(worldIn, pos, state);
    }

    @Override
    protected boolean shouldBePowered(World worldIn, BlockPos pos, IBlockState state) {
        return super.shouldBePowered(worldIn, pos, state);
    }
    protected boolean shouldBeUnpowered(World world, BlockPos pos, IBlockState state) {
        return this.calculateSideInputStrength(world, pos, state) > 0;
    }

    protected int calculateSideInputStrength(World world, BlockPos pos, IBlockState state) {
        EnumFacing left = state.getValue(FACING).rotateYCCW();
        EnumFacing right = state.getValue(FACING).rotateY();

        BlockPos leftPos = pos.offset(left);
        int leftPower = world.getRedstonePower(leftPos, left);

        if (leftPower >= 15) {
            return leftPower;
        } else {
            IBlockState leftState = world.getBlockState(leftPos);
            leftPower = Math.max(leftPower, leftState.getBlock() == Blocks.REDSTONE_WIRE ? leftState.getValue(BlockRedstoneWire.POWER) : 0);
            if (leftPower >= 15) return leftPower;
        }

        BlockPos rightPos = pos.offset(right);
        int rightPower = world.getRedstonePower(rightPos, right);

        if (rightPower >= 15) {
            return rightPower;
        } else {
            IBlockState rightState = world.getBlockState(rightPos);
            rightPower = Math.max(rightPower, rightState.getBlock() == Blocks.REDSTONE_WIRE ? rightState.getValue(BlockRedstoneWire.POWER) : 0);
            if (rightPower >= 15) return rightPower;
        }

        return Math.max(leftPower, rightPower);
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        boolean powered = this.shouldBePowered(worldIn, pos, state);
        boolean unpowered = this.shouldBeUnpowered(worldIn, pos, state);

        IBlockState update = state.withProperty(SET, powered).withProperty(RESET, unpowered);
        if (update != state || (powered && !this.isRepeaterPowered) || (unpowered && this.isRepeaterPowered)) {
            // Prioritize powered or unpowered? Unpowered allows for fast NOT-gates I suppose but also very fast clocks that may cause lag
            IBlockState set = unpowered ? this.getUnpoweredState(update) : powered ? this.getPoweredState(update) : update;
            worldIn.setBlockState(pos, set, 2);

            worldIn.updateBlockTick(pos, set.getBlock(), this.getTickDelay(set), -1);
        }
    }

    @Override
    protected void updateState(World worldIn, BlockPos pos, IBlockState state) {
        boolean powered = this.shouldBePowered(worldIn, pos, state);
        boolean unpowered = this.shouldBeUnpowered(worldIn, pos, state);

        if ((state.getValue(SET) != powered || state.getValue(RESET) != unpowered) && !worldIn.isBlockTickPending(pos, this)) {
            int i = -1;

            if (this.isFacingTowardsRepeater(worldIn, pos, state)) {
                i = -3;
            } else if (this.isRepeaterPowered) {
                i = -2;
            }

            worldIn.updateBlockTick(pos, this, this.getDelay(state), i);
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(meta & 0b0011))
                .withProperty(SET, (meta & 0b0100) != 0)
                .withProperty(RESET, (meta & 0b1000) != 0);
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos, @javax.annotation.Nullable EnumFacing side) {
        return side != null && side.getAxis().isHorizontal();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(SET) ? 0b0100 : 0b0000) | (state.getValue(RESET) ? 0b1000 : 0b0000);
    }
}
