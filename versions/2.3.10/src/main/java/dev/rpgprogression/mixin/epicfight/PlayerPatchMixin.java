package dev.rpgprogression.mixin.epicfight;

import dev.rpgprogression.compat.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.stamina.StaminaManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Pseudo @Mixin(value=PlayerPatch.class,remap=false)
public abstract class PlayerPatchMixin {
    @Unique private PlayerPatch<?> rpg$patch(){return (PlayerPatch<?>)(Object)this;}
    @Inject(method="getMaxStamina",at=@At("HEAD"),cancellable=true)
    private void rpg$max(CallbackInfoReturnable<Float> cir){
        var p=rpg$patch().getOriginal();if(EpicFightCompat.shares(p))cir.setReturnValue((float)(ProgressionManager.get(p).maxStamina()/RPGConfig.EPICFIGHT_UNIT.get()));
    }
    @Inject(method="getStamina",at=@At("HEAD"),cancellable=true)
    private void rpg$value(CallbackInfoReturnable<Float> cir){
        var p=rpg$patch().getOriginal();if(EpicFightCompat.shares(p))cir.setReturnValue((float)(ProgressionManager.get(p).stamina/RPGConfig.EPICFIGHT_UNIT.get()));
    }
    @Inject(method="hasStamina",at=@At("HEAD"),cancellable=true)
    private void rpg$canSpend(float amount,CallbackInfoReturnable<Boolean> cir){
        var p=rpg$patch().getOriginal();if(EpicFightCompat.shares(p)){
            cir.setReturnValue(Float.isFinite(amount)&&amount>=0&&StaminaManager.canSpend(p,amount*RPGConfig.EPICFIGHT_UNIT.get()));
        }
    }
    @Inject(method="setStamina",at=@At("HEAD"),cancellable=true)
    private void rpg$set(float value,CallbackInfo ci){
        if(EpicFightCompat.shares(rpg$patch().getOriginal())){EpicFightBridge.set(rpg$patch(),value);ci.cancel();}
    }
    @Inject(method="setStaminaRegenAwaitTicks",at=@At("TAIL"))
    private void rpg$skillRegenDelay(int ticks,CallbackInfo ci){
        if(EpicFightCompat.shares(rpg$patch().getOriginal()))EpicFightBridge.releaseRegenerationDelay(rpg$patch(),ticks);
    }
    /** Preserve animation/action ticking, suppress only Epic Fight's parallel stamina regeneration. */
    @Redirect(method="preTickServer",at=@At(value="INVOKE",target="Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;setStamina(F)V"))
    private void rpg$singleRegen(PlayerPatch<?> patch,float value){if(!EpicFightCompat.shares(patch.getOriginal()))patch.setStamina(value);}
}
