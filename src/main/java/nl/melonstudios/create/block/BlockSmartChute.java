package nl.melonstudios.create.block;

import nl.melonstudios.create.init.CreateTabs;
import com.melonstudios.melonlib.misc.AABB;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.state.EnumChuteVariant;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.TileEntitySmartChute;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.TextBuilder;
import nl.melonstudios.create.util.filter.ItemFilterExact;
import nl.melonstudios.create.util.interfaces.IGoggleInfo;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

/**
 * Smart chute: a chute that only pulls items matching a ghost filter,
 * and only while unpowered.
 *
 * <p>Paraphrased from the reference SmartChuteBlock: POWERED mirrors the
 * vanilla redstone check (refreshed one tick after any neighbour change so
 * dust updates settle first), and extraction is gated on the unpowered
 * state. The reference filter dialogue (count, up-to/exactly) is folded
 * into the backport funnel idiom instead: right-click with an item sets a
 * ghost filter, sneak + empty hand clears, wrench toggles exact mode, and
 * sneak + wrench steps the dialled amount 64..1.</p>
 *
 * <p>Meta packs variant id (0-2) plus the powered bit, the same shape the
 * reference keeps on its state.</p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class BlockSmartChute extends Block implements ITileEntityProvider, IWrenchable, IGoggleInfo {
    public static final PropertyEnum<EnumChuteVariant> VARIANT = PropertyEnum.create("variant", EnumChuteVariant.class);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public static final AxisAlignedBB BOX = AABB.create(1, 0, 1, 15, 16, 15);

    public BlockSmartChute() {
        super(Material.IRON);
        this.blockSoundType = SoundType.METAL;

        this.setHardness(3.0F);
        this.setResistance(BlockProperties.IRON_RESISTANCE);
        this.setHarvestLevel("pickaxe", 1);

        this.setRegistryName("smart_chute");
        this.setUnlocalizedName("create.smart_chute");
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(VARIANT, EnumChuteVariant.NORMAL)
                .withProperty(POWERED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT, POWERED);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySmartChute();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOptimizedBase) ((TileEntityOptimizedBase) te).destroy();
        if (this.hasTileEntity(state)) worldIn.removeTileEntity(pos);
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        EnumChuteVariant variant = state.getValue(VARIANT);
        if (variant == EnumChuteVariant.FAT) {
            world.setBlockState(pos, state.withProperty(VARIANT, EnumChuteVariant.NORMAL));
        } else {
            world.setBlockState(pos, state.withProperty(VARIANT, variant.getId() == 0 ? EnumChuteVariant.WINDOW : EnumChuteVariant.NORMAL));
        }
        TileEntity te = world.getTileEntity(pos);
        if (te != null) {
            te.validate();
            world.setTileEntity(pos, te);
        }
        return true;
    }

    /**
     * Ghost-filter interaction (backport funnel idiom): held item sets the
     * filter, empty hand clears it when sneaking, wrench toggles
     * up-to/exactly, sneak + wrench steps the dialled amount
     * (64 -&gt; 32 -&gt; ... -&gt; 1 -&gt; 64, backport sneak-click idiom).
     */
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (playerIn.isSpectator()) return false;
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntitySmartChute)) return false;
        TileEntitySmartChute chute = (TileEntitySmartChute) te;
        ItemStack held = playerIn.getHeldItem(hand);
        if (worldIn.isRemote) return true;
        if (held.isEmpty()) {
            if (playerIn.isSneaking()) {
                chute.filter = null;
                chute.sync();
            }
            return true;
        }
        if (held.getItem() == ItemInit.WRENCH) {
            if (playerIn.isSneaking()) chute.cycleAmount();
            else chute.extractionExact = !chute.extractionExact;
            chute.sync();
            return true;
        }
        chute.filter = new ItemFilterExact(held);
        chute.sync();
        return true;
    }

    //region redstone gating: POWERED follows the neighbour signal, one tick late
    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!worldIn.isRemote) worldIn.scheduleUpdate(pos, this, 1);
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, java.util.Random rand) {
        if (worldIn.isRemote) return;
        boolean powered = worldIn.isBlockPowered(pos);
        if (state.getValue(POWERED) != powered) {
            worldIn.setBlockState(pos, state.withProperty(POWERED, powered), 3);
        }
    }
    //endregion

    @Override
    public List<String> getGoggleInfo(World world, BlockPos pos, IBlockState state) {
        List<String> list = BlockKineticBase.withTEDo(world, pos, TileEntitySmartChute.class, (te) -> {
            TextBuilder builder = new TextBuilder();
            if (te.filter != null) {
                builder.text("Filter: ").formatting(TextFormatting.AQUA)
                        .text(te.filter.getRenderItem().getDisplayName()).enter();
                builder.formatting(TextFormatting.GRAY)
                        .text((te.extractionExact ? "Exactly " : "Up to ") + te.extractionAmount).enter();
            }
            if (state.getValue(POWERED)) {
                builder.formatting(TextFormatting.RED).text("Powered (idle)").enter();
            }
            if (te.stack.isEmpty() && te.filter == null && !state.getValue(POWERED)) {
                return Collections.emptyList();
            }
            if (!te.stack.isEmpty()) {
                builder.space().space().formatting(TextFormatting.GRAY).text(te.stack.getCount() + "x ")
                        .formatting(TextFormatting.AQUA).text(te.stack.getDisplayName()).enter();
            }
            return builder.build();
        });
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(VARIANT).getId();
        if (state.getValue(POWERED)) meta |= 4;
        return meta;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(VARIANT, EnumChuteVariant.VALUES[(meta & 3) % 3])
                .withProperty(POWERED, (meta & 4) != 0);
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
