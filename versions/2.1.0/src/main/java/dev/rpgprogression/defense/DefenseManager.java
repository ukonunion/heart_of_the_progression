package dev.rpgprogression.defense;

import dev.rpgprogression.attribute.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.player.Player;

public final class DefenseManager {
    public static double percent(PlayerProgression d,DefenseType type) {
        double raw=switch(type) {
            case PHYSICAL -> AttributeManager.bonus(d,RPGAttribute.VITALITY)*RPGConfig.PHYSICAL.get();
            case ELEMENTAL -> AttributeManager.bonus(d,RPGAttribute.INTELLIGENCE)*RPGConfig.ELEMENTAL.get();
            case MAGICAL -> AttributeManager.bonus(d,RPGAttribute.INTELLIGENCE)*RPGConfig.MAGICAL.get();
            case FIRE -> AttributeManager.bonus(d,RPGAttribute.ENDURANCE)*RPGConfig.FIRE.get();
            case POISON -> AttributeManager.bonus(d,RPGAttribute.VITALITY)*RPGConfig.POISON.get();
            case EXHAUSTION -> AttributeManager.bonus(d,RPGAttribute.ENDURANCE)*RPGConfig.EXHAUSTION.get();
        };
        return Math.min(RPGConfig.DEFENSE_CAP.get(),raw+dev.rpgprogression.food.FoodManager.protection(d));
    }
    public static double displayedPercent(Player p,PlayerProgression d,DefenseType type){return EquipmentDefense.combine(percent(d,type),EquipmentDefense.percent(p,type));}
    public static DefenseType classify(DamageSource source) {
        if(source.is(DamageTypes.FELL_OUT_OF_WORLD)||source.is(DamageTypes.GENERIC_KILL))return null;
        if(source.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("rpgprogression","is_magic"))))return DefenseType.MAGICAL;
        if(source.is(DamageTypeTags.IS_FIRE))return DefenseType.FIRE;
        if(source.is(DamageTypes.STARVE)||source.is(DamageTypes.DRY_OUT))return DefenseType.EXHAUSTION;
        if(source.is(DamageTypes.LIGHTNING_BOLT)||source.is(DamageTypes.FREEZE))return DefenseType.ELEMENTAL;
        return DefenseType.PHYSICAL;
    }
    public static DefenseType classify(DamageSource source,net.minecraft.world.entity.LivingEntity target){
        // Poison ticks and instant harming share MAGIC; retain poison defense for active poison.
        if(source.is(DamageTypes.MAGIC)&&source.getEntity()==null&&source.getDirectEntity()==null
                &&target.hasEffect(net.minecraft.world.effect.MobEffects.POISON))return DefenseType.POISON;
        return classify(source);
    }
    private DefenseManager() {}
}
