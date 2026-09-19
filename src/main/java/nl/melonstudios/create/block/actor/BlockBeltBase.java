package nl.melonstudios.create.block.actor;

import com.melonstudios.melonlib.misc.StackUtil;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.block.state.EnumBeltPart;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.actor.TileEntityBeltBase;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.Nullable;
import java.util.Random;

@SuppressWarnings("deprecation")
public abstract class BlockBeltBase extends BlockKineticBase implements ITileEntityProvider {
    public static final PropertyEnum<EnumBeltPart> PART = PropertyEnum.create("part", EnumBeltPart.class);
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

    public BlockBeltBase() {
        super(Material.ROCK, MapColor.BLACK);
        this.blockSoundType = SoundType.CLOTH;

        this.blockHardness = BlockProperties.WOOL_HARDNESS;
        this.blockResistance = BlockProperties.WOOL_RESISTANCE;

        this.setHarvestLevel(null, -1);
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        EnumBeltPart part = state.getValue(PART);
        return part != EnumBeltPart.MIDDLE && this.getRotationAxis(state) == side.getAxis();
    }

    @Nullable
    @Override
    public abstract TileEntityBeltBase createNewTileEntity(World worldIn, int meta);

    public abstract EnumFacing.Axis getTransportAxis(IBlockState state);
    public abstract boolean isFunctional(IBlockState state);

    @Override
    public abstract IBlockState getStateFromMeta(int meta);

    @Override
    public abstract int getMetaFromState(IBlockState state);

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return ItemInit.BELT_CONNECTOR;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(ItemInit.BELT_CONNECTOR);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(ItemInit.BELT_CONNECTOR);
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(ItemInit.BELT_CONNECTOR);
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return true;
    }

    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return true;
    }

    @Nullable
    @Override
    public String getHarvestTool(IBlockState state) {
        return null;
    }

    @Override
    public int getHarvestLevel(IBlockState state) {
        return -1;
    }

    @Override
    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        // Vertical belts cannot transport (reference BeltBlock.canTransportObjects
        // rejects VERTICAL/SIDEWAYS; caps are likewise gated in hasCapability).
        if (!this.isFunctional(state)) return;
        if (entityIn.onGround && entityIn.isEntityAlive() && !worldIn.isRemote) {
            if (MathHelper.floor(entityIn.posX) == pos.getX() &&
                    MathHelper.floor(entityIn.posY) == pos.getY() &&
                    MathHelper.floor(entityIn.posZ) == pos.getZ()) {
                TileEntity te = worldIn.getTileEntity(pos);
                if (te instanceof TileEntityBeltBase && entityIn instanceof EntityItem) {
                    TileEntityBeltBase belt = (TileEntityBeltBase) te;
                    EntityItem item = (EntityItem) entityIn;

                    if (belt.getSpeed() != 0.0F) {
                        if (belt.getFlag()) {
                            if (belt.left.isEmpty()) {
                                item.setDead();
                                belt.left = item.getItem().copy();
                                belt.leftPos = 0.5;
                                belt.sync();
                            }
                        } else {
                            if (belt.right.isEmpty()) {
                                item.setDead();
                                belt.right = item.getItem().copy();
                                belt.rightPos = 0.5;
                                belt.sync();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityBeltBase) {
            TileEntityBeltBase belt = (TileEntityBeltBase) te;
            ItemStack held = playerIn.getHeldItem(hand);
            if (!held.isEmpty() && !playerIn.isSneaking()) {
                // Belt-end extension / merging with the connector (see BeltSlicer).
                // Block activation runs before Item.onItemUse, so handling it
                // here keeps it from conflicting with the connector's
                // shaft-placement logic; sneaking still bypasses to the item.
                if (held.getItem() == ItemInit.BELT_CONNECTOR) {
                    return BeltSlicer.useConnector(worldIn, pos, state, playerIn, hand, hitX, hitY, hitZ);
                }
                // Shaft on a middle segment installs a pulley.
                if (held.getItem() == Item.getItemFromBlock(BlockInit.SHAFT)) {
                    return BeltSlicer.useShaft(worldIn, pos, state, playerIn, hand);
                }
            }
            if (facing == EnumFacing.UP && held.isEmpty()) {
                if (!worldIn.isRemote) {
                    if (!belt.left.isEmpty()) {
                        ItemStack stack = belt.left.copy();
                        belt.left = ItemStack.EMPTY;
                        belt.sync();
                        if (!playerIn.addItemStackToInventory(stack) && !stack.isEmpty()) {
                            StackUtil.spawnItemWithVelocity(worldIn, playerIn.posX, playerIn.posY + 0.5, playerIn.posZ,
                                    stack.copy(), 0.0, 0.2, 0.0);
                        }
                    }
                    if (!belt.right.isEmpty()) {
                        ItemStack stack = belt.right.copy();
                        belt.right = ItemStack.EMPTY;
                        belt.sync();
                        if (!playerIn.addItemStackToInventory(stack) && !stack.isEmpty()) {
                            StackUtil.spawnItemWithVelocity(worldIn, playerIn.posX, playerIn.posY + 0.5, playerIn.posZ,
                                    stack.copy(), 0.0, 0.2, 0.0);
                        }
                    }
                }
                return true;
            } else {
                if (!held.isEmpty() && hand == EnumHand.MAIN_HAND) {
                    if (held.getItem() == Items.WATER_BUCKET) {
                        if (!worldIn.isRemote) belt.applyColor(null);
                        return true;
                    } else {
                        for (int i = 0; i < 16; i++) {
                            EnumDyeColor color = EnumDyeColor.byMetadata(i);
                            String ore = dyes[15 - i];
                            if (OreDictionary.getOres(ore).stream().anyMatch(prd -> prd.isItemEqual(held))) {
                                if (!worldIn.isRemote) belt.applyColor(color);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean onWrenched(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ) {
        return BeltSlicer.useWrench(world, pos, state, side);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }
}
