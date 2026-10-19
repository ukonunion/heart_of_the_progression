package dev.rpgprogression.requirements;

import dev.rpgprogression.attribute.RPGAttribute;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import java.util.*;

/** Shared vertical layout for item and spell tooltips. Fulfilled requirements are omitted by the caller. */
public final class RequirementText {
    public static List<MutableComponent> lines(Map<RPGAttribute,Integer> missing){
        var lines=new ArrayList<MutableComponent>();
        if(!missing.isEmpty()){
            lines.add(Component.translatable("rpgprogression.requirements.title").withStyle(ChatFormatting.GOLD));
            missing.forEach((attribute,minimum)->lines.add(Component.translatable("rpgprogression.requirements.entry",Component.translatable(attribute.key()),minimum).withStyle(ChatFormatting.RED)));
        }
        return lines;
    }
    private RequirementText(){}
}
