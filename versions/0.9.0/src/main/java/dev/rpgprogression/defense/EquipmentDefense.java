package dev.rpgprogression.defense;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/** Estimates native equipment reduction for the UI, never applies it a second time. */
public final class EquipmentDefense {
    public static final float REFERENCE_DAMAGE=10;
    public static double armorPercent(double armor,double toughness){return (1-CombatRules.getDamageAfterAbsorb(REFERENCE_DAMAGE,(float)Math.floor(armor),(float)toughness)/REFERENCE_DAMAGE)*100;}
    public static DamageSource magicSource(Player p){return p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("irons_spellbooks","fire_magic"))).map(h->new DamageSource(h)).orElseGet(()->p.damageSources().magic());}
    public static boolean nativeSpell(DamageSource source){return source.typeHolder().unwrapKey().map(k->k.location().getNamespace().equals("irons_spellbooks")).orElse(false);}
    public static double spellPercent(double armor,double toughness,double resistance,boolean armorApplies){
        float damage=(float)(REFERENCE_DAMAGE*spellRemaining(resistance));
        if(armorApplies)damage=CombatRules.getDamageAfterAbsorb(damage,(float)Math.floor(armor),(float)toughness);
        return (1-damage/REFERENCE_DAMAGE)*100;
    }
    public static double spellRemaining(double resistance){return 2-(resistance<=1.5?resistance:2-.25/(resistance-1));}
    public static double spellResistance(Player p){
        var attribute=ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks","spell_resist"));
        return attribute!=null&&p.getAttribute(attribute)!=null?p.getAttributeValue(attribute):1;
    }
    public static double percent(Player p,DefenseType type){
        if(p==null||type!=DefenseType.PHYSICAL&&type!=DefenseType.MAGICAL)return 0;
        var source=type==DefenseType.PHYSICAL?p.damageSources().mobAttack(p):magicSource(p);
        double remaining=1-spellPercent(p.getArmorValue(),p.getAttributeValue(Attributes.ARMOR_TOUGHNESS),type==DefenseType.MAGICAL&&nativeSpell(source)?spellResistance(p):1,!source.is(DamageTypeTags.BYPASSES_ARMOR))/100;
        int protection=EnchantmentHelper.getDamageProtection(p.getArmorSlots(),source);
        remaining*=CombatRules.getDamageAfterMagicAbsorb(REFERENCE_DAMAGE,protection)/REFERENCE_DAMAGE;
        return Math.min(100,(1-remaining)*100);
    }
    public static double combine(double first,double second){return 100*(1-(1-first/100)*(1-second/100));}
    private EquipmentDefense(){}
}
