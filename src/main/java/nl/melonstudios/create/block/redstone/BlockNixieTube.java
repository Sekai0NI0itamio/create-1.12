package nl.melonstudios.create.block.redstone;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;

/**
 * Nixie tube: renders the display-link line behind/above it as glowing text.
 * Pair with a display link (link reads the machine, tube shows the text).
 */
public class BlockNixieTube extends Block implements IWrenchable {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool CEILING = PropertyBool.create("ceiling");

    public BlockNixieTube() {
        super(Material.IRON, MapColor.IRON);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(3.0F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 1);
        this.setLightLevel(5.0F / 15.0F);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(CEILING, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, CEILING);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState()
                .withProperty(FACING, placer.getHorizontalFacing().getOpposite())
                .withProperty(CEILING, facing == EnumFacing.DOWN);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(CEILING)) {
            meta |= 4;
        }
        return meta;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
                .withProperty(CEILING, (meta & 4) != 0);
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (side.getAxis().isVertical()) {
            world.setBlockState(pos, state.withProperty(CEILING, !state.getValue(CEILING)), 3);
        } else {
            world.setBlockState(pos, state.withProperty(FACING, state.getValue(FACING).rotateY()), 3);
        }
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        if (state.getValue(CEILING)) {
            return new AxisAlignedBB(0.25, 0.25, 0.25, 0.75, 1, 0.75);
        }
        return new AxisAlignedBB(0.25, 0, 0.25, 0.75, 0.75, 0.75);
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
