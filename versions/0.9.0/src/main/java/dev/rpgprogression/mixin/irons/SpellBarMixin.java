package dev.rpgprogression.mixin.irons;
import dev.rpgprogression.compat.IronsCompat;
import io.redspace.ironsspellbooks.gui.overlays.SpellBarOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(value=SpellBarOverlay.class,remap=false)
public abstract class SpellBarMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void rpg$hideSpellSet(ForgeGui gui,GuiGraphics g,float partial,int width,int height,CallbackInfo ci){
        if(IronsCompat.enabled())ci.cancel();
    }
}
