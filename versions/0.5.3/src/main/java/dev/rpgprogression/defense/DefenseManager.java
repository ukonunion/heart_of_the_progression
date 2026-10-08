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
            case FIRE -> AttributeManager.bonus(d,RPGAttribute.ENDURANCE)*RPGConfig.FIRE.get();
            case POISON -> AttributeManager.bonus(d,RPGAttribute.VITALITY)*RPGConfig.POISON.get();
            case EXHAUSTION -> AttributeManager.bonus(d,RPGAttribute.ENDURANCE)*RPGConfig.EXHAUSTION.get();
        };
        return Math.min(RPGConfig.DEFENSE_CAP.get(),raw);
    }
    public static DefenseType classify(DamageSource source) {
        if(source.is(DamageTypes.FELL_OUT_OF_WORLD)||source.is(DamageTypes.GENERIC_KILL))return null;
        if(source.is(DamageTypeTags.IS_FIRE))return DefenseType.FIRE;
        // Vanilla poison uses indirect-free MAGIC. Attributed magic uses elemental defense.
        if(source.is(DamageTypes.MAGIC)&&source.getEntity()==null&&source.getDirectEntity()==null)return DefenseType.POISON;
        if(source.is(DamageTypes.STARVE)||source.is(DamageTypes.DRY_OUT))return DefenseType.EXHAUSTION;
        if(source.is(DamageTypes.LIGHTNING_BOLT)||source.is(DamageTypes.FREEZE)||source.is(DamageTypes.MAGIC)
                ||source.is(DamageTypes.INDIRECT_MAGIC)||source.is(DamageTypes.WITHER)||source.is(DamageTypes.DRAGON_BREATH))return DefenseType.ELEMENTAL;
        return DefenseType.PHYSICAL;
    }
    public static float reduceFoodExhaustion(Player p,float amount) {
        return p.getCapability(PlayerProgressionProvider.CAPABILITY).map(d->(float)(amount*(1-percent(d,DefenseType.EXHAUSTION)/100))).orElse(amount);
    }
    private DefenseManager() {}
}
