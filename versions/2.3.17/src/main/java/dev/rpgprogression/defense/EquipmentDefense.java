package dev.rpgprogression.defense;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import dev.rpgprogression.progression.PlayerProgression;
import dev.rpgprogression.progression.PlayerProgressionProvider;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.*;

/** Shared armor policy for actual damage and the menu. Optional mods are identified by registry ID. */
public final class EquipmentDefense {
    public static final float REFERENCE_DAMAGE=10;
    public static final double IRONS_ARMOR_WEIGHT=.5;
    private static final TagKey<DamageType> MAGIC=TagKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("rpgprogression","is_magic"));
    public static boolean ironsArmor(ItemStack stack){return stack.getItem() instanceof ArmorItem&&BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("irons_spellbooks");}
    public static boolean magical(DamageSource source){return source.is(MAGIC)||nativeSpell(source);}
    public static double armorPercent(double armor,double toughness){return (1-armorOnly(REFERENCE_DAMAGE,(float)Math.floor(armor),(float)toughness)/REFERENCE_DAMAGE)*100;}
    /** Registry reports have no attacker; use vanilla's armor curve without weapon enchantments. */
    private static float armorOnly(float damage,float armor,float toughness){float scale=2+toughness/4;float effective=net.minecraft.util.Mth.clamp(armor-damage/scale,armor*.2f,20);return damage*(1-effective/25);}
    public static DamageSource magicSource(Player p){return p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("irons_spellbooks","fire_magic"))).map(h->new DamageSource(h)).orElseGet(()->p.damageSources().magic());}
    public static boolean nativeSpell(DamageSource source){return source.typeHolder().unwrapKey().map(k->k.location().getNamespace().equals("irons_spellbooks")).orElse(false);}
    private static AttributeModifier weighted(AttributeModifier modifier){return new AttributeModifier(modifier.id(),modifier.amount()*IRONS_ARMOR_WEIGHT,modifier.operation());}
    /** Equipment-only calculation also used by registry reports. Ordinary armor has no magic contribution. */
    public static double armorAttribute(Iterable<ItemStack> stacks,Holder<Attribute> attribute,boolean magic){
        var result=new AttributeInstance(attribute,x->{});result.setBaseValue(0);
        for(var stack:stacks){if(!(stack.getItem() instanceof ArmorItem item)||magic&&!ironsArmor(stack))continue;
            stack.forEachModifier(item.getEquipmentSlot(),(entry,modifier)->{
                if(!entry.equals(attribute))return;
                if(result.getModifier(modifier.id())!=null)result.removeModifier(modifier.id());
                result.addTransientModifier(ironsArmor(stack)?weighted(modifier):modifier);
            });
        }return result.getValue();
    }
    public static double armorAttribute(Player p,Holder<Attribute> attribute,boolean magic){
        if(magic)return armorAttribute(p.getArmorSlots(),attribute,true);
        // Retain base armor and unrelated native modifiers; change only Iron's equipped armor.
        var actual=p.getAttribute(attribute);if(actual==null)return 0;
        var result=new AttributeInstance(attribute,x->{});result.setBaseValue(actual.getBaseValue());
        for(var modifier:actual.getModifiers())result.addTransientModifier(modifier);
        for(var stack:p.getArmorSlots())if(ironsArmor(stack)){
            var item=(ArmorItem)stack.getItem();
            stack.forEachModifier(item.getEquipmentSlot(),(entry,modifier)->{
                if(!entry.equals(attribute))return;
                var active=result.getModifier(modifier.id());
                if(active!=null){result.removeModifier(active);result.addTransientModifier(weighted(active));}
            });
        }return result.getValue();
    }
    public static float afterArmor(Player p,DamageSource source,float damage){
        boolean magic=magical(source);
        if(source.is(DamageTypeTags.BYPASSES_EFFECTS)||!magic&&source.is(DamageTypeTags.BYPASSES_ARMOR))return damage;
        return CombatRules.getDamageAfterAbsorb(p,damage,source,(float)Math.floor(armorAttribute(p,Attributes.ARMOR,magic)),(float)armorAttribute(p,Attributes.ARMOR_TOUGHNESS,magic));
    }
    public static float protection(ServerPlayer player,DamageSource source){
        if(source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)||source.is(DamageTypeTags.BYPASSES_EFFECTS))return 0;
        // EnchantmentArmorMixin restricts the native equipment iteration for magic damage.
        return EnchantmentHelper.getDamageProtection(player.serverLevel(),player,source);
    }
    public static double spellPercent(double armor,double toughness,double resistance,boolean armorApplies){
        float damage=(float)(REFERENCE_DAMAGE*spellRemaining(resistance));
        if(armorApplies)damage=armorOnly(damage,(float)Math.floor(armor),(float)toughness);
        return (1-damage/REFERENCE_DAMAGE)*100;
    }
    public static double spellRemaining(double resistance){return 2-(resistance<=1.5?resistance:2-.25/(resistance-1));}
    public static double spellResistance(Player p){
        var attribute=BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath("irons_spellbooks","spell_resist"));
        return attribute.isPresent()&&p.getAttribute(attribute.get())!=null?p.getAttributeValue(attribute.get()):1;
    }
    public static double percent(Player p,DefenseType type){
        if(p==null||type!=DefenseType.PHYSICAL&&type!=DefenseType.MAGICAL)return 0;
        if(p instanceof ServerPlayer server)return serverPercent(server,type);
        return PlayerProgressionProvider.get(p).map(d->type==DefenseType.PHYSICAL?d.equipmentPhysicalPercent:d.equipmentMagicalPercent).orElse(0.0);
    }
    public static boolean updateCache(ServerPlayer player,PlayerProgression data){
        double physical=serverPercent(player,DefenseType.PHYSICAL),magical=serverPercent(player,DefenseType.MAGICAL);
        boolean changed=Math.abs(data.equipmentPhysicalPercent-physical)>1e-8||Math.abs(data.equipmentMagicalPercent-magical)>1e-8;
        data.equipmentPhysicalPercent=physical;data.equipmentMagicalPercent=magical;
        return changed;
    }
    public static double serverPercent(ServerPlayer p,DefenseType type){
        if(type!=DefenseType.PHYSICAL&&type!=DefenseType.MAGICAL)return 0;
        // A defense report has no attacker: the player's held Breach weapon must not lower their own armor rating.
        var source=type==DefenseType.PHYSICAL?new DamageSource(p.damageSources().mobAttack(p).typeHolder()):magicSource(p);
        float damage=(float)(REFERENCE_DAMAGE*(type==DefenseType.MAGICAL&&nativeSpell(source)?spellRemaining(spellResistance(p)):1));
        damage=afterArmor(p,source,damage);
        damage=CombatRules.getDamageAfterMagicAbsorb(damage,protection(p,source));
        return Math.min(100,(1-damage/REFERENCE_DAMAGE)*100);
    }
    public static double combine(double first,double second){return 100*(1-(1-first/100)*(1-second/100));}
    private EquipmentDefense(){}
}
