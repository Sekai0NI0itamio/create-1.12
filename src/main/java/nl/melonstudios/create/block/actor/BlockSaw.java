package nl.melonstudios.create.block.actor;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.block.state.CreateStateProperties;
import nl.melonstudios.create.block.state.EnumSawRotation;
import nl.melonstudios.create.init.DamageSourceInit;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.tileentity.actor.TileEntitySaw;
import nl.melonstudios.create.tileentity.actor.TileEntitySawProcessing;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.SubInteractionBox;
import nl.melonstudios.create.util.TextBuilder;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("deprecation")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockSaw extends BlockKineticBase implements ITileEntityProvider {
    public static final PropertyEnum<EnumSawRotation> FACING = CreateStateProperties.SAW_ROTATION;

    public BlockSaw(MapColor mapColor, SoundType soundType) {
        super(Material.ROCK, mapColor);
        this.setSoundType(soundType);

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumSawRotation.UP_ALONG_X)
        );
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }


    @Override
    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        if (entityIn instanceof EntityItem) return;
        if (!new AxisAlignedBB(pos).shrink(0.1).intersects(entityIn.getEntityBoundingBox())) return;
        withTEDo(worldIn, pos, TileEntityKinetic.class, te ->{
            if (te.getSpeed() == 0) return;
            entityIn.attackEntityFrom(DamageSourceInit.CUTTING, (float) BlockDrill.getDamage(te.getSpeed()));
        });
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return meta < 4 ? new TileEntitySaw() : meta > 5 ? null : new TileEntitySawProcessing();
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        withTEDo(worldIn, pos, TileEntitySaw.class, TileEntitySaw::destroyNextTick);
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        EnumSawRotation rotation = state.getValue(FACING);
        EnumFacing real = rotation.getToEnumFacing();
        if (real != EnumFacing.UP) {
            return side == real.getOpposite();
        }
        switch (side.getAxis()) {
            case X: return rotation == EnumSawRotation.UP_ALONG_X;
            case Z: return rotation == EnumSawRotation.UP_ALONG_Z;
            default:return false;
        }
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        EnumSawRotation rotation = state.getValue(FACING);
        if (rotation.getToEnumFacing() == EnumFacing.UP) {
            return rotation == EnumSawRotation.UP_ALONG_X ? EnumFacing.Axis.X : EnumFacing.Axis.Z;
        }
        return rotation.getToEnumFacing().getAxis();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getMeta();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumSawRotation.byMeta(meta % 6));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        if (placer.isSneaking()) {
            return this.getDefaultState().withProperty(FACING, EnumSawRotation.findSneakStateWithContext(facing, placer.getHorizontalFacing()));
        }
        return this.getDefaultState().withProperty(FACING, EnumSawRotation.findSneakStateWithContext(placer.getHorizontalFacing(), placer.getHorizontalFacing()));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BlockProperties.CASING_12PX_MAPPED[state.getValue(FACING).getToEnumFacing().getIndex()];
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        if (rot == Rotation.NONE) return state;
       EnumSawRotation sawRotation = state.getValue(FACING);
       if (sawRotation.getToEnumFacing().getAxis() == EnumFacing.Axis.Y) {
           if (rot == Rotation.CLOCKWISE_180) return state;
           switch (sawRotation) {
               case UP_ALONG_X: return state.withProperty(FACING, EnumSawRotation.UP_ALONG_Z);
               case UP_ALONG_Z: return state.withProperty(FACING, EnumSawRotation.UP_ALONG_X);
           }
       }
       EnumFacing real = rot.rotate(sawRotation.getToEnumFacing());
       switch (real) {
           case NORTH: return state.withProperty(FACING, EnumSawRotation.NORTH);
           case EAST: return state.withProperty(FACING, EnumSawRotation.EAST);
           case SOUTH: return state.withProperty(FACING, EnumSawRotation.SOUTH);
           case WEST: return state.withProperty(FACING, EnumSawRotation.WEST);
           default: return state;
       }
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        EnumSawRotation sawRotation = state.getValue(FACING);
        EnumFacing real = sawRotation.getToEnumFacing();
        if (real.getAxis() == EnumFacing.Axis.Y) {
            return state;
        } else {
            EnumFacing newRot = mirrorIn.mirror(real);
            switch (newRot) {
                case NORTH: return state.withProperty(FACING, EnumSawRotation.NORTH);
                case EAST: return state.withProperty(FACING, EnumSawRotation.EAST);
                case SOUTH: return state.withProperty(FACING, EnumSawRotation.SOUTH);
                case WEST: return state.withProperty(FACING, EnumSawRotation.WEST);
                default: return state;
            }
        }
        return state;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return false;
        return SubInteractionBox.handleInteraction(worldIn, pos, playerIn, false,
                playerIn.isSneaking(), playerIn.getHeldItem(hand), hitX, hitY, hitZ);
    }

    @Override
    public List<String> getGoggleInfo(World world, BlockPos pos, IBlockState state) {
        List<String> list = super.getGoggleInfo(world, pos, state);
        if (list.equals(Collections.emptyList())) return list;
        withTEDo(world, pos, TileEntitySawProcessing.class, (te) -> {
            if (!te.currentlyProcessing.isEmpty()) {
                TextBuilder builder = new TextBuilder();
                builder.enter().formatting(TextFormatting.GRAY).translate("goggles.info.currently_processing")
                        .text(": ").enter().space().space()
                        .formatting(TextFormatting.AQUA).text(te.currentlyProcessing.getDisplayName())
                        .enter();
                list.addAll(builder.build());
            }
            if (!te.outputQueue.isEmpty()) {
                TextBuilder builder = new TextBuilder();
                builder.enter().formatting(TextFormatting.RED).translate("goggles.info.clogging")
                        .enter();
                list.addAll(builder.build());
            }
        });
        return list;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        EnumSawRotation sawRotation = state.getValue(FACING);
        if (sawRotation.getToEnumFacing() == EnumFacing.UP) {
            world.setBlockState(pos, state.withProperty(FACING,
                    sawRotation == EnumSawRotation.UP_ALONG_X ? EnumSawRotation.UP_ALONG_Z : EnumSawRotation.UP_ALONG_X)
            );
            return true;
        }
        if (EnumFacing.Axis.Y.apply(side)) {
            world.setBlockState(pos, state.withProperty(FACING,
                    EnumSawRotation.fromEnumFacingHorizontal(sawRotation.getToEnumFacing().rotateY())));
            return true;
        }
        world.setBlockState(pos, state.withProperty(FACING,
                EnumSawRotation.fromEnumFacingHorizontal(side)));
        return true;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || "axe".equals(type);
    }
}
