package nl.melonstudios.create.block.logistics;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.melonstudios.create.block.BlockKineticBase;
import nl.melonstudios.create.tileentity.logistics.TileEntityPackager;

import javax.annotation.Nullable;

/**
 * Stock ticker: right-click to see a chat summary of stock in the inventory
 * below, sneak-right-click to order everything into the packager above/below.
 * Simplified backport of the stock-ticker request flow (no GUI).
 */
@SuppressWarnings("deprecation")
public class BlockStockTicker extends BlockKineticBase implements ITileEntityProvider {
    public BlockStockTicker() {
        super(Material.ROCK, MapColor.STONE);
        this.blockSoundType = SoundType.STONE;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return null;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return false;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        IItemHandler stock = this.stockBelow(world, pos);
        TileEntityPackager packager = this.packagerNear(world, pos);
        if (player.isSneaking()) {
            if (packager != null && stock != null) {
                int n = 0;
                for (int i = 0; i < stock.getSlots() && n < 9; i++) {
                    net.minecraft.item.ItemStack s = stock.getStackInSlot(i);
                    if (s.isEmpty()) continue;
                    net.minecraft.item.ItemStack req = s.copy();
                    req.setCount(Math.min(64, s.getCount()));
                    packager.requests.set(n++, req);
                }
                packager.packageAll();
                packager.sync();
                player.sendStatusMessage(new TextComponentString("Ordered " + n + " request(s) to the packager."), true);
            } else {
                player.sendStatusMessage(new TextComponentString("Need a packager + stocked inventory adjacent."), true);
            }
            return true;
        }
        if (stock == null) {
            player.sendStatusMessage(new TextComponentString("No stocked inventory below."), true);
            return true;
        }
        StringBuilder sb = new StringBuilder("Stock: ");
        int shown = 0;
        for (int i = 0; i < stock.getSlots() && shown < 6; i++) {
            net.minecraft.item.ItemStack s = stock.getStackInSlot(i);
            if (s.isEmpty()) continue;
            if (shown > 0) sb.append(", ");
            sb.append(s.getCount()).append("x ").append(s.getDisplayName());
            shown++;
        }
        if (shown == 0) sb.append("empty");
        sb.append(" (sneak-click to order)");
        player.sendStatusMessage(new TextComponentString(sb.toString()), false);
        return true;
    }

    private IItemHandler stockBelow(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos.down());
        if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP)) {
            return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        }
        return null;
    }

    private TileEntityPackager packagerNear(World world, BlockPos pos) {
        for (EnumFacing f : EnumFacing.VALUES) {
            TileEntity te = world.getTileEntity(pos.offset(f));
            if (te instanceof TileEntityPackager) return (TileEntityPackager) te;
        }
        return null;
    }

    @Override
    public EnumFacing.Axis getRotationAxis(IBlockState state) {
        return EnumFacing.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(World world, BlockPos pos, IBlockState state, EnumFacing side) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public boolean isToolEffective(String type, IBlockState state) {
        return "pickaxe".equals(type);
    }
}
