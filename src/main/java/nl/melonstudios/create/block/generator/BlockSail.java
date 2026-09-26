package nl.melonstudios.create.block.generator;

import nl.melonstudios.create.init.CreateTabs;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockColored;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import nl.melonstudios.create.extensions.IExtensionBlock;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.kinetics.contraption.IWrenchable;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.interfaces.ISail;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Random;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class BlockSail extends BlockColored implements IExtensionBlock, ISail, IWrenchable {
    public static final PropertyEnum<EnumDyeColor> COLOR = BlockColored.COLOR;
    private static final String[] dyes = {
            "dyeBlack",
            "dyeRed",
            "dyeGreen",
            "dyeBrown",
            "dyeBlue",
            "dyePurple",
            "dyeCyan",
            "dyeLightGray",
            "dyeGray",
            "dyePink",
            "dyeLime",
            "dyeYellow",
            "dyeLightBlue",
            "dyeMagenta",
            "dyeOrange",
            "dyeWhite"
    };
    private final EnumFacing facing;

    public static BlockSail byFacing(EnumFacing facing) {
        switch (facing) {
            case DOWN: return BlockInit.SAIL_DOWN;
            case UP: return BlockInit.SAIL_UP;
            case NORTH: return BlockInit.SAIL_NORTH;
            case SOUTH: return BlockInit.SAIL_SOUTH;
            case WEST: return BlockInit.SAIL_WEST;
            case EAST: return BlockInit.SAIL_EAST;
            default:throw new IllegalArgumentException("Null facing not allowed");
        }
    }

    public BlockSail(EnumFacing facing) {
        super(Material.WOOD);
        this.facing = facing;
        this.blockSoundType = SoundType.WOOD;

        this.setRegistryName("sail_" + facing.getName());
        this.setUnlocalizedName("create.sail");

        this.setHarvestLevel("axe", -1);
        this.setHardness(BlockProperties.WOOD_HARDNESS);
        this.setResistance(BlockProperties.WOOD_RESISTANCE);

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BlockProperties.SAIL_MAPPED[this.facing.getIndex()];
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean onBlockActivated(
            World worldIn, BlockPos pos, IBlockState state,
            EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ
    ) {
        if (playerIn.isSneaking()) return false;
        ItemStack stack = playerIn.getHeldItem(hand);
        for (int i = 0; i < 16; i++) {
            EnumDyeColor enumDyeColor = EnumDyeColor.byMetadata(i);
            String ore = dyes[15-i];
            if (OreDictionary.getOres(ore).stream().anyMatch((pred) -> pred.isItemEqual(stack))) {
                worldIn.setBlockState(pos, state.withProperty(COLOR, enumDyeColor), 3);
                return true;
            }
        }
        return false;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return BlockInit.SAIL_ITEM;
    }
    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }
    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(BlockInit.SAIL_ITEM);
    }

    @Override
    public IBlockState getStateForPlacement(
            World world, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ,
            int meta, EntityLivingBase placer, EnumHand hand
    ) {
        if (placer.isSneaking()) {
            return byFacing(facing).getDefaultState();
        }
        IBlockState placedOn = world.getBlockState(pos.offset(facing.getOpposite()));
        if (placedOn.getBlock() instanceof BlockSail) {
            return placedOn.withProperty(COLOR, EnumDyeColor.WHITE);
        }
        return byFacing(EnumFacing.getDirectionFromEntityLiving(pos, placer)).getDefaultState();
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

    @Override
    public boolean create$isSideSticky(IBlockState state, EnumFacing side) {
        return this.isStickyBlock(state);
    }

    @Override
    public void create$addStickyLocations(World world, BlockPos pos, IBlockState state, List<BlockPos> positions) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing.getAxis().apply(this.facing)) continue;
            BlockPos off = pos.offset(facing);
            IBlockState hi = world.getBlockState(off);
            if (hi.getBlock() instanceof ISail) {
                if (((ISail)hi.getBlock()).getFacing(hi) == this.facing) {
                    positions.add(off);
                }
            }
        }
    }

    @Override
    public EnumFacing getFacing(IBlockState state) {
        return this.facing;
    }

    @Override
    public IBlockState withFacing(IBlockState state, EnumFacing facing) {
        return byFacing(facing).getDefaultState().withProperty(COLOR, state.getValue(COLOR));
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return byFacing(rot.rotate(this.facing)).getDefaultState().withProperty(COLOR, state.getValue(COLOR));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return byFacing(mirrorIn.mirror(this.facing)).getDefaultState().withProperty(COLOR, state.getValue(COLOR));
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        EnumFacing myFacing = this.getFacing(state);
        if (side.getAxis() == myFacing.getAxis()) return false;
        world.setBlockState(pos, byFacing(myFacing.rotateAround(side.getAxis())).getDefaultState().withProperty(COLOR, state.getValue(COLOR)));
        return true;
    }
}
