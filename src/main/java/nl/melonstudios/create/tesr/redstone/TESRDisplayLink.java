package nl.melonstudios.create.tesr.redstone;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.tileentity.redstone.TileEntityDisplayLink;

/**
 * Renders the display-link line as floating amber text above the block.
 */
@SideOnly(Side.CLIENT)
public class TESRDisplayLink extends TileEntitySpecialRenderer<TileEntityDisplayLink> {
    @Override
    public void render(TileEntityDisplayLink te, double x, double y, double z, float pt, int destroyStage, float alpha) {
        String line = te.line;
        if (line == null || line.isEmpty()) return;
        FontRenderer fr = this.getFontRenderer();
        if (fr == null) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 1.1, z + 0.5);
        GlStateManager.rotate(-this.rendererDispatcher.entityYaw, 0, 1, 0);
        float scale = 0.016F;
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        int w = fr.getStringWidth(line);
        fr.drawString(line, -w / 2, 0, 0xFFAA33);
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
