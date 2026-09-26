package nl.melonstudios.create.block.train;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;

/**
 * Cart assembler: rail that captures minecarts into contraption trains.
 * Backport behavior: unpowered it brakes carts to a stop (assembly hold);
 * powered it releases them with a push along the rail. BACKWARDS flips the
 * release direction (wrench toggles). Right-click reports state. Only
 * straight shapes (reference rail_type variants collapse to one model).
 */
@SuppressWarnings("deprecation")
public class BlockCartAssembler extends BlockRailBase implements IWrenchable {
    public static final PropertyEnum<EnumRailDirection> SHAPE =
            PropertyEnum.create("shape", EnumRailDirection.class, EnumRailDirection.NORTH_SOUTH,
                    EnumRailDirection.EAST_WEST);
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyBool BACKWARDS = PropertyBool.create("backwards");

    public BlockCartAssembler() {
        super(false);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(0.8F);
        this.setResistance(0.8F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("cart_assembler");
        this.setUnlocalizedName("create.cart_assembler");
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(SHAPE, EnumRailDirection.NORTH_SOUTH)
                .withProperty(POWERED, false)
                .withProperty(BACKWARDS, false));
    }

    @Override
    public PropertyEnum<EnumRailDirection> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, SHAPE, POWERED, BACKWARDS);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(SHAPE, (meta & 1) == 0 ? EnumRailDirection.NORTH_SOUTH : EnumRailDirection.EAST_WEST)
                .withProperty(POWERED, (meta & 2) != 0)
                .withProperty(BACKWARDS, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(SHAPE) == EnumRailDirection.EAST_WEST ? 1 : 0)
                | (state.getValue(POWERED) ? 2 : 0)
                | (state.getValue(BACKWARDS) ? 4 : 0);
    }

    @Override
    public void onMinecartPass(World world, EntityMinecart cart, BlockPos pos) {
        if (world.isRemote) return;
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockCartAssembler)) return;
        boolean ew = state.getValue(SHAPE) == EnumRailDirection.EAST_WEST;
        boolean backwards = state.getValue(BACKWARDS);
        if (world.isBlockPowered(pos)) {
            double push = 0.15;
            if (ew) cart.motionX += backwards ? -push : push;
            else cart.motionZ += backwards ? -push : push;
            cart.velocityChanged = true;
        } else {
            cart.motionX *= 0.5;
            cart.motionZ *= 0.5;
            if (cart.motionX * cart.motionX + cart.motionZ * cart.motionZ < 0.0004) {
                cart.motionX = 0;
                cart.motionZ = 0;
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block blockIn,
                                BlockPos fromPos) {
        if (world.isRemote) return;
        boolean powered = world.isBlockPowered(pos);
        if (state.getValue(POWERED) != powered) {
            world.setBlockState(pos, state.withProperty(POWERED, powered), 3);
        }
    }

    @Override
    public IBlockState getActualState(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos) {
        if (world instanceof World) {
            boolean powered = ((World) world).isBlockPowered(pos);
            if (state.getValue(POWERED) != powered) return state.withProperty(POWERED, powered);
        }
        return state;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                   EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        boolean held = world.isBlockPowered(pos);
        player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                held ? "Cart assembler: releasing" : "Cart assembler: holding carts"), true);
        return true;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY,
                             float hitZ) {
        world.setBlockState(pos, state.withProperty(BACKWARDS, !state.getValue(BACKWARDS)), 3);
        return true;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
