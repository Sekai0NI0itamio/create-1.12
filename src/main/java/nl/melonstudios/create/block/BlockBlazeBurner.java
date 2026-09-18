package nl.melonstudios.create.block;

import com.melonstudios.melonlib.item.IMetaName;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.TileEntityBlazeBurner;
import nl.melonstudios.create.util.BlockProperties;
import nl.melonstudios.create.util.Utils;
import nl.melonstudios.create.util.interfaces.IHeatProvider;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Random;

public class BlockBlazeBurner extends Block implements IHeatProvider, IMetaName, ITileEntityProvider {
    public enum Variant implements IStringSerializable {
        EMPTY(0),
        LIT(0),
        PASSIVE(0),
        HEATED(1),
        SUPERHEATED(2);

        private final String name = this.toString().toLowerCase(Locale.ENGLISH);
        private final int id = this.ordinal();
        private final int heating;

        Variant(int heating) {
            this.heating = heating;
        }

        @Override
        public String getName() {
            return this.name;
        }
        public int getID() {
            return this.id;
        }
        public int getHeating() {
            return this.heating;
        }

        public static final Variant[] VALUES = values();
        public static Variant byID(int id) {
            return id < 0 || id > 4 ? EMPTY : VALUES[id];
        }
    }
    public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

    public BlockBlazeBurner() {
        super(Material.IRON, MapColor.NETHERRACK);

        this.setHardness(3.0F);
        this.setResistance(BlockProperties.IRON_RESISTANCE);

        this.setHarvestLevel("pickaxe", 1);

        this.setCreativeTab(ItemInit.TAB_CREATE);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "tile.create.blaze_burner" + (stack.getMetadata() != 0 ? "" : "_empty");
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(VARIANT).getID() < 2 ? 0 : 1;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getID();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, Variant.byID(meta));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(VARIANT, meta != 0 ? Variant.PASSIVE : Variant.EMPTY);
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, 0));
        items.add(new ItemStack(this, 1, 1));
    }

    @Override
    public int getHeat(World world, BlockPos pos, IBlockState state) {
        return state.getValue(VARIANT).getHeating();
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return meta < 2 ? null : new TileEntityBlazeBurner();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return state.getValue(VARIANT).getID() > 1;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        Variant variant = state.getValue(VARIANT);
        if (variant == Variant.EMPTY) {
            ItemStack held = playerIn.getHeldItem(hand);
            if (held.getItem() == Items.FLINT_AND_STEEL) {
                held.damageItem(1, playerIn);
                worldIn.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
                worldIn.setBlockState(pos, state.withProperty(VARIANT, Variant.LIT));
                return true;
            }
            if (held.getItem() == Items.FIRE_CHARGE) {
                held.shrink(1);
                worldIn.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
                worldIn.setBlockState(pos, state.withProperty(VARIANT, Variant.LIT));
                return true;
            }
            return false;
        }
        if (variant.getID() < 2 || hand != EnumHand.MAIN_HAND) return false;
        TileEntityBlazeBurner te = Utils.cast(worldIn.getTileEntity(pos), TileEntityBlazeBurner.class);
        if (te == null) return false;
        ItemStack held = playerIn.getHeldItem(hand);
        if (TileEntityBlazeBurner.isCreativeFuel(held)) {
            te.applyCreativeFuel();
            if (!worldIn.isRemote) {
                worldIn.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 0.7F, 1.0F);
                worldIn.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.BLOCKS, 1.0F, 1.0F);
            } else {
                Random rnd = new Random();
                for (int i = 0; i < 16; i++) {
                    double x = pos.getX() + rnd.nextDouble();
                    double y = pos.getY() + rnd.nextDouble();
                    double z = pos.getZ() + rnd.nextDouble();
                    worldIn.spawnParticle(EnumParticleTypes.FLAME, x, y, z, 0, 0.05, 0);
                }
            }
            return true;
        }
        if (variant == Variant.SUPERHEATED) return false;
        boolean isBlazecake = OreDictionary.containsMatch(true, OreDictionary.getOres("create:blazecake"), held);
        if (isBlazecake) {
            te.blazecake(5000);
            if (!worldIn.isRemote) {
                ItemStack container = held.getItem().getContainerItem(held);
                held.shrink(1);
                if (!container.isEmpty()) playerIn.addItemStackToInventory(container);
                worldIn.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 0.7F, 1.0F);
                worldIn.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                worldIn.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.BLOCKS, 0.7F, 1.0F);
            } else {
                Random rnd = new Random();
                for (int i = 0; i < 16; i++) {
                    double x = pos.getX() + rnd.nextDouble();
                    double y = pos.getY() + rnd.nextDouble();
                    double z = pos.getZ() + rnd.nextDouble();
                    worldIn.spawnParticle(EnumParticleTypes.FLAME, x, y, z, 0, 0.05, 0);
                }
            }
            return true;
        } else {
            int fuelTicks = ForgeEventFactory.getItemBurnTime(held);
            if (fuelTicks == -1) fuelTicks = this.getFurnaceBurnTimeLogicWhyItSucksSoMuchPleaseWhyMojang(held);
            if (fuelTicks > 0) {
                te.feed(fuelTicks);
                if (!worldIn.isRemote) {
                    ItemStack container = held.getItem().getContainerItem(held);
                    held.shrink(1);
                    if (!container.isEmpty()) playerIn.addItemStackToInventory(container);
                    worldIn.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 0.7F, 1.0F);
                    worldIn.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                } else {
                    Random rnd = new Random();
                    for (int i = 0; i < 8; i++) {
                        double x = pos.getX() + rnd.nextDouble();
                        double y = pos.getY() + rnd.nextDouble();
                        double z = pos.getZ() + rnd.nextDouble();
                        worldIn.spawnParticle(EnumParticleTypes.FLAME, x, y, z, 0, 0.05, 0);
                    }
                }
                return true;
            }
        }
        return false;
    }

    //this does not spark joy
    private int getFurnaceBurnTimeLogicWhyItSucksSoMuchPleaseWhyMojang(ItemStack stack) {
        Item item = stack.getItem();

        if (item == Item.getItemFromBlock(Blocks.WOODEN_SLAB)) {
            return 150;
        } else if (item == Item.getItemFromBlock(Blocks.WOOL)) {
            return 100;
        } else if (item == Item.getItemFromBlock(Blocks.CARPET)) {
            return 67;
        } else if (item == Item.getItemFromBlock(Blocks.LADDER)) {
            return 300;
        } else if (item == Item.getItemFromBlock(Blocks.WOODEN_BUTTON)) {
            return 100;
        } else if (Block.getBlockFromItem(item).getDefaultState().getMaterial() == Material.WOOD) {
            return 300;
        } else if (item == Item.getItemFromBlock(Blocks.COAL_BLOCK)) {
            return 16000;
        } else if (item instanceof ItemTool && "WOOD".equals(((ItemTool)item).getToolMaterialName())) {
            return 200;
        } else if (item instanceof ItemSword && "WOOD".equals(((ItemSword)item).getToolMaterialName())) {
            return 200;
        } else if (item instanceof ItemHoe && "WOOD".equals(((ItemHoe)item).getMaterialName())) {
            return 200;
        } else if (item == Items.STICK) {
            return 100;
        } else if (item != Items.BOW && item != Items.FISHING_ROD) {
            if (item == Items.SIGN) {
                return 200;
            } else if (item == Items.COAL) {
                return 1600;
            } else if (item == Items.LAVA_BUCKET) {
                return 20000;
            } else if (item != Item.getItemFromBlock(Blocks.SAPLING) && item != Items.BOWL) {
                if (item == Items.BLAZE_ROD) {
                    return 2400;
                } else if (item instanceof ItemDoor && item != Items.IRON_DOOR) {
                    return 200;
                } else {
                    return item instanceof ItemBoat ? 400 : 0;
                }
            } else {
                return 100;
            }
        } else {
            return 300;
        }
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
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        Variant variant = state.getValue(VARIANT);
        switch (variant) {
            case PASSIVE:
                return 7;
            case LIT:
            case HEATED:
            case SUPERHEATED:
                return 15;
            default:
                return 0;
        }
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        Variant variant = stateIn.getValue(VARIANT);
        if (variant == Variant.EMPTY) return;
        worldIn.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                pos.getX() + 0.25 + rand.nextDouble() * 0.5, pos.getY() + 0.5, pos.getZ() + 0.25 + rand.nextDouble() * 0.5,
                0.0, 0.0, 0.0);
        if (rand.nextFloat() < 0.2F) {
            worldIn.playSound(Minecraft.getMinecraft().player,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 0.5F, 0.8F + rand.nextFloat() * 0.3F
            );
        }
    }
}
