package nl.melonstudios.create.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.BlockStressValues;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.tileentity.TileEntityKinetic;
import nl.melonstudios.create.tileentity.TileEntityOptimizedBase;
import nl.melonstudios.create.tileentity.marker.ISpeedRequirement;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.TextBuilder;
import nl.melonstudios.create.util.interfaces.IGoggleInfo;
import nl.melonstudios.create.util.interfaces.IRotate;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("deprecation")
public abstract class BlockKineticBase extends Block implements IRotate, IGoggleInfo, IWrenchable {
    public BlockKineticBase(Material blockMaterialIn, MapColor blockMapColorIn) {
        super(blockMaterialIn, blockMapColorIn);

        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);

        this.setHarvestLevel("pickaxe", 0);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOptimizedBase) ((TileEntityOptimizedBase)te).destroy();
        if (this.hasTileEntity(state) || te != null) worldIn.removeTileEntity(pos);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
    }

    public static boolean isPosPowered(World world, BlockPos pos) {
        return world.isBlockPowered(pos) || world.isBlockIndirectlyGettingPowered(pos) > 0;
    }
    public static int getPosPower(World world, BlockPos pos) {
        int power = world.getRedstonePower(pos.down(), EnumFacing.DOWN);
        if (power >= 15) return power;
        power = Math.max(power, world.getRedstonePower(pos.up(), EnumFacing.UP));
        if (power >= 15) return power;
        power = Math.max(power, world.getRedstonePower(pos.north(), EnumFacing.NORTH));
        if (power >= 15) return power;
        power = Math.max(power, world.getRedstonePower(pos.south(), EnumFacing.SOUTH));
        if (power >= 15) return power;
        power = Math.max(power, world.getRedstonePower(pos.east(), EnumFacing.EAST));
        if (power >= 15) return power;
        power = Math.max(power, world.getRedstonePower(pos.west(), EnumFacing.WEST));
        if (power >= 15) return power;
        return Math.max(power, world.isBlockIndirectlyGettingPowered(pos));
    }

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

    @Nullable
    public static TileEntityKinetic getKineticTE(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileEntityKinetic ? (TileEntityKinetic) te : null;
    }

    @Override
    public List<String> getGoggleInfo(World world, BlockPos pos, IBlockState state) {
        TileEntityKinetic te = getKineticTE(world, pos);
        if (te != null) {
            TextBuilder builder = new TextBuilder();
            float stressCapacity = te.calculateCapacity() * Math.abs(te.getGeneratedSpeed());
            float stressImpact = te.calculateImpact() * Math.abs(te.getTheoreticalSpeed());
            boolean flag;
            if (stressCapacity != 0 || stressImpact != 0) {
                flag = true;
                builder.translate("goggles.kinetic_stats").enter();
                if (stressCapacity != 0) {
                    builder.formatting(TextFormatting.GRAY)
                            .translate("goggles.kinetic_capacity")
                            .text(": ").formatting(TextFormatting.AQUA).number(stressCapacity).text("su").enter();
                }
                if (stressImpact != 0) {
                    builder.formatting(TextFormatting.GRAY)
                            .translate("goggles.kinetic_impact")
                            .text(": ").formatting(TextFormatting.AQUA).number(stressImpact).text("su").enter();
                }
            } else flag = false;
            if (te instanceof ISpeedRequirement) {
                float min = ((ISpeedRequirement)te).minimumSpeed();
                if (te.getTheoreticalSpeed() < min && te.getSpeed() != 0.0F) {
                    if (flag) builder.enter();
                    builder.formatting(TextFormatting.GOLD).translate("goggles.speed_requirement").enter();
                    builder.formatting(TextFormatting.AQUA).space().space().translate("goggles.speed_requirement.desc", min).enter();
                }
            }
            return builder.build();
        }
        return Collections.emptyList();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, player, tooltip, advanced);

        float capacity = BlockStressValues.getStressCapacity(this);
        float impact = BlockStressValues.getStressImpact(this);

        TextBuilder builder = new TextBuilder();
        if (capacity != 0.0F) {
            builder.space().text("Kinetic Stress Capacity: ").formatting(TextFormatting.AQUA).number(capacity).text("x RPM").enter();
        }
        if (impact != 0.0F) {
            builder.space().text("Kinetic Stress Impact: ").formatting(TextFormatting.AQUA).number(impact).text("x RPM").enter();
        }
        if (!builder.isEmpty()) tooltip.addAll(builder.build());
    }

    @SuppressWarnings("unchecked")
    public static <T extends TileEntity> void withTEDo(World world, BlockPos pos, Class<T> clazz, Consumer<T> action) {
        TileEntity te = world.getTileEntity(pos);
        if (clazz.isInstance(te)) action.accept((T) te);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends TileEntity, R> R withTEDo(World world, BlockPos pos, Class<T> clazz, Function<T, R> action) {
        TileEntity te = world.getTileEntity(pos);
        if (clazz.isInstance(te)) return action.apply((T) te);
        return null;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        return false;
    }
}
