package dev.rpgprogression.mixin.client;

import dev.rpgprogression.client.hud.CenteredHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Let the native XP level render during its temporary window on a saddled mount. */
@Mixin(Gui.class)
public abstract class ExperienceVisibilityMixin {
    @Inject(method="isExperienceBarVisible",at=@At("HEAD"),cancellable=true)
    private void rpg$mountedExperience(CallbackInfoReturnable<Boolean> cir) {
        var mc=Minecraft.getInstance();
        if(CenteredHud.experienceOpacity()>0&&mc.player!=null&&!mc.player.isSpectator()&&mc.gameMode!=null)
            cir.setReturnValue(mc.gameMode.hasExperience());
    }
}
