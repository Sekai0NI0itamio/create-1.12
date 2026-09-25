package nl.melonstudios.create.tileentity.actor;

import com.melonstudios.melonlib.blockdict.BlockDictionary;
import com.melonstudios.melonlib.misc.StackUtil;
import net.minecraft.block.BlockBush;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.kinetics.contraption.ContraptionInventory;
import nl.melonstudios.create.kinetics.contraption.IContraptionActor;
import nl.melonstudios.create.kinetics.contraption.accessor.IContraptionAccessor;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * Roller actor: works the strip it rolls over. Own-words port of the
 * RollerMovementBehaviour idea: plowable soil below gets tilled to farmland
 * (hoe sound, like the plough), and soft surface plants in the target cell are
 * crushed straight into the contraption inventory, with leftovers dropped.
 */
public class TileEntityRoller extends TileEntity implements IContraptionActor {
    public TileEntityRoller() {
    }

    public boolean moving = false;

    @Override
    public void setOnContraption(boolean onContraption) {
        this.moving = onContraption;
    }

    @Override
    public boolean isOnContraption() {
        return this.moving;
    }

    @Override
    public void contraptionTick(IContraptionAccessor contraption, World world, Vector3fc position, BlockPos blockPosition, boolean moved, Vector3fc movement) {
        if (!moved || world.isRemote) return;
        BlockPos soilPos = blockPosition.down();
        IBlockState soilState = world.getBlockState(soilPos);
        if (BlockDictionary.isBlockTagged(soilState, "create:plowable")) {
            world.playSound(null, soilPos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.setBlockState(soilPos, Blocks.FARMLAND.getDefaultState(), 3);
        }
        IBlockState at = world.getBlockState(blockPosition);
        if (at.getBlock() instanceof BlockBush) {
            ContraptionInventory inventory = contraption.getInventory();
            NonNullList<ItemStack> drops = NonNullList.create();
            at.getBlock().getDrops(drops, world, blockPosition, at, 0);
            List<ItemStack> leftovers = new ArrayList<>();
            for (ItemStack stack : drops) {
                stack = inventory.insertItem(stack, false);
                if (!stack.isEmpty()) leftovers.add(stack);
            }
            if (!leftovers.isEmpty()) {
                StackUtil.dropItemsAt(world, blockPosition, leftovers.toArray(new ItemStack[0]));
            }
            world.playEvent(2001, blockPosition, BlockBush.getStateId(at));
            world.setBlockToAir(blockPosition);
        }
    }
}
