package nl.melonstudios.create.block.deco;

import com.melonstudios.melonlib.item.IMetaName;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.melonstudios.create.block.state.CreateStateProperties;
import nl.melonstudios.create.block.state.EnumOrestoneVariant;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.util.BlockProperties;

import java.util.Random;

public class BlockOrestonePillar extends Block implements IMetaName {
    public static final PropertyEnum<EnumOrestoneVariant> VARIANT = CreateStateProperties.ORESTONE_VARIANT;
    public BlockOrestonePillar(EnumFacing.Axis axis) {
        super(Material.ROCK);

        if (axis != EnumFacing.Axis.Y) {
            this.setRegistryName("orestone_pillar_" + axis.getName());
        } else this.setRegistryName("orestone_pillar");

        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(VARIANT, EnumOrestoneVariant.ASURINE)
        );
        this.setHarvestLevel("pickaxe", 0);
        this.setHardness(BlockProperties.STONE_HARDNESS);
        this.setResistance(BlockProperties.STONE_RESISTANCE);

        this.setCreativeTab(CreateTabs.TAB_CREATE_DECORATIONS);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

    @Override
    public String getUnlocalizedName(ItemStack itemStack) {
        return "tile.create.pillar_" + EnumOrestoneVariant.byId(itemStack.getMetadata()).getName();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (int i = 0; i < 7; i++) {
            items.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getId();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, EnumOrestoneVariant.byId(meta));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(BlockInit.ORESTONE_PILLAR_Y);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getId();
    }

    // Same reference hardness split as the other orestone blocks.
    @Override
    public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
        switch (state.getValue(VARIANT)) {
            case SCORIA:
            case SCORCHIA:
                return 1.5F;
            default:
                return 1.25F;
        }
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        switch (state.getValue(VARIANT)) {
            case ASURINE:
                return MapColor.BLUE;
            case CRIMSITE:
                return MapColor.NETHERRACK;
            case LIMESTONE:
                return MapColor.SAND;
            case OCHRUM:
                return MapColor.ORANGE_STAINED_HARDENED_CLAY;
            case SCORCHIA:
                return MapColor.GRAY;
            case SCORIA:
                return MapColor.BROWN;
            case VERIDIUM:
                return MapColor.GREEN;
            default:
                return MapColor.STONE;
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        if (!placer.isSneaking()) {
            IBlockState placedOn = world.getBlockState(pos.offset(facing.getOpposite()));
            if (placedOn.getBlock() instanceof BlockOrestonePillar) {
                return placedOn.withProperty(VARIANT, EnumOrestoneVariant.byId(meta));
            }
        }
        EnumFacing.Axis axis = facing.getAxis();
        BlockOrestonePillar pillar;
        switch (axis) {
            case X: pillar = BlockInit.ORESTONE_PILLAR_X; break;
            case Y: pillar = BlockInit.ORESTONE_PILLAR_Y; break;
            case Z: pillar = BlockInit.ORESTONE_PILLAR_Z; break;
            default:throw new IllegalStateException("erm what the heck");
        }
        return pillar.getDefaultState().withProperty(VARIANT, EnumOrestoneVariant.byId(meta));
    }
}
