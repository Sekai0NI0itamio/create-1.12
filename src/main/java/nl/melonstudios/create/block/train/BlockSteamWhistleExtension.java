package nl.melonstudios.create.block.train;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.train.BlockSteamWhistle.WhistleSize;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;

/**
 * Steam whistle extension: stackable tube above a whistle, up to 6 high.
 * Each level lowers the hoot pitch by 2 semitones (reference rule); a second
 * click on a SINGLE segment widens it to DOUBLE instead of growing taller.
 */
@SuppressWarnings("deprecation")
public class BlockSteamWhistleExtension extends Block implements IWrenchable {
    public static final PropertyEnum<WhistleSize> SIZE = PropertyEnum.create("size", WhistleSize.class);
    public static final PropertyEnum<Shape> SHAPE = PropertyEnum.create("shape", Shape.class);

    public enum Shape implements net.minecraft.util.IStringSerializable {
        SINGLE, DOUBLE;

        @Override
        public String getName() {
            return this.name().toLowerCase();
        }
    }

    public BlockSteamWhistleExtension() {
        super(Material.IRON, MapColor.GOLD);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(1.5F);
        this.setResistance(6.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(ItemInit.TAB_CREATE);
        this.setRegistryName("steam_whistle_extension");
        this.setUnlocalizedName("create.steam_whistle_extension");
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(SIZE, WhistleSize.MEDIUM)
                .withProperty(SHAPE, Shape.SINGLE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, SIZE, SHAPE);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(SIZE).ordinal() * 2 + state.getValue(SHAPE).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        WhistleSize[] sizes = WhistleSize.values();
        return this.getDefaultState()
                .withProperty(SIZE, sizes[Math.min(meta / 2, sizes.length - 1)])
                .withProperty(SHAPE, (meta & 1) == 1 ? Shape.DOUBLE : Shape.SINGLE);
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY,
                             float hitZ) {
        if (world.isRemote) return true;
        if (state.getValue(SHAPE) == Shape.DOUBLE) {
            world.setBlockState(pos, state.withProperty(SHAPE, Shape.SINGLE), 3);
            return true;
        }
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        if (state.getValue(SHAPE) == Shape.DOUBLE) {
            return new AxisAlignedBB(0.1875, 0, 0.1875, 0.8125, 1.0, 0.8125);
        }
        return new AxisAlignedBB(0.3125, 0, 0.3125, 0.6875, 1.0, 0.6875);
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
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
