package nl.melonstudios.create.tesr.generator;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
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
        // Reference renders the kinetic shaft plus the crank handle.
        this.spinShaft(te, pt, facing.getAxis());
        IBlockState state = this.facings[facing.getIndex()];
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);

        this.spinModel(te, pt, facing.getAxis(), model, state, 1.0F);
    }
}
