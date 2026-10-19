package dev.rpgprogression.effect;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.*;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,"rpgprogression");
    public static final DeferredHolder<MobEffect,MobEffect> SPARK_EXHAUSTION=EFFECTS.register("spark_exhaustion",()->new SparkExhaustion());
    public static final DeferredHolder<MobEffect,MobEffect> SKINT_CHARGED=EFFECTS.register("skint_charged",SkintCharged::new);
    public static void exhaust(LivingEntity entity,int ticks){
        entity.addEffect(new MobEffectInstance(SPARK_EXHAUSTION,ticks,0,false,false,true));
    }
    private static final class SparkExhaustion extends MobEffect {
        private SparkExhaustion(){super(MobEffectCategory.HARMFUL,0xC8A464);}
        @Override public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> cures,MobEffectInstance effect){cures.clear();}
    }
    private static final class SkintCharged extends MobEffect {
        private SkintCharged(){
            super(MobEffectCategory.BENEFICIAL,0xFFD56A);
            addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,dev.rpgprogression.mob.SkintCharge.HEALTH_MODIFIER,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,amplifier->.5);
        }
        @Override public net.minecraft.core.particles.ParticleOptions createParticleOptions(MobEffectInstance instance){return dev.rpgprogression.particle.ModParticles.SKINT_CHARGED.get();}
        @Override public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> cures,MobEffectInstance effect){cures.clear();}
    }
    private ModEffects(){}
}
