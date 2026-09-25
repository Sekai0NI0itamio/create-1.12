package nl.melonstudios.create.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.network.CreateLegacyPacketManager;
import nl.melonstudios.create.network.CreateLegacySPackets;
import nl.melonstudios.create.init.SoundInit;
import nl.melonstudios.create.tileentity.marker.ITileEntityWithSubInteractions;
import nl.melonstudios.create.util.SubInteractionBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(InventoryPlayer.class)
public abstract class MixinInventoryPlayer {
    @Shadow
    public EntityPlayer player;

    @SideOnly(Side.CLIENT)
    @Inject(method = "changeCurrentItem", at = @At("HEAD"), cancellable = true)
    public void changeCurrentItem(int direction, CallbackInfo ci) {
        RayTraceResult object = Minecraft.getMinecraft().objectMouseOver;
        start:
        if (object.typeOfHit == RayTraceResult.Type.BLOCK) {
            World world = this.player.world;
            BlockPos pos = object.getBlockPos();
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof ITileEntityWithSubInteractions) {
                Collection<SubInteractionBox> boxes = ((ITileEntityWithSubInteractions)te).getSubInteractionBoxes();
                float hitX = (float) (object.hitVec.x - pos.getX());
                float hitY = (float) (object.hitVec.y - pos.getY());
                float hitZ = (float) (object.hitVec.z - pos.getZ());
                for (SubInteractionBox box : boxes) {
                    if (box.isInside(hitX, hitY, hitZ) && box.getInteraction() instanceof SubInteractionBox.ScrollInteraction) {
                        SubInteractionBox.ScrollInteraction interaction = (SubInteractionBox.ScrollInteraction) box.getInteraction();
                        CreateLegacyPacketManager.sendToServer(CreateLegacySPackets.SCROLL_INTERACTION.create(pos, hitX, hitY, hitZ, direction));
                        if (interaction.scroll(this.player, this.player.isSneaking(), this.player.getHeldItem(EnumHand.MAIN_HAND), direction)) {
                            this.player.playSound(SoundInit.scroll_value, 0.25F, 1.2F);
                            ci.cancel();
                        }
                        break start;
                    }
                }
            }
        }
    }
}
