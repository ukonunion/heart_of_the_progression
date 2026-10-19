package dev.rpgprogression.requirements;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.PlayerProgressionProvider;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
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
        // Vanilla material tiers are also used by Epic Fight and other tiered tools/weapons.
        if(stack.getItem() instanceof TieredItem tool){
            var tier=tool.getTier();int minimum=tier==Tiers.IRON?15:tier==Tiers.DIAMOND?20:tier==Tiers.NETHERITE?25:0;
            if(minimum>0)result.put(RPGAttribute.ENDURANCE,minimum);
        }
        if(stack.is(Items.BREWING_STAND)||stack.is(Items.ENCHANTING_TABLE))result.put(RPGAttribute.INTELLIGENCE,15);
        String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for(var attribute:RPGAttribute.values()){
            var rules=rules(attribute);
            // Explicit item entries replace the built-in default; item=0 disables that default.
            // Multiple matching user rules (including tags) still use their highest minimum.
            if(rules.stream().anyMatch(r->!r.tag()&&r.id().equals(item)))result.remove(attribute);
            for(var rule:rules){
                boolean matches=rule.tag()?stack.is(TagKey.create(Registries.ITEM,ResourceLocation.parse(rule.id()))):rule.id().equals(item);
                if(matches&&rule.minimum()>0)result.merge(attribute,rule.minimum(),Math::max);
            }
        }
        return result;
    }
    /** The tooltip and server authorization use the same per-attribute comparison. */
    public static Map<RPGAttribute,Integer> unmet(Player player,ItemStack stack){
        var missing=required(stack);
        if(player!=null)PlayerProgressionProvider.get(player).ifPresent(d->missing.entrySet().removeIf(e->d.get(e.getKey())>=e.getValue()));
        return missing;
    }
    public static boolean allowed(Player player,ItemStack stack){
        if(player.isCreative()||player.isSpectator()||stack.isEmpty())return true;
        return unmet(player,stack).isEmpty();
    }
    private ItemRequirements(){}
}
