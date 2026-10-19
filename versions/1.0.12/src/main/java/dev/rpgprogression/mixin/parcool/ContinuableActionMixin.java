package dev.rpgprogression.mixin.parcool;

import com.alrex.parcool.api.action.*;
import com.alrex.parcool.common.Parkourability;
import dev.rpgprogression.compat.ParCoolBridge;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value=ContinuableAction.class,remap=false)
public abstract class ContinuableActionMixin extends Action {
    protected ContinuableActionMixin(Parkourability p,ActionEntry<? extends Action> e){super(p,e);}
    @Shadow public abstract boolean isDoing();
    @Shadow public abstract void finish();
    @Inject(method="start",at=@At("HEAD"),cancellable=true)
    private void rpg$canStart(CallbackInfo ci){if(ParCoolBridge.blocked(parkourability,entry))ci.cancel();}
    @Inject(method="tickOnWorking",at=@At("HEAD"),cancellable=true)
    private void rpg$stopExhausted(CallbackInfo ci){
        if(ParCoolBridge.blocked(parkourability,entry)){finish();ci.cancel();}
    }
    @Inject(method="finish",at=@At("HEAD"))
    private void rpg$finishCost(CallbackInfo ci){
        if(isDoing())ParCoolBridge.spend(parkourability,entry,StaminaConsumption.Type.FINISH);
    }
}
