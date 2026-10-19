package dev.rpgprogression.effect;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.*;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,"rpgprogression");
    public static final RegistryObject<MobEffect> SPARK_EXHAUSTION=EFFECTS.register("spark_exhaustion",()->new SparkExhaustion());
    public static void exhaust(LivingEntity entity,int ticks){
        entity.addEffect(new MobEffectInstance(SPARK_EXHAUSTION.get(),ticks,0,false,false,true));
    }
    private static final class SparkExhaustion extends MobEffect {
        private SparkExhaustion(){super(MobEffectCategory.HARMFUL,0xC8A464);}
        @Override public java.util.List<net.minecraft.world.item.ItemStack> getCurativeItems(){return java.util.List.of();}
    }
    private ModEffects(){}
}
