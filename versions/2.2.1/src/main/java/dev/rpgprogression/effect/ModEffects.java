package dev.rpgprogression.effect;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.*;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,"rpgprogression");
    public static final DeferredHolder<MobEffect,MobEffect> SPARK_EXHAUSTION=EFFECTS.register("spark_exhaustion",()->new SparkExhaustion());
    public static void exhaust(LivingEntity entity,int ticks){
        entity.addEffect(new MobEffectInstance(SPARK_EXHAUSTION,ticks,0,false,false,true));
    }
    private static final class SparkExhaustion extends MobEffect {
        private SparkExhaustion(){super(MobEffectCategory.HARMFUL,0xC8A464);}
        @Override public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> cures,MobEffectInstance effect){cures.clear();}
    }
    private ModEffects(){}
}
