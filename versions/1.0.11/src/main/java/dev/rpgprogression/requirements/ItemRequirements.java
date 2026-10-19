package dev.rpgprogression.requirements;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.PlayerProgressionProvider;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class ItemRequirements {
    private static final Map<RPGAttribute,List<? extends String>> snapshots=new EnumMap<>(RPGAttribute.class);
    private static final Map<RPGAttribute,List<RequirementRule>> parsed=new EnumMap<>(RPGAttribute.class);
    private static synchronized List<RequirementRule> rules(RPGAttribute attribute){
        var source=RPGConfig.ITEM_REQUIREMENTS.get(attribute).get();
        if(!source.equals(snapshots.get(attribute))){snapshots.put(attribute,List.copyOf(source));parsed.put(attribute,source.stream().map(RequirementRule::parse).toList());}
        return parsed.get(attribute);
    }
    public static Map<RPGAttribute,Integer> required(ItemStack stack){
        var result=new EnumMap<RPGAttribute,Integer>(RPGAttribute.class);if(stack.isEmpty())return result;
        String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for(var attribute:RPGAttribute.values())for(var rule:rules(attribute)){
            boolean matches=rule.tag()?stack.is(TagKey.create(Registries.ITEM,new ResourceLocation(rule.id()))):rule.id().equals(item);
            if(matches&&rule.minimum()>0)result.merge(attribute,rule.minimum(),Math::max);
        }
        return result;
    }
    public static boolean allowed(Player player,ItemStack stack){
        if(player.isCreative()||player.isSpectator()||stack.isEmpty())return true;
        var requirements=required(stack);if(requirements.isEmpty())return true;
        return player.getCapability(PlayerProgressionProvider.CAPABILITY).map(d->requirements.entrySet().stream().allMatch(e->d.get(e.getKey())>=e.getValue())).orElse(false);
    }
    private ItemRequirements(){}
}
