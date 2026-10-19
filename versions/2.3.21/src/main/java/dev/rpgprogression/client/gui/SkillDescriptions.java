package dev.rpgprogression.client.gui;

import dev.rpgprogression.compat.EpicFightCompat;
import dev.rpgprogression.skill.SkillNode;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.*;

/** Explanations are independent of learning requirements and can be edited in the language files. */
public final class SkillDescriptions {
    public static Component name(SkillNode node){
        String key="rpgprogression.skills.name."+node.id().replace(':','.');
        if(!I18n.exists(key))return node.name();
        return node.id().equals("epicfight:vengeance")
                ?Component.translatable(key,Component.translatable(key+".hit").withStyle(ChatFormatting.BOLD))
                :Component.translatable(key);
    }
    public static List<Component> describe(SkillNode node){
        String key="rpgprogression.skills.description."+node.id().replace(':','.');
        var result=new ArrayList<Component>();
        if(I18n.exists(key))result.add(Component.translatable(key));
        else if(node.branch()==2&&EpicFightCompat.enabled())result.add(EpicSkillText.nativeDescription(node.id()));
        else result.add(Component.translatable("rpgprogression.skills.description.unavailable"));
        if(node.branch()==2&&EpicFightCompat.enabled())result.addAll(EpicSkillText.bonuses(node.id()));
        return result;
    }
    private SkillDescriptions(){}
}
