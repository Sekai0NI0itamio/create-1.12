package nl.melonstudios.create.block.train;

import nl.melonstudios.create.init.CreateTabs;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nullable;

/**
 * Fake track: invisible map marker showing where real track runs. No
 * collision, replaceable, never opaque — trains render over it. Breaks
 * instantly like the reference.
 */
@SuppressWarnings("deprecation")
public class BlockFakeTrack extends Block {
    public BlockFakeTrack() {
        super(Material.CIRCUITS, MapColor.IRON);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(0.0F);
        this.setResistance(0.0F);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
        this.setRegistryName("fake_track");
        this.setUnlocalizedName("create.fake_track");
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.125, 1.0);
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }
}
