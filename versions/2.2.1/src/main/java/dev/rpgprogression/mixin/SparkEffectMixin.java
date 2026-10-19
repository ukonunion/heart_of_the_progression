package dev.rpgprogression.mixin;

import dev.rpgprogression.effect.ModEffects;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Expose one real status to vanilla blindness/nausea queries without adding two extra effects. */
@Mixin(LivingEntity.class)
public abstract class SparkEffectMixin {
    @Unique private MobEffectInstance rpg$spark(Holder<MobEffect> requested){
        if((!requested.equals(MobEffects.BLINDNESS)&&!requested.equals(MobEffects.CONFUSION))||!ModEffects.SPARK_EXHAUSTION.isBound())return null;
        return ((LivingEntity)(Object)this).getActiveEffectsMap().get(ModEffects.SPARK_EXHAUSTION);
    }
    @Inject(method="hasEffect",at=@At("RETURN"),cancellable=true)
    private void rpg$combinedPresence(Holder<MobEffect> effect,CallbackInfoReturnable<Boolean> cir){
        if(!cir.getReturnValue()&&rpg$spark(effect)!=null)cir.setReturnValue(true);
    }
    @Inject(method="getEffect",at=@At("RETURN"),cancellable=true)
    private void rpg$combinedDuration(Holder<MobEffect> effect,CallbackInfoReturnable<MobEffectInstance> cir){
        var spark=rpg$spark(effect);if(spark==null)return;
        // Vanilla nausea stops building during its last 60 ticks. Keep a 3-second status functional.
        int duration=spark.isInfiniteDuration()?-1:spark.getDuration()+(effect.equals(MobEffects.CONFUSION)?60:0);
        var original=cir.getReturnValue();
        if(original!=null&&(original.isInfiniteDuration()||duration!=-1&&original.getDuration()>=duration))return;
        cir.setReturnValue(new MobEffectInstance(effect,duration,original==null?0:original.getAmplifier(),false,false,false));
    }
}
