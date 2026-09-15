package nl.melonstudios.create.tesr.actor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockRender;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityCrushingWheel;
import nl.melonstudios.create.util.EnumRenderPart;

@SideOnly(Side.CLIENT)
public class TESRCrushingWheel extends TESRKineticBase<TileEntityCrushingWheel> {
    @Override
    protected void render(TileEntityCrushingWheel te, float pt, float alpha) {
        EnumFacing.Axis axis = te.getRenderAxis();
        IBlockState state = BlockRender.byEnum(axis == EnumFacing.Axis.Z
                ? EnumRenderPart.CRUSHING_WHEEL_Z : EnumRenderPart.CRUSHING_WHEEL_X);
        IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);
        this.spinModel(te, pt, axis, model, state, 1.0F);
    }
}
