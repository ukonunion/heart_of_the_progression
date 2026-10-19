package dev.rpgprogression.client.gui;

import dev.rpgprogression.config.RPGConfig;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;
import java.util.*;

/** Loaded only on the client with Epic Fight present; addon descriptions use its native API. */
final class EpicSkillText {
    static Component nativeDescription(String id){
        var skill=SkillManager.getSkill(id);
        String key="skill."+id.replace(':','.')+".tooltip";
        return skill!=null&&I18n.exists(key)?Component.translatable(key,skill.getTooltipArgsOfScreen(new ArrayList<>()).toArray()):Component.translatable("rpgprogression.skills.description.unavailable");
    }
    static List<Component> bonuses(String id){
        var skill=SkillManager.getSkill(id);if(skill==null)return List.of();
        var result=new ArrayList<Component>();
        for(var entry:skill.getModfierEntry()){
            var attribute=entry.getKey();var modifier=entry.getValue();
            double amount=modifier.getAmount();boolean percent=modifier.getOperation()!=AttributeModifier.Operation.ADDITION;
            Component label;
            if(attribute==EpicFightAttributes.MAX_STAMINA.get()){
                label=Component.translatable("rpgprogression.stat.stamina");if(!percent)amount*=RPGConfig.EPICFIGHT_UNIT.get();
            }else if(attribute==EpicFightAttributes.STAMINA_REGEN.get()){
                label=Component.translatable("rpgprogression.stat.stamina_regen");
                if(!percent)amount/=attribute.getDefaultValue();percent=true;
            }else label=Component.translatable(attribute.getDescriptionId());
            if(percent)amount*=100;
            String value=String.format(Locale.ROOT,"%+.1f%s",amount,percent?"%":"").replace(".0","");
            result.add(Component.translatable("rpgprogression.skills.bonus",label,value));
        }
        return result;
    }
    private EpicSkillText(){}
}
