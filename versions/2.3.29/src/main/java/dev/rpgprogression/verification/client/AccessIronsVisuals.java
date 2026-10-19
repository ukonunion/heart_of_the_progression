package dev.rpgprogression.verification.client;
import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.verification.AccessRulesChecks;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;
public final class AccessIronsVisuals {
    public interface AddPage{void add(AccessVisualChecks.Panel left,AccessVisualChecks.Panel right,String file);}
    public static void add(Minecraft mc,JsonArray checks,AddPage add){
        var representatives=new TreeMap<Integer,SpellData>();for(var spell:SpellRegistry.REGISTRY.stream().toList())if(!spell.equals(SpellRegistry.none()))for(int level:new int[]{spell.getMinLevel(),spell.getMaxLevel()})representatives.putIfAbsent(spell.getManaCost(level),new SpellData(spell,level));
        var panels=new ArrayList<AccessVisualChecks.Panel>();for(int units=1;units<=8;units++){
            var data=representatives.get(units);if(data==null)throw new IllegalStateException("Missing mana representative "+units);var scroll=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("irons_spellbooks:scroll")));ISpellContainer.createScrollContainer(data.getSpell(),data.getLevel(),scroll);int minimum=10+5*units;ClientData.get().set(RPGAttribute.INTELLIGENCE,minimum-1);
            var lines=scroll.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.Default.NORMAL);String expected=Component.translatable(RPGAttribute.INTELLIGENCE.key()).getString()+" "+minimum;AccessRulesChecks.check(checks,lines.size()>2&&lines.get(2).getString().equals(expected),"the real "+units+"-mana scroll tooltip requires Intelligence "+minimum);panels.add(new AccessVisualChecks.Panel("Мана "+units+" → Интеллект "+minimum,scroll,List.copyOf(lines)));
        }
        for(int i=0;i<panels.size();i+=2)add.add(panels.get(i),panels.get(i+1),String.format("access-%02d-scrolls.png",6+i/2));
    }
    private AccessIronsVisuals(){}
}
