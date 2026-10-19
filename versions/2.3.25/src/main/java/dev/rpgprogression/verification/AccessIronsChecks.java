package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.requirements.ItemRequirements;
import dev.rpgprogression.resource.ResourceMath;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import java.util.*;

public final class AccessIronsChecks {
    public static void run(ServerPlayer p,JsonArray checks){
        int base=RPGConfig.SPELL_BASE_INTELLIGENCE.get(),per=RPGConfig.SPELL_INTELLIGENCE_PER_MANA.get();var rules=new EnumMap<RPGAttribute,List<? extends String>>(RPGAttribute.class);RPGConfig.SPELL_REQUIREMENTS.forEach((a,c)->rules.put(a,List.copyOf(c.get())));var d=ProgressionManager.get(p);
        try{
            RPGConfig.SPELL_BASE_INTELLIGENCE.set(10);RPGConfig.SPELL_INTELLIGENCE_PER_MANA.set(5);RPGConfig.SPELL_REQUIREMENTS.values().forEach(c->c.set(List.of()));var unitsFound=new TreeSet<Integer>();int count=0;
            for(var spell:SpellRegistry.REGISTRY.stream().toList())if(!spell.equals(SpellRegistry.none()))for(int level:new int[]{spell.getMinLevel(),spell.getMaxLevel()}){
                int units=IronsCompat.enabled()?spell.getManaCost(level):ResourceMath.spellCost(spell.getManaCost(level));int expected=units<=0?0:10+Math.min(8,units)*5;unitsFound.add(units);count++;
                var scroll=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("irons_spellbooks:scroll")));ISpellContainer.createScrollContainer(spell,level,scroll);
                AccessRulesChecks.check(checks,IronsRequirements.spellMinimum(spell,level)==expected&&ItemRequirements.required(scroll).equals(expected==0?Map.of():Map.of(RPGAttribute.INTELLIGENCE,expected)),spell.getSpellId()+" level "+level+" uses "+units+" mana and its actual scroll requires Intelligence "+expected);
                if(expected>0){d.set(RPGAttribute.INTELLIGENCE,expected-1);double mana=d.mana;AccessRulesChecks.check(checks,!ItemRequirements.allowed(p,scroll)&&!IronsRequirements.spellAllowed(p,spell,level,CastSource.SCROLL)&&!spell.attemptInitiateCast(scroll,level,p.level(),p,CastSource.SCROLL,true,"mainhand")&&d.mana==mana&&scroll.getCount()==1,"the actual scroll cast is rejected one Intelligence below its threshold without consuming scroll or mana: "+spell.getSpellId());}
                d.set(RPGAttribute.INTELLIGENCE,expected);AccessRulesChecks.check(checks,ItemRequirements.allowed(p,scroll)&&IronsRequirements.spellAllowed(p,spell,level,CastSource.SCROLL)&&IronsRequirements.spellAllowed(p,spell,level,CastSource.SPELLBOOK),"exact Intelligence threshold satisfies the shared scroll/book spell rule: "+spell.getSpellId());
            }
            AccessRulesChecks.check(checks,unitsFound.containsAll(Set.of(1,2,3,4,5,6,7,8)),"actual installed spells cover every converted mana unit 1..8: "+unitsFound+" in "+count+" cases");
            var spell=SpellRegistry.FIREBALL_SPELL.get();String id=spell.getSpellId().toString();RPGConfig.SPELL_REQUIREMENTS.get(RPGAttribute.INTELLIGENCE).set(List.of(id+"=18"));RPGConfig.SPELL_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(List.of(id+"=12"));AccessRulesChecks.check(checks,IronsRequirements.spellRequired(spell,1).equals(Map.of(RPGAttribute.INTELLIGENCE,18,RPGAttribute.STRENGTH,12)),"existing per-spell overrides still replace the new formula and add attributes");RPGConfig.SPELL_REQUIREMENTS.values().forEach(c->c.set(List.of()));RPGConfig.SPELL_INTELLIGENCE_PER_MANA.set(0);AccessRulesChecks.check(checks,IronsRequirements.spellRequired(spell,1).isEmpty(),"a zero per-mana multiplier still disables the complete automatic spell requirement");
        }finally{RPGConfig.SPELL_BASE_INTELLIGENCE.set(base);RPGConfig.SPELL_INTELLIGENCE_PER_MANA.set(per);rules.forEach((a,r)->RPGConfig.SPELL_REQUIREMENTS.get(a).set(r));}
    }
    private AccessIronsChecks(){}
}
