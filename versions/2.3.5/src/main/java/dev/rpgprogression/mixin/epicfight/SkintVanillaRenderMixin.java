package dev.rpgprogression.mixin.epicfight;

import dev.rpgprogression.client.SkintPlayerAnimation;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Use the base player model only for this interaction; combat rendering continues normally elsewhere. */
@Mixin(targets="yesman.epicfight.client.events.engine.RenderEngine",remap=false)
public abstract class SkintVanillaRenderMixin {
    @Inject(method="epicfight$renderLivingPre",at=@At("HEAD"),cancellable=true,remap=false)
    private void rpgprogression$vanillaSkint(RenderLivingEvent.Pre<?,?> event,CallbackInfo ci){
        if(SkintPlayerAnimation.active(event.getEntity()))ci.cancel();
    }
}
