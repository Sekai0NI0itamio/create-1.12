package nl.melonstudios.create.tesr.actor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockRender;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityMixer;
import nl.melonstudios.create.util.EnumRenderPart;
import nl.melonstudios.create.util.Utils;

@SideOnly(Side.CLIENT)
public class TESRMixer extends TESRKineticBase<TileEntityMixer> {
    @Override
    protected void render(TileEntityMixer te, float pt, float alpha) {
        this.spinShaftlessCog(te, te.getSpeed(), EnumFacing.Axis.Y, pt);

        // Cosine-eased drop matching the reference lower curve; the backport
        // lowers over 0 -> 20 ticks and holds, so only the ease-in half applies.
        // NOTE: the reference also renders a pole between cog and head; no pole
        // model exists in the backport (see NEEDS-LEAD), so only the whisk renders.
        float num = Utils.clampedLerp(pt, te.loweringOld, te.lowering) / 20.0F;
        float eased = (2.0F - (float) Math.cos(num * Math.PI)) * 0.5F - 0.5F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, -eased, 0.0F);
        GlStateManager.translate(0.0F, -1.0F, 0.0F);
        {
            IBlockState state = BlockRender.byEnum(EnumRenderPart.WHISK);
            IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);
            // Reference spins the head 2x while fully lowered, 1x while moving, half speed at rest.
            float m = te.lowering >= 20 ? 2.0F : (te.lowering > 0 ? 1.0F : 0.5F);
            this.spinModel(te, pt, EnumFacing.Axis.Y, model, state, m);
        }
        GlStateManager.popMatrix();
    }
}
