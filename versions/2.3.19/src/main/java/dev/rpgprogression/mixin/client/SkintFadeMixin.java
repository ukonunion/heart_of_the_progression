package dev.rpgprogression.mixin.client;

import dev.rpgprogression.client.SkintFade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class SkintFadeMixin {
    @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
    private void rpgprogression$travelFade(DeltaTracker delta,boolean renderLevel,CallbackInfo ci){
        if(SkintFade.active()){
            var mc=Minecraft.getInstance();SkintFade.render(new GuiGraphics(mc,mc.renderBuffers().bufferSource()));
        }
    }
}
