package dev.rpgprogression.requirements;

import dev.rpgprogression.attribute.RPGAttribute;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.*;
import java.util.function.Predicate;

/** A matching configured rule replaces that attribute's automatic requirement, including zero. */
public final class ConfiguredRequirements {
    private final Map<RPGAttribute,List<? extends String>> snapshots=new EnumMap<>(RPGAttribute.class);
    private final Map<RPGAttribute,List<RequirementRule>> parsed=new EnumMap<>(RPGAttribute.class);
    private synchronized List<RequirementRule> rules(RPGAttribute attribute,List<? extends String> source){
        if(!source.equals(snapshots.get(attribute))){snapshots.put(attribute,List.copyOf(source));parsed.put(attribute,source.stream().map(RequirementRule::parse).toList());}
        return parsed.get(attribute);
    }
    public void apply(Map<RPGAttribute,ModConfigSpec.ConfigValue<List<? extends String>>> config,String id,Predicate<String> tagMatches,Map<RPGAttribute,Integer> result){
        for(var attribute:RPGAttribute.values()){
            int minimum=-1;
            for(var rule:rules(attribute,config.get(attribute).get()))if(rule.tag()?tagMatches.test(rule.id()):rule.id().equals(id))minimum=Math.max(minimum,rule.minimum());
            if(minimum>=0){result.remove(attribute);if(minimum>0)result.put(attribute,minimum);}
        }
    }
}
