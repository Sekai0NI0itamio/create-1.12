package nl.melonstudios.create.block;

import nl.melonstudios.create.init.CreateTabs;
import com.melonstudios.melonlib.blockdict.BlockDictionary;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.entity.EntityGlue;
import nl.melonstudios.create.extensions.IExtensionBlock;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.GluedSurface;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.TileEntityDistanceController;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.SubInteractionBox;
import nl.melonstudios.create.util.Utils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockChassisRadial extends BlockRotatedPillar implements IExtensionBlock, IWrenchable, ITileEntityProvider {
    public BlockChassisRadial() {
        super(Material.ROCK, MapColor.WOOD);
        this.setSoundType(SoundType.WOOD);

        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);
        this.setHarvestLevel("pickaxe", 0);

        this.setDefaultState(this.blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS);
    }

    @Override
    public void create$addStickyLocations(World world, BlockPos pos, IBlockState state, List<BlockPos> positions) {
        EnumFacing.Axis axis = state.getValue(AXIS);

        BlockPos negative = pos.offset(EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.NEGATIVE, axis));
        BlockPos positive = pos.offset(EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, axis));
        if (world.getBlockState(negative) == state) positions.add(negative);
        if (world.getBlockState(positive) == state) positions.add(positive);

        boolean[] glues = new boolean[6];
        List<EnumFacing> sides = Utils.getSurrounding(axis);
        for (EnumFacing side : sides) {
            GluedSurface surface = new GluedSurface(pos, side);
            glues[side.getIndex()] = !world.getEntities(EntityGlue.class, (glue) -> surface.equals(glue.getSurface())).isEmpty();
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        attachment:
        {
            optimization:
            {
                for (boolean glue : glues) {
                    if (glue) break optimization;
                }
                break attachment; //Ignore next steps if there is no glue at all
            }

            int dist = this.getConnectionDistance(world, pos);

            List<BlockPos> wheel = new ArrayList<>();
            for (EnumFacing side : sides) {
                if (glues[side.getIndex()]) {
                    mutable.setPos(pos).move(side);
                    if (validPropagation(world, pos, mutable)) {
                        BlockPos next = mutable.toImmutable();
                        wheel.add(next);
                        this.propagate(world, wheel, pos, next, mutable, dist * dist + 1, sides);
                    }
                }
            }

            positions.addAll(wheel);
            wheel.clear();
        }

        for (EnumFacing side : sides) {
            if (!glues[side.getIndex()]) {
                mutable.setPos(pos).move(side);
                if (!positions.contains(mutable)) {
                    if (BlockDictionary.isBlockTagged(world.getBlockState(mutable), "create:autoAttachToChassisRadial")) {
                        positions.add(mutable.toImmutable());
                    }
                }
            }
        }
    }

    private void propagate(
            World world, List<BlockPos> positions, BlockPos middle,
            BlockPos src, BlockPos.MutableBlockPos mutable,
            int maxDist, List<EnumFacing> sides
    ) {
        for (EnumFacing side : sides) {
            mutable.setPos(src).move(side);
            if (validPropagation(world, src, mutable) && middle.distanceSq(mutable) < maxDist) {
                if (!positions.contains(mutable)) {
                    BlockPos next = mutable.toImmutable();
                    positions.add(next);
                    this.propagate(world, positions, middle, next, mutable, maxDist, sides);
                }
            }
        }
    }

    private static boolean validPropagation(World world, BlockPos pos) {
        return !world.getBlockState(pos).getBlock().isReplaceable(world, pos);
    }
    private static boolean validPropagation(World world, BlockPos from, BlockPos to) {
        IBlockState state = world.getBlockState(to);
        if (state.getBlock().isReplaceable(world, to)) return false;
        if (state.getBlock() instanceof BlockBush) {
            return from.getX() == to.getX() && from.getZ() == to.getZ() && from.getY() == to.getY() - 1;
        }
        return true;
    }

    private int getConnectionDistance(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileEntityDistanceController ? ((TileEntityDistanceController)te).setDistance : 8;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (state.getValue(AXIS).apply(side)) return false;
        EnumFacing.Axis rotated = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, state.getValue(AXIS))
                .rotateAround(side.getAxis()).getAxis();
        Utils.setBlockTESafe(world, pos, state.withProperty(AXIS, rotated), 3);
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityDistanceController();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return false;
        return SubInteractionBox.handleInteraction(worldIn, pos, playerIn, false,
                playerIn.isSneaking(), playerIn.getHeldItem(hand), hitX, hitY, hitZ);
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || "axe".equals(type);
    }
}
