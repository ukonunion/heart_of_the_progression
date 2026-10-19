package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.EpicAttackAccess;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.requirements.ItemRequirements;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import java.util.*;

public final class EquipmentEpicChecks {
    private static void check(JsonArray checks,boolean ok,String text){SkintTravelChecks.check(checks,ok,text);if(!ok)throw new IllegalStateException(text);}
    public static void run(ServerPlayer p,JsonArray checks){
        var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);if(patch==null)throw new IllegalStateException("Missing Epic Fight patch");var d=ProgressionManager.get(p);boolean battle=patch.isEpicFightMode();
        try{
            patch.toEpicFightMode(false);
            for(String kind:new String[]{"dagger","greatsword","spear"}){
                var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("epicfight:iron_"+kind)));p.setItemInHand(InteractionHand.MAIN_HAND,stack);var attribute=kind.equals("dagger")?RPGAttribute.AGILITY:RPGAttribute.STRENGTH;
                for(var a:RPGAttribute.values())d.set(a,100);d.set(RPGAttribute.ENDURANCE,0);d.set(RPGAttribute.INTELLIGENCE,13);d.set(attribute,14);d.stamina=d.maxStamina();d.exhausted=false;double before=d.stamina;
                patch.onStrike(yesman.epicfight.gameasset.Animations.SWORD_AUTO1.get(),InteractionHand.MAIN_HAND);check(checks,!((EpicAttackAccess)patch).rpg$attackAllowed()&&d.stamina==before,"native iron "+kind+" strike rejects the replaced attribute below fifteen without spending stamina");
                d.set(attribute,15);d.set(RPGAttribute.INTELLIGENCE,12);patch.onStrike(yesman.epicfight.gameasset.Animations.SWORD_AUTO1.get(),InteractionHand.MAIN_HAND);check(checks,!((EpicAttackAccess)patch).rpg$attackAllowed()&&d.stamina==before,"native iron "+kind+" strike independently checks Intelligence thirteen");
                d.set(RPGAttribute.INTELLIGENCE,13);patch.onStrike(yesman.epicfight.gameasset.Animations.SWORD_AUTO1.get(),InteractionHand.MAIN_HAND);check(checks,((EpicAttackAccess)patch).rpg$attackAllowed()&&d.stamina<before,"native iron "+kind+" strike succeeds at both exact thresholds with zero Endurance and retains its stamina cost");
            }
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.AGILITY).set(List.of("epicfight:iron_dagger=3"));RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.INTELLIGENCE).set(List.of("epicfight:iron_dagger=0"));RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.SPARK_POTENTIAL).set(List.of("epicfight:iron_dagger=9"));
            var dagger=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("epicfight:iron_dagger")));
            check(checks,ItemRequirements.required(dagger).equals(Map.of(RPGAttribute.AGILITY,3,RPGAttribute.SPARK_POTENTIAL,9)),"config overrides lower the new dagger requirement, remove Intelligence, and add another attribute");
            RPGConfig.ITEM_REQUIREMENTS.values().forEach(c->c.set(List.of()));
        }finally{if(battle)patch.toEpicFightMode(false);else patch.toVanillaMode(false);}
    }
    private EquipmentEpicChecks(){}
}
