package dev.rpgprogression.mixin.irons;

import dev.rpgprogression.compat.*;
import dev.rpgprogression.progression.ProgressionManager;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo @Mixin(value=MagicData.class,remap=false)
public abstract class MagicDataMixin {
    @Shadow private ServerPlayer serverPlayer;
    @Shadow private float mana;
    @Shadow public abstract void setMana(float value);
    @Inject(method="getMana",at=@At("HEAD"),cancellable=true)
    private void rpg$mana(CallbackInfoReturnable<Float> cir){if(IronsCompat.shares(serverPlayer)){mana=(float)ProgressionManager.get(serverPlayer).mana;cir.setReturnValue(mana);}}
    @Inject(method="setMana",at=@At("HEAD"),cancellable=true)
    private void rpg$set(float value,CallbackInfo ci){if(IronsCompat.shares(serverPlayer)){mana=IronsBridge.set(serverPlayer,(MagicData)(Object)this,value);ci.cancel();}}
    @Inject(method="addMana",at=@At("HEAD"),cancellable=true)
    private void rpg$add(float amount,CallbackInfo ci){if(IronsCompat.shares(serverPlayer)){setMana((float)ProgressionManager.get(serverPlayer).mana+amount);ci.cancel();}}
}
