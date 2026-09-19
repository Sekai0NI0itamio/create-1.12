package nl.melonstudios.create.block;

import com.melonstudios.melonlib.item.IMetaName;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.tileentity.TileEntityGearbox;
import nl.melonstudios.create.util.interfaces.IRotate;

import javax.annotation.Nullable;

public class BlockGearbox extends BlockKineticRotatedPillarBase implements ITileEntityProvider, IMetaName {
    public BlockGearbox(MapColor map, SoundType sound) {
        super(Material.ROCK, map);
        this.blockSoundType = sound;

        this.setRegistryName("gearbox");
        this.setUnlocalizedName("create.gearbox");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityGearbox();
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side.getAxis() != state.getValue(AXIS);
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        if (meta == 0) return this.getDefaultState().withProperty(AXIS, EnumFacing.Axis.Y);
        else {
            EnumFacing.Axis preferredAxis = null;
            for (EnumFacing side : EnumFacing.HORIZONTALS) {
                IBlockState state = world.getBlockState(pos.offset(side));
                IRotate rotate = IRotate.is(state);
                if (rotate != null) {
                    if (rotate.hasShaftTowards(world, pos.offset(side), state, side.getOpposite())) {
                        if (preferredAxis != null && preferredAxis != side.getAxis()) {
                            preferredAxis = null;
                            break;
                        } else {
                            preferredAxis = side.getAxis();
                        }
                    }
                }
            }

            EnumFacing.Axis axis = preferredAxis == null ? placer.getHorizontalFacing().rotateY().getAxis() :
                    preferredAxis == EnumFacing.Axis.X ? EnumFacing.Axis.Z : EnumFacing.Axis.X;
            return this.getDefaultState().withProperty(AXIS, axis);
        }
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return stack.getMetadata() == 0 ? "tile.create.gearbox" : "tile.create.gearbox_vertical";
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, 0));
        items.add(new ItemStack(this, 1, 1));
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(AXIS) == EnumFacing.Axis.Y ? 0 : 1;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(this, 1, this.damageDropped(state));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public float getAmbientOcclusionLightValue(IBlockState state) {
        return 0.2F;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || "axe".equals(type);
    }
}
