package nl.melonstudios.create.tesr.generator;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockRender;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.generator.TileEntityHandCrank;
import nl.melonstudios.create.util.EnumRenderPart;

@SideOnly(Side.CLIENT)
public class TESRHandCrank extends TESRKineticBase<TileEntityHandCrank> {
    public TESRHandCrank() {
        for (int i = 0; i < 6; i++) this.facings[i] = BlockInit.HAND_CRANK.getStateFromMeta(i);
        this.handle = BlockRender.byEnum(EnumRenderPart.HAND_CRANK_HANDLE);
    }

    protected final IBlockState[] facings = new IBlockState[6];
    protected final IBlockState handle;
    @Override
    protected void render(TileEntityHandCrank te, float pt, float alpha) {
        EnumFacing facing = te.getRenderFacing();
        // Base stub spins rigidly at the kinetic shaft angle.
        IBlockState state = this.facings[facing.getIndex()];
        IBakedModel model = this.mc.getBlockRendererDispatcher().getModelForState(state);

        this.spinModel(te, pt, facing.getAxis(), model, state, 1.0F);

        // Handle is a separate part spinning at its own chased angle, so it
        // lags the shaft on spin-up/down instead of snapping with it.
        IBakedModel handleModel = this.mc.getBlockRendererDispatcher().getModelForState(this.handle);
        this.rotateHandle(te.getIndependentAngle(pt), facing, handleModel, this.handle);
    }

    /**
     * Spins the UP-authored handle around the crank shaft: the baked part has
     * a single fixed orientation, so the facing alignment is applied here in
     * GL (authored +Y onto the facing direction) with the independent spin
     * around the world shaft axis outside it.
     */
    protected final void rotateHandle(float angle, EnumFacing facing, IBakedModel model, IBlockState state) {
        EnumFacing.Axis axis = facing.getAxis();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        GlStateManager.rotate(angle,
                axis == EnumFacing.Axis.X ? 1 : 0,
                axis == EnumFacing.Axis.Y ? 1 : 0,
                axis == EnumFacing.Axis.Z ? 1 : 0);
        switch (facing) {
            case DOWN:
                GlStateManager.rotate(180.0F, 1, 0, 0);
                break;
            case NORTH:
                GlStateManager.rotate(-90.0F, 1, 0, 0);
                break;
            case SOUTH:
                GlStateManager.rotate(90.0F, 1, 0, 0);
                break;
            case EAST:
                GlStateManager.rotate(-90.0F, 0, 0, 1);
                break;
            case WEST:
                GlStateManager.rotate(90.0F, 0, 0, 1);
                break;
            default:
                break;
        }
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        this.renderBakedModel(1.0F, model, state);
        GlStateManager.popMatrix();
    }
}
