package dev.rpgprogression.verification.client;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.verification.SkintTravelChecks;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.function.Consumer;

public final class IronsRequirementsVisuals {
    private static ItemStack item(String path){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks",path)));}
    private static RequirementsVisualChecks.Panel panel(Minecraft mc,ItemStack stack,String label){return new RequirementsVisualChecks.Panel(label,List.copyOf(stack.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.Default.NORMAL)),stack);}
    public static void add(Minecraft mc,JsonArray checks,Consumer<RequirementsVisualChecks.Page> add){
        var d=ClientData.get();var book=item("netherite_spell_book");ISpellContainer.set(book,ISpellContainer.create(12,true,true));
        d.set(RPGAttribute.INTELLIGENCE,24);d.set(RPGAttribute.SPARK_POTENTIAL,19);var both=panel(mc,book,"Интеллект 24, потенциал 19");
        SkintTravelChecks.check(checks,both.tooltip().get(2).getString().contains("Интеллект")&&both.tooltip().get(3).getString().contains("Потенциал"),"the two actual book requirements occupy separate tooltip rows");
        d.set(RPGAttribute.INTELLIGENCE,25);d.set(RPGAttribute.SPARK_POTENTIAL,20);var free=panel(mc,book,"Интеллект 25, потенциал 20");
        SkintTravelChecks.check(checks,free.tooltip().stream().noneMatch(c->c.getString().equals("Требуется:")),"the entire native book requirement section disappears when fulfilled");
        add.accept(new RequirementsVisualChecks.Page("10-book-column.png",both,free));
        var spell=SpellRegistry.FIREBOLT_SPELL.get();var scroll=item("scroll");ISpellContainer.createScrollContainer(spell,1,scroll);
        int minimum=dev.rpgprogression.compat.IronsRequirements.spellMinimum(spell,1);d.set(RPGAttribute.INTELLIGENCE,Math.max(0,minimum-1));var locked=panel(mc,scroll,"Интеллект "+(minimum-1));
        d.set(RPGAttribute.INTELLIGENCE,minimum);var unlocked=panel(mc,scroll,"Интеллект "+minimum);
        SkintTravelChecks.check(checks,locked.tooltip().get(2).getString().equals("Интеллект "+minimum)&&unlocked.tooltip().stream().noneMatch(c->c.getString().equals("Требуется:")),"a real scroll tooltip updates at exactly five Intelligence per mana");
        add.accept(new RequirementsVisualChecks.Page("11-scroll-column.png",locked,unlocked));
        d.set(RPGAttribute.INTELLIGENCE,0);
        var tip=io.redspace.ironsspellbooks.util.TooltipsUtils.formatActiveSpellTooltip(book,new SpellData(spell,1),CastSource.SPELLBOOK,mc.player);
        SkintTravelChecks.check(checks,tip.size()>2&&tip.get(1).getString().equals("Требуется:")&&tip.get(2).getString().startsWith("Интеллект "),"the native selected-book-spell tooltip also shows its Intelligence requirement vertically");
    }
    private IronsRequirementsVisuals(){}
}
