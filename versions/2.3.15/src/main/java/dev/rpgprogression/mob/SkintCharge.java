package dev.rpgprogression.mob;

import dev.rpgprogression.effect.ModEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;

@EventBusSubscriber(modid="rpgprogression")
public final class SkintCharge {
    public static final ResourceLocation HEALTH_MODIFIER=ResourceLocation.fromNamespaceAndPath("rpgprogression","skint_charged_health");
    public static final String CHECKED="rpgprogression:skint_charge_checked";
    public static final double CHANCE=.10;

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof Mob mob))return;
        if(mob.getPersistentData().getBoolean(CHECKED))return;
        mob.getPersistentData().putBoolean(CHECKED,true);
        boolean guaranteed=false;
        if(mob instanceof EnderDragon){
            var end=level.getServer().getLevel(Level.END);
            var fight=end==null?null:end.getDragonFight();
            guaranteed=SkintChargeWorldData.get(level).claimFirstDragon(fight!=null&&fight.hasPreviouslyKilledDragon());
        }
        // Loaded ordinary mobs keep their saved result. They never receive a new random roll.
        if(guaranteed||!event.loadedFromDisk()&&mob.getRandom().nextDouble()<CHANCE)charge(mob);
    }
    public static boolean charged(LivingEntity mob){return mob.hasEffect(ModEffects.SKINT_CHARGED);}
    public static void charge(Mob mob){
        if(mob.level().isClientSide||charged(mob))return;
        float fraction=mob.getHealth()/mob.getMaxHealth();
        // Dragons and withers reject addEffect; forceAddEffect still runs the native hooks and attribute lifecycle.
        mob.forceAddEffect(new MobEffectInstance(ModEffects.SKINT_CHARGED,MobEffectInstance.INFINITE_DURATION,0,false,true,true),null);
        if(charged(mob))mob.setHealth(fraction*mob.getMaxHealth());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void applicable(MobEffectEvent.Applicable event){
        if(event.getEntity() instanceof Mob&&event.getEffectInstance().is(ModEffects.SKINT_CHARGED))event.setResult(MobEffectEvent.Applicable.Result.APPLY);
    }
    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void damage(LivingIncomingDamageEvent event){
        if(!event.getEntity().level().isClientSide&&event.getSource().getEntity() instanceof Mob attacker&&charged(attacker))event.setAmount(event.getAmount()*1.3F);
    }
    /** Remove only our HP modifier from a copy, retaining other mods' bonuses and attribute clamps. */
    public static double ordinaryMaxHealth(LivingEntity mob){
        if(!charged(mob))return mob.getMaxHealth();
        var attribute=mob.getAttribute(Attributes.MAX_HEALTH);
        if(attribute==null)return mob.getMaxHealth();
        var copy=new AttributeInstance(attribute.getAttribute(),ignored->{});copy.setBaseValue(attribute.getBaseValue());
        for(var modifier:attribute.getModifiers())if(!modifier.id().equals(HEALTH_MODIFIER))copy.addTransientModifier(modifier);
        return (float)copy.getValue(); // Match LivingEntity.getMaxHealth's precision before XP rounding.
    }
    private SkintCharge(){}
}
