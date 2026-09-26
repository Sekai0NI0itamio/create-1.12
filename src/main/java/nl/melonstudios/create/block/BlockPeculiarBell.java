package nl.melonstudios.create.block;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.util.BlockProperties;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Peculiar bell. Reference: PeculiarBellBlock (rings on use and redstone
 * pulse; converts to the haunted bell when placed/updated above soul
 * fire/soul campfire with conversion particles + sound).
 * 1.12 simplification: no soul fire exists, so the trigger block is lava or
 * lit fire below; conversion keeps facing-less placement and plays the
 * peculiar sound (haunted sound registration is NEEDS-LEAD).
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockPeculiarBell extends Block {
    public BlockPeculiarBell() {
        super(Material.IRON);
        this.blockSoundType = SoundType.METAL;
        this.setHardness(BlockProperties.IRON_HARDNESS);
        this.setResistance(BlockProperties.IRON_HARDNESS);
        this.setCreativeTab(CreateTabs.TAB_CREATE);
    }

    /** Reference playSound volume 2.0, pitch 0.94. */
    public float ringPitch() {
        return 0.94F;
    }

    public void ring(World worldIn, BlockPos pos) {
        worldIn.playSound(null, pos, SoundInit.peculiar_bell_use,
                SoundCategory.BLOCKS, 2.0F, ringPitch());
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) ring(worldIn, pos);
        return true;
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (worldIn.isRemote) return;
        if (worldIn.isBlockPowered(pos)) ring(worldIn, pos);
        tryConvert(worldIn, pos, state);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) tryConvert(worldIn, pos, state);
    }

    /** Reference tryConvert: soul fire/campfire below becomes the haunted bell. */
    protected void tryConvert(World worldIn, BlockPos pos, IBlockState state) {
        IBlockState below = worldIn.getBlockState(pos.down());
        boolean hauntedGround = below.getBlock() == Blocks.LAVA
                || below.getBlock() == Blocks.FLOWING_LAVA
                || below.getBlock() == Blocks.FIRE;
        if (!hauntedGround || !(state.getBlock() instanceof BlockPeculiarBell)) return;
        // Block swap itself is NEEDS-LEAD (BlockInit holds the haunted instance).
        BlockHauntedBell.convert(worldIn, pos);
        worldIn.playSound(null, pos, SoundInit.peculiar_bell_use, SoundCategory.BLOCKS, 2.0F, 0.6F);
        for (int i = 0; i < 20; i++) {
            worldIn.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    (worldIn.rand.nextDouble() - 0.5D) * 0.2D, 0.2D, (worldIn.rand.nextDouble() - 0.5D) * 0.2D,
                    new int[0]);
        }
    }
}
