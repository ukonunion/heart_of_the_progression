package dev.rpgprogression.defense;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Shared armor policy for actual damage and the menu. Optional mods are identified by registry ID. */
public final class EquipmentDefense {
    public static final float REFERENCE_DAMAGE=10;
    public static final double IRONS_ARMOR_WEIGHT=.5;
    private static final TagKey<DamageType> MAGIC=TagKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("rpgprogression","is_magic"));
    public static boolean ironsArmor(ItemStack stack){return stack.getItem() instanceof ArmorItem&&ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace().equals("irons_spellbooks");}
    public static boolean magical(DamageSource source){return source.is(MAGIC)||nativeSpell(source);}
    public static double armorPercent(double armor,double toughness){return (1-CombatRules.getDamageAfterAbsorb(REFERENCE_DAMAGE,(float)Math.floor(armor),(float)toughness)/REFERENCE_DAMAGE)*100;}
    public static DamageSource magicSource(Player p){return p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("irons_spellbooks","fire_magic"))).map(h->new DamageSource(h)).orElseGet(()->p.damageSources().magic());}
    public static boolean nativeSpell(DamageSource source){return source.typeHolder().unwrapKey().map(k->k.location().getNamespace().equals("irons_spellbooks")).orElse(false);}
    private static AttributeModifier weighted(AttributeModifier modifier){return new AttributeModifier(modifier.getId(),modifier.getName(),modifier.getAmount()*IRONS_ARMOR_WEIGHT,modifier.getOperation());}
    /** Equipment-only calculation also used by registry reports. Ordinary armor has no magic contribution. */
    public static double armorAttribute(Iterable<ItemStack> stacks,Attribute attribute,boolean magic){
        var result=new AttributeInstance(attribute,x->{});result.setBaseValue(0);
        for(var stack:stacks){if(!(stack.getItem() instanceof ArmorItem item)||magic&&!ironsArmor(stack))continue;
            for(var modifier:stack.getAttributeModifiers(item.getEquipmentSlot()).get(attribute)){
                if(result.getModifier(modifier.getId())!=null)result.removeModifier(modifier.getId());
                result.addTransientModifier(ironsArmor(stack)?weighted(modifier):modifier);
            }
        }return result.getValue();
    }
    public static double armorAttribute(Player p,Attribute attribute,boolean magic){
        if(magic)return armorAttribute(p.getArmorSlots(),attribute,true);
        // Retain base armor and unrelated native modifiers; change only Iron's equipped armor.
        var actual=p.getAttribute(attribute);if(actual==null)return 0;
        var result=new AttributeInstance(attribute,x->{});result.setBaseValue(actual.getBaseValue());
        for(var modifier:actual.getModifiers())result.addTransientModifier(modifier);
        for(var stack:p.getArmorSlots())if(ironsArmor(stack)){
            var item=(ArmorItem)stack.getItem();
            for(var modifier:stack.getAttributeModifiers(item.getEquipmentSlot()).get(attribute)){
                var active=result.getModifier(modifier.getId());
                if(active!=null){result.removeModifier(active);result.addTransientModifier(weighted(active));}
            }
        }return result.getValue();
    }
    public static float afterArmor(Player p,DamageSource source,float damage){
        boolean magic=magical(source);
        if(source.is(DamageTypeTags.BYPASSES_EFFECTS)||!magic&&source.is(DamageTypeTags.BYPASSES_ARMOR))return damage;
        return CombatRules.getDamageAfterAbsorb(damage,(float)Math.floor(armorAttribute(p,Attributes.ARMOR,magic)),(float)armorAttribute(p,Attributes.ARMOR_TOUGHNESS,magic));
    }
    public static int protection(Iterable<ItemStack> stacks,DamageSource source){
        if(source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)||source.is(DamageTypeTags.BYPASSES_EFFECTS))return 0;
        if(!magical(source))return EnchantmentHelper.getDamageProtection(stacks,source);
        var allowed=new ArrayList<ItemStack>();for(var stack:stacks)if(ironsArmor(stack))allowed.add(stack);
        return EnchantmentHelper.getDamageProtection(allowed,source);
    }
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
        float damage=(float)(REFERENCE_DAMAGE*(type==DefenseType.MAGICAL&&nativeSpell(source)?spellRemaining(spellResistance(p)):1));
        damage=afterArmor(p,source,damage);
        damage=CombatRules.getDamageAfterMagicAbsorb(damage,protection(p.getArmorSlots(),source));
        return Math.min(100,(1-damage/REFERENCE_DAMAGE)*100);
    }
    public static double combine(double first,double second){return 100*(1-(1-first/100)*(1-second/100));}
    private EquipmentDefense(){}
}
