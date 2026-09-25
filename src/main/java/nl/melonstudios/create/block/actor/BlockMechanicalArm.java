package nl.melonstudios.create.block.actor;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.block.BlockKineticDirectionalBase;
import nl.melonstudios.create.tileentity.actor.TileEntityMechanicalArm;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public class BlockMechanicalArm extends BlockKineticDirectionalBase implements ITileEntityProvider {
    public BlockMechanicalArm() {
        super(Material.ROCK, MapColor.STONE);
        this.setSoundType(SoundType.STONE);
        this.setRegistryName("mechanical_arm");
        this.setUnlocalizedName("create.mechanical_arm");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityMechanicalArm();
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side.getAxis() == EnumFacing.Axis.Y;
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return EnumFacing.Axis.Y;
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        BlockKineticBase.withTEDo(worldIn, pos, TileEntityMechanicalArm.class,
                TileEntityMechanicalArm::redstoneUpdate);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, net.minecraft.block.Block blockIn, BlockPos fromPos) {
        BlockKineticBase.withTEDo(worldIn, pos, TileEntityMechanicalArm.class,
                TileEntityMechanicalArm::redstoneUpdate);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return false;
        ItemStack inHand = playerIn.getHeldItemMainhand();
        Boolean result = BlockKineticBase.withTEDo(worldIn, pos, TileEntityMechanicalArm.class, (te) -> {
            if (!te.heldItem.isEmpty() && inHand.isEmpty() && !playerIn.isSneaking()) {
                if (!worldIn.isRemote) {
                    playerIn.addItemStackToInventory(te.heldItem.copy());
                    te.heldItem = ItemStack.EMPTY;
                    te.resetToSearch();
                }
                return true;
            }
            if (playerIn.isSneaking()) {
                if (!worldIn.isRemote) {
                    if (!inHand.isEmpty()) {
                        te.setFilter(inHand.copy());
                    } else {
                        te.cycleMode();
                    }
                }
                return true;
            }
            return false;
        });
        return Boolean.TRUE.equals(result);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BlockProperties.CASING_12PX_MAPPED[state.getValue(FACING).getIndex()];
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || "axe".equals(type);
    }
}
