package nl.melonstudios.create.tesr.actor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.block.BlockRender;
import nl.melonstudios.create.tesr.TESRKineticBase;
import nl.melonstudios.create.tileentity.actor.TileEntitySaw;
import nl.melonstudios.create.util.EnumRenderPart;
import nl.melonstudios.create.util.EnumRenderPart;

@SideOnly(Side.CLIENT)
public class TESRSaw extends TESRKineticBase<TileEntitySaw> {
    @Override
    protected void render(TileEntitySaw te, float pt, float alpha) {
        EnumFacing facing = te.facing();
        EnumFacing.Axis axis = facing.getAxis();
        IBlockState saw = BlockRender.byEnum(EnumRenderPart.getSaw(axis));
        this.spinHalfShaft(te, te.getSpeed(), facing.getOpposite(), pt);
        GlStateManager.pushMatrix();
        GlStateManager.translate(facing.getFrontOffsetX() * 0.25F, 0.0F, facing.getFrontOffsetZ() * 0.25F);
        this.spinModel(te, pt, axis, this.mc.getBlockRendererDispatcher().getModelForState(saw), saw, 4.0F);
        GlStateManager.popMatrix();
    }
}
