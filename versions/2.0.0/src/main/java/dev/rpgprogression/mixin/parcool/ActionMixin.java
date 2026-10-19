package dev.rpgprogression.mixin.parcool;

import com.alrex.parcool.api.action.*;
import com.alrex.parcool.common.Parkourability;
import dev.rpgprogression.compat.ParCoolBridge;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value=Action.class,remap=false)
public abstract class ActionMixin {
    @Shadow @Final protected Parkourability parkourability;
    @Shadow @Final protected ActionEntry<? extends Action> entry;
    @Inject(method="start",at=@At("HEAD"),cancellable=true)
    private void rpg$canStart(CallbackInfo ci){if(ParCoolBridge.blocked(parkourability,entry))ci.cancel();}
    @Inject(method="start",at=@At("TAIL"))
    private void rpg$startCost(CallbackInfo ci){ParCoolBridge.spend(parkourability,entry,StaminaConsumption.Type.START);}
    @Inject(method="takeCost",at=@At("HEAD"))
    private void rpg$workingCost(StaminaConsumption.Type type,CallbackInfo ci){ParCoolBridge.spend(parkourability,entry,type);}
}
