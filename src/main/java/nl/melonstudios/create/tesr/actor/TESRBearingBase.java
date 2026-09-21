package nl.melonstudios.create.tesr.actor;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockRender;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntityBearingBase;
import nl.melonstudios.create.util.EnumRenderPart;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SideOnly(Side.CLIENT)
public class TESRBearingBase<T extends TileEntityBearingBase> extends TESRKineticBase<T> {
    @Override
    protected void render(T te, float pt, float alpha) {
        EnumFacing facing = te.getFacing();
        this.spinHalfShaft(te, te.getSpeed(), facing.getOpposite(), pt);
        if (te.isAssembled()) {
            {
                GlStateManager.pushMatrix();
                IBlockState state = BlockRender.byEnum(EnumRenderPart.getBearingPlate(facing));
                IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);
                this.rotateModel((float)MathHelper.clampedLerp(te.angleOld, te.angle, pt), facing.getAxis(), model, state, 1.0F);
                GlStateManager.popMatrix();
            }
        }
    }
}
