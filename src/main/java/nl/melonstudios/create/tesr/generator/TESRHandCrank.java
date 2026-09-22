package nl.melonstudios.create.tesr.generator;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.generator.TileEntityHandCrank;

@SideOnly(Side.CLIENT)
public class TESRHandCrank extends TESRKineticBase<TileEntityHandCrank> {
    public TESRHandCrank() {
        for (int i = 0; i < 6; i++) this.facings[i] = BlockInit.HAND_CRANK.getStateFromMeta(i);
    }

    protected final IBlockState[] facings = new IBlockState[6];
    @Override
    protected void render(TileEntityHandCrank te, float pt, float alpha) {
        EnumFacing facing = te.getRenderFacing();
        // The crank block model is a single piece (no separate handle part yet),
        // and it spins at exactly the shaft angle, so one spin call draws it.
        // A second shaft-only pass would redraw the same shaft twice (z-fight).
        IBlockState state = this.facings[facing.getIndex()];
        IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);

        this.spinModel(te, pt, facing.getAxis(), model, state, 1.0F);
    }
}
