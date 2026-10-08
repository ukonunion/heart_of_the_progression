package dev.rpgprogression.mixin.irons;

import dev.rpgprogression.compat.IronsCompat;
import io.redspace.ironsspellbooks.gui.overlays.ManaBarOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(value=ManaBarOverlay.class,remap=false)
public abstract class ManaOverlayMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void rpg$singleHud(ForgeGui gui,GuiGraphics graphics,float partial,int w,int h,CallbackInfo ci){if(IronsCompat.enabled())ci.cancel();}
}
