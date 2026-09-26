package nl.melonstudios.create.block;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.item.ItemClipboard;
import nl.melonstudios.create.tileentity.TileEntityClipboard;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Wall-mounted clipboard. Reference: ClipboardBlock (WRITTEN property follows
 * whether the block entity holds entry NBT; sneak-interact syncs the held
 * clipboard item both ways).
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockClipboard extends Block implements ITileEntityProvider {
    public static final PropertyBool WRITTEN = PropertyBool.create("written");

    public BlockClipboard() {
        super(Material.WOOD);
        this.blockSoundType = SoundType.WOOD;
        this.setHardness(BlockProperties.WOOD_HARDNESS);
        this.setResistance(BlockProperties.WOOD_RESISTANCE);
        this.setDefaultState(this.blockState.getBaseState().withProperty(WRITTEN, false));
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityClipboard();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        boolean written = te instanceof TileEntityClipboard && ((TileEntityClipboard) te).isWritten();
        return state.withProperty(WRITTEN, written);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, WRITTEN);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityClipboard)) return false;
        TileEntityClipboard clip = (TileEntityClipboard) te;
        ItemStack held = playerIn.getHeldItem(hand);
        if (worldIn.isRemote) return true;
        if (playerIn.isSneaking() && held.getItem() instanceof ItemClipboard) {
            if (ItemClipboard.isWritten(held)) {
                clip.setData(held);
                playerIn.sendStatusMessage(new TextComponentString("Clipboard pinned to block."), true);
            } else if (clip.isWritten()) {
                playerIn.setHeldItem(hand, clip.getData().copy());
                playerIn.sendStatusMessage(new TextComponentString("Clipboard copied from block."), true);
            } else {
                playerIn.sendStatusMessage(new TextComponentString("Both clipboards are empty."), true);
            }
            return true;
        }
        if (clip.isWritten()) {
            playerIn.sendStatusMessage(new TextComponentString(
                    ItemClipboard.entriesOf(clip.getData()).tagCount() + " entries. Sneak-use with a clipboard to copy."), true);
        } else {
            playerIn.sendStatusMessage(new TextComponentString("Empty clipboard. Sneak-use with a written clipboard to pin it."), true);
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityClipboard && ItemClipboard.isWritten(stack)) {
            ((TileEntityClipboard) te).setData(stack);
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityClipboard && ((TileEntityClipboard) te).isWritten()
                && !worldIn.isRemote && worldIn.getGameRules().getBoolean("doTileDrops")) {
            spawnAsEntity(worldIn, pos, ((TileEntityClipboard) te).getData().copy());
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityClipboard();
    }
}
