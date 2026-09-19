package nl.melonstudios.create.block.generator;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticDirectionalBase;
import nl.melonstudios.create.tileentity.generator.TileEntityHandCrank;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockHandCrank extends BlockKineticDirectionalBase implements ITileEntityProvider {
    public BlockHandCrank(MapColor blockMapColorIn, SoundType soundTypeIn) {
        super(Material.WOOD, blockMapColorIn);
        this.blockSoundType = soundTypeIn;

        // Reference uses wooden properties (SharedProperties::wooden, axeOrPickaxe).
        this.setHardness(BlockProperties.WOOD_HARDNESS);
        this.setResistance(BlockProperties.WOOD_RESISTANCE);
        this.setHarvestLevel("axe", 0);

        this.setRegistryName("hand_crank");
        this.setUnlocalizedName("create.hand_crank");
    }

    public int getRotationSpeed() {
        return 32;
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return state.getValue(FACING).getOpposite() == side;
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityHandCrank();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (playerIn.isSpectator()) return false;

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityHandCrank) {
            ((TileEntityHandCrank)te).turn(playerIn.isSneaking());
        }
        // Reference: causeFoodExhaustion(speed * crankHungerMultiplier), defaults 32 * 0.01.
        playerIn.addExhaustion(0.32F);
        return true;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing preferred = this.getPreferredFacing(world, pos);
        IBlockState defaultState = this.getDefaultState();

        if (preferred == null || placer.isSneaking()) {
            return defaultState.withProperty(FACING, facing);
        }

        return defaultState.withProperty(FACING, preferred.getOpposite());
    }

    private boolean canSurvive(IBlockState state, World world, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        BlockPos offPos = pos.offset(facing.getOpposite());
        IBlockState hello = world.getBlockState(offPos);
        return hello.getCollisionBoundingBox(world, offPos) != Block.NULL_AABB;
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (worldIn.isRemote) return;

        EnumFacing facing = state.getValue(FACING);
        if (fromPos == pos.offset(facing.getOpposite())) {
            if (!this.canSurvive(state, worldIn, pos)) {
                worldIn.destroyBlock(pos, true);
            }
        }
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }
}
