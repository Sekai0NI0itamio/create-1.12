package nl.melonstudios.create.tesr.actor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockRender;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityEncasedFan;
import nl.melonstudios.create.util.EnumRenderPart;

@SideOnly(Side.CLIENT)
public class TESREncasedFan extends TESRKineticBase<TileEntityEncasedFan> {
    @Override
    protected void render(TileEntityEncasedFan te, float pt, float alpha) {
        EnumFacing facing = te.getAirFacing();
        IBlockState state = BlockRender.byEnum(EnumRenderPart.FAN_PROPELLER);
        IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);
        // Propeller spins around the fan's facing axis.
        this.spinModel(te, pt, facing.getAxis(), model, state, 1.0F);
    }
}
