package dev.rpgprogression.client;

import dev.rpgprogression.attribute.AttributeManager;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.PlayerProgressionProvider;
import dev.rpgprogression.requirements.ItemCombatStats;
import dev.rpgprogression.requirements.ItemRequirements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class RequirementTooltip {
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tooltip(ItemTooltipEvent event){
        var stack=event.getItemStack();if(stack.isEmpty()||!RPGConfig.SERVER.isLoaded())return;
        var player=event.getEntity()!=null?event.getEntity():Minecraft.getInstance().player;
        var data=player==null?ClientData.get():player.getCapability(PlayerProgressionProvider.CAPABILITY).orElse(ClientData.get());
        var lines=event.getToolTip();var details=new ArrayList<Component>();
        details.add(Component.empty());
        details.add(heading("rpgprogression.requirements.title"));
        var rules=ItemRequirements.required(stack);
        if(rules.isEmpty())details.add(gray("rpgprogression.requirements.none"));
        else rules.forEach((attribute,minimum)->details.add(Component.translatable("rpgprogression.item.attribute",
                Component.translatable(attribute.key()),Component.literal(Integer.toString(minimum)).withStyle(
                        !data.initialized?ChatFormatting.GRAY:data.get(attribute)>=minimum?ChatFormatting.GREEN:ChatFormatting.RED))));
        details.add(Component.empty());
        details.add(heading("rpgprogression.item.scaling.title"));
        boolean ranged=ItemCombatStats.ranged(stack),melee=!ranged&&ItemCombatStats.melee(stack);
        double strength=melee?RPGConfig.STRENGTH_DAMAGE.get():0,agility=ranged?RPGConfig.DRAW_FACTOR.get():0;
        var unusedScaling=new ArrayList<Component>();int scalingStart=details.size();
        if(strength==0&&agility==0)details.add(gray("rpgprogression.item.scaling.none"));
        else {
            for(var attribute:RPGAttribute.values()){
                Component value=gray("rpgprogression.item.scaling.absent");
                if(attribute==RPGAttribute.STRENGTH&&strength>0)value=Component.translatable("rpgprogression.item.scaling.damage",coefficient(strength));
                if(attribute==RPGAttribute.AGILITY&&agility>0)value=Component.translatable("rpgprogression.item.scaling.draw",coefficient(agility));
                var row=Component.translatable("rpgprogression.item.attribute",Component.translatable(attribute.key()),value);details.add(row);
                if(!(attribute==RPGAttribute.STRENGTH&&strength>0)&&!(attribute==RPGAttribute.AGILITY&&agility>0))unusedScaling.add(row);
            }
            details.add(gray("rpgprogression.item.scaling.baseline",RPGConfig.START.get()));
            if(data.initialized){
                if(strength>0)details.add(gray("rpgprogression.item.scaling.current_damage",number(AttributeManager.bonus(data,RPGAttribute.STRENGTH)*strength*100)));
                if(agility>0)details.add(gray("rpgprogression.item.scaling.current_draw",number((1/AttributeManager.drawRatio(player)-1)*100)));
            }
        }
        int costLines=ranged||melee?(stack.getItem() instanceof TridentItem?2:1):0;
        // Extra native stats/lore can make six scaling rows overflow at larger GUI scales.
        // Keep every active coefficient; group only the attributes which have no scaling.
        var mc=Minecraft.getInstance();
        if(!unusedScaling.isEmpty()&&(lines.size()+details.size()+costLines)*(mc.font.lineHeight+1)+2>mc.getWindow().getGuiScaledHeight()-14){
            details.removeAll(unusedScaling);
            details.add(scalingStart+RPGAttribute.values().length-unusedScaling.size(),gray("rpgprogression.item.scaling.other_absent"));
        }
        // Keep the item's own name, lore, enchantments, equipment stats and other mods' text.
        int beforeStats=lines.size();
        for(int i=1;i<lines.size();i++)if(keyStarts(lines.get(i),"item.modifiers.")){beforeStats=i;if(i>1&&lines.get(i-1).getString().isEmpty())beforeStats--;break;}
        lines.addAll(beforeStats,details);
        if(!ranged&&!melee)return;
        var costs=new ArrayList<Component>();
        if(ranged)costs.add(cost("rpgprogression.item.stamina.shot",ItemCombatStats.rangedCost(player,stack)));
        else {
            costs.add(cost("rpgprogression.item.stamina.hit",ItemCombatStats.attackCost(player,stack)));
            if(stack.getItem() instanceof TridentItem)costs.add(cost("rpgprogression.item.stamina.throw",ItemCombatStats.rangedCost(player,stack)));
        }
        int afterStats=-1;
        for(int i=0;i<lines.size();i++)if(hasKey(lines.get(i),"attribute.name.generic.attack_damage")||hasKey(lines.get(i),"attribute.name.generic.attack_speed"))afterStats=i+1;
        if(afterStats<0){afterStats=lines.size();costs.add(0,Component.empty());}
        lines.addAll(afterStats,costs);
    }
    private static Component heading(String key){return Component.translatable(key).withStyle(ChatFormatting.GOLD);}
    private static Component gray(String key,Object...args){return Component.translatable(key,args).withStyle(ChatFormatting.GRAY);}
    private static Component cost(String key,double value){return Component.translatable(key,number(value)).withStyle(ChatFormatting.DARK_GREEN);}
    private static String coefficient(double value){return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();}
    private static String number(double value){return value>0&&value<.01?"<0.01":BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
    private static boolean hasKey(Component component,String key){return match(component,key,false);}
    private static boolean keyStarts(Component component,String prefix){return match(component,prefix,true);}
    private static boolean match(Component component,String key,boolean prefix){
        if(component.getContents() instanceof TranslatableContents text){
            if(prefix?text.getKey().startsWith(key):text.getKey().equals(key))return true;
            for(Object argument:text.getArgs())if(argument instanceof Component child&&match(child,key,prefix))return true;
        }
        return component.getSiblings().stream().anyMatch(c->match(c,key,prefix));
    }
    private RequirementTooltip(){}
}
