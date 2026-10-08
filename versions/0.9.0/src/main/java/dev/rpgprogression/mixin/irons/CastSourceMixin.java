package dev.rpgprogression.mixin.irons;
import dev.rpgprogression.compat.IronsCompat;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(value=CastSource.class,remap=false)
public abstract class CastSourceMixin {
    @Inject(method="consumesMana",at=@At("HEAD"),cancellable=true)
    private void rpg$paidScrolls(CallbackInfoReturnable<Boolean> cir){
        CastSource source=(CastSource)(Object)this;
        if(IronsCompat.enabled()&&(source==CastSource.SCROLL||source==CastSource.SWORD))cir.setReturnValue(true);
    }
}
