package nl.melonstudios.create.block;

import com.melonstudios.melonlib.misc.AABB;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.state.EnumChuteVariant;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.TileEntityChute;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.TextBuilder;
import nl.melonstudios.create.util.interfaces.IGoggleInfo;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class BlockChute extends Block implements ITileEntityProvider, IWrenchable, IGoggleInfo {
    public static final PropertyEnum<EnumChuteVariant> VARIANT = PropertyEnum.create("variant", EnumChuteVariant.class);

    public static final AxisAlignedBB BOX = AABB.create(1, 0, 1, 15, 16, 15);

    public BlockChute() {
        super(Material.IRON);
        this.blockSoundType = SoundType.METAL;

        this.setHardness(3.0F);
        this.setResistance(BlockProperties.IRON_RESISTANCE);
        this.setHarvestLevel("pickaxe", 1);

        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityChute();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOptimizedBase) ((TileEntityOptimizedBase)te).destroy();
        if (this.hasTileEntity(state)) worldIn.removeTileEntity(pos);
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        EnumChuteVariant variant = state.getValue(VARIANT);
        if (variant == EnumChuteVariant.FAT) {
            world.setBlockState(pos, state.withProperty(VARIANT, EnumChuteVariant.NORMAL));
        } else {
            world.setBlockState(pos, state.withProperty(VARIANT, variant.getId() == 0 ? EnumChuteVariant.WINDOW : EnumChuteVariant.NORMAL));
        }
        if (te != null) {
            te.validate();
            world.setTileEntity(pos, te);
        }
        return true;
    }

    @Override
    public List<String> getGoggleInfo(World world, BlockPos pos, IBlockState state) {
        List<String> list =  BlockKineticBase.withTEDo(world, pos, TileEntityChute.class, (te) -> {
            if (te.stack.isEmpty()) return Collections.emptyList();
            TextBuilder builder = new TextBuilder();
            builder.translate("goggles.inventory_contents").enter();
            builder.space().space().formatting(TextFormatting.GRAY).text(te.stack.getCount() + "x ")
                    .formatting(TextFormatting.AQUA).text(te.stack.getDisplayName()).enter();
            return builder.build();
        });
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getId();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, EnumChuteVariant.VALUES[meta % 3]);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(VARIANT) == EnumChuteVariant.FAT ? FULL_BLOCK_AABB : BOX;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    //region this is not a full block
    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isTranslucent(IBlockState state) {
        return true;
    }

    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
    //endregion
}
