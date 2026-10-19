package dev.rpgprogression.client;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.requirements.ItemCombatStats;
import dev.rpgprogression.requirements.ItemRequirements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

/** Add unmet requirements beneath the name while retaining native stats and action costs. */
@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class RequirementTooltip {
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tooltip(ItemTooltipEvent event){
        var stack=event.getItemStack();if(stack.isEmpty()||!RPGConfig.SERVER.isLoaded())return;
        var player=event.getEntity()!=null?event.getEntity():Minecraft.getInstance().player;
        var lines=event.getToolTip();var missing=ItemRequirements.unmet(player,stack);
        if(!missing.isEmpty()){
            var requirement=Component.translatable("rpgprogression.requirements.title").withStyle(ChatFormatting.GOLD).append(" ");
            boolean first=true;
            for(var entry:missing.entrySet()){
                if(!first)requirement.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));first=false;
                requirement.append(Component.translatable("rpgprogression.requirements.entry",Component.translatable(entry.getKey().key()),entry.getValue()).withStyle(ChatFormatting.RED));
            }
            lines.add(Math.min(1,lines.size()),requirement);
        }
        if(!ItemCombatStats.weapon(stack))return;
        var costs=new ArrayList<Component>();
        if(stack.getItem() instanceof CrossbowItem)costs.add(cost("rpgprogression.item.stamina.charge",ItemCombatStats.rangedCost(player,stack)));
        else if(stack.getItem() instanceof BowItem)costs.add(cost("rpgprogression.item.stamina.shot",ItemCombatStats.rangedCost(player,stack)));
        else {
            costs.add(cost("rpgprogression.item.stamina.hit",ItemCombatStats.attackCost(player,stack)));
            if(stack.getItem() instanceof TridentItem)costs.add(cost("rpgprogression.item.stamina.throw",ItemCombatStats.rangedCost(player,stack)));
        }
        int afterStats=-1;
        for(int i=0;i<lines.size();i++)if(hasKey(lines.get(i),"attribute.name.generic.attack_damage")||hasKey(lines.get(i),"attribute.name.generic.attack_speed")||hasKey(lines.get(i),"attribute.name.attack_damage")||hasKey(lines.get(i),"attribute.name.attack_speed"))afterStats=i+1;
        if(afterStats<0){afterStats=lines.size();costs.add(0,Component.empty());}
        lines.addAll(afterStats,costs);
    }
    private static Component cost(String key,double value){
        String amount=value>0&&value<.01?"<0.01":BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return Component.translatable(key,amount).withStyle(ChatFormatting.DARK_GREEN);
    }
    private static boolean hasKey(Component component,String key){
        if(component.getContents() instanceof TranslatableContents text){
            if(text.getKey().equals(key))return true;
            for(Object argument:text.getArgs())if(argument instanceof Component child&&hasKey(child,key))return true;
        }
        return component.getSiblings().stream().anyMatch(c->hasKey(c,key));
    }
    private RequirementTooltip(){}
}
