package nl.melonstudios.create.block.actor;

import nl.melonstudios.create.init.CreateTabs;
import com.melonstudios.melonlib.misc.BlockStateProperties;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.BlockProperties;

import java.util.Random;

@SuppressWarnings("deprecation")
public class BlockMechanicalPistonHead extends Block {
    public static final PropertyDirection FACING = BlockStateProperties.FACING;
    public static final PropertyBool STICKY = PropertyBool.create("sticky");

    public BlockMechanicalPistonHead() {
        super(Material.ROCK, MapColor.WOOD);
        this.blockSoundType = SoundType.WOOD;

        this.blockHardness = BlockProperties.WOOD_HARDNESS;
        this.blockResistance = BlockProperties.WOOD_RESISTANCE;

        this.setHarvestLevel("axe", -1);

        this.setDefaultState(this.getDefaultState()
                .withProperty(FACING, EnumFacing.UP)
                .withProperty(STICKY, false)
        );

        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, STICKY);
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


    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() | (state.getValue(STICKY) ? 0b1000 : 0b0000);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.VALUES[meta & 0b0111]).withProperty(STICKY, (meta & 0b1000) != 0);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(BlockInit.PISTON_POLE);
    }
}
