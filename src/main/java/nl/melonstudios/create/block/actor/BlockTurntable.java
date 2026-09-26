package nl.melonstudios.create.block.actor;

import nl.melonstudios.create.init.CreateTabs;
import com.melonstudios.melonlib.misc.Localizer;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.actor.TileEntityTurntable;

import javax.annotation.Nullable;
import java.util.List;

public class BlockTurntable extends BlockKineticBase implements ITileEntityProvider {
    public BlockTurntable(MapColor blockMapColorIn, SoundType soundType) {
        super(Material.ROCK, blockMapColorIn);
        this.blockSoundType = soundType;

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    protected static final AxisAlignedBB AABB_BOTTOM_HALF = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB_BOTTOM_HALF;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityTurntable();
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return side == EnumFacing.DOWN;
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return EnumFacing.Axis.Y;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn) {
        if (!entityIn.onGround) return;
        if (entityIn.motionY > 0.0) return;
        if (entityIn.posY < pos.getY() + 0.5F) return;

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityTurntable) {
            TileEntityTurntable turntable = (TileEntityTurntable) te;

            float speed = turntable.getSpeed() * 3 / 10;
            if (speed == 0) return;

            // Reference TurntableBlock turns every entity's yaw, and additionally
            // turns head/body yaw of non-player living entities standing on it.
            entityIn.rotationYaw -= speed;
            if (entityIn instanceof EntityLivingBase && !(entityIn instanceof EntityPlayer)) {
                EntityLivingBase living = (EntityLivingBase) entityIn;
                living.rotationYawHead -= speed;
                living.renderYawOffset -= speed;
            }
            entityIn.velocityChanged = true;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, player, tooltip, advanced);

        tooltip.add(Localizer.translate("tile.create.turntable.desc"));
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type) || "axe".equals(type);
    }
}
