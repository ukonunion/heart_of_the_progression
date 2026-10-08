package dev.rpgprogression.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.rpgprogression.client.hud.CenteredHud;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fade the actual vanilla XP textures and level text, including the mounted fallback. */
@Mixin(Gui.class)
public abstract class ExperienceHudMixin {
    @Inject(method="renderExperienceBar",at=@At("HEAD"),cancellable=true)
    private void rpg$fade(GuiGraphics graphics,int x,CallbackInfo ci){float alpha=CenteredHud.experienceOpacity();if(alpha<=0){ci.cancel();return;}graphics.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,alpha);}
    @ModifyArg(method="renderExperienceBar",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"),index=4)
    private int rpg$levelAlpha(int color){return (Math.max(1,Math.round(CenteredHud.experienceOpacity()*255))<<24)|(color&0xFFFFFF);}
    @Inject(method="renderExperienceBar",at=@At("TAIL"))
    private void rpg$restore(GuiGraphics graphics,int x,CallbackInfo ci){graphics.flush();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();}
}
