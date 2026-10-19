package dev.rpgprogression.client.gui;

import dev.rpgprogression.config.RPGConfig;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import java.util.*;

/** Loaded only on the client with Epic Fight present; addon descriptions use its native API. */
final class EpicSkillText {
    static Component nativeDescription(String id){
        var skill=EpicFightRegistries.SKILL.get(ResourceLocation.parse(id));
        String key="skill."+id.replace(':','.')+".tooltip";
        return skill!=null&&I18n.exists(key)?Component.translatable(key,skill.getTooltipArgsOfScreen(new ArrayList<>()).toArray()):Component.translatable("rpgprogression.skills.description.unavailable");
    }
    static List<Component> bonuses(String id){
        var skill=EpicFightRegistries.SKILL.get(ResourceLocation.parse(id));if(skill==null)return List.of();
        var result=new ArrayList<Component>();
        for(var entry:skill.getModfierEntry()){
            var attribute=entry.getKey();var modifier=entry.getValue();
            double amount=modifier.amount();boolean percent=modifier.operation()!=AttributeModifier.Operation.ADD_VALUE;
            Component label;
            if(attribute.is(EpicFightAttributes.MAX_STAMINA.getKey())){
                label=Component.translatable("rpgprogression.stat.stamina");if(!percent)amount*=RPGConfig.EPICFIGHT_UNIT.get();
            }else if(attribute.is(EpicFightAttributes.STAMINA_REGEN.getKey())){
                label=Component.translatable("rpgprogression.stat.stamina_regen");
                if(!percent)amount/=attribute.value().getDefaultValue();percent=true;
            }else label=Component.translatable(attribute.value().getDescriptionId());
            if(percent)amount*=100;
            String value=String.format(Locale.ROOT,"%+.1f%s",amount,percent?"%":"").replace(".0","");
            result.add(Component.translatable("rpgprogression.skills.bonus",label,value));
        }
        return result;
    }
    private EpicSkillText(){}
}
