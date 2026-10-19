package dev.rpgprogression.verification;

import com.google.gson.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.requirements.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.nio.file.*;
import java.util.*;

/** Real registry, inclusive authorization, overrides and forced armor checks. Excluded from JAR. */
public final class EquipmentBalanceChecks {
    private static void check(JsonArray checks,boolean ok,String text){SkintTravelChecks.check(checks,ok,text);if(!ok)throw new IllegalStateException(text);}
    public static void run(ServerPlayer p,JsonArray checks){
        var d=ProgressionManager.get(p);var saved=d.save();var mode=p.gameMode.getGameModeForPlayer();var held=p.getMainHandItem();var armor=p.getItemBySlot(EquipmentSlot.CHEST);boolean defaults=RPGConfig.DEFAULT_ITEM_REQUIREMENTS.get();
        var rules=new EnumMap<RPGAttribute,List<? extends String>>(RPGAttribute.class);RPGConfig.ITEM_REQUIREMENTS.forEach((a,c)->rules.put(a,List.copyOf(c.get())));
        try{
            p.setGameMode(GameType.SURVIVAL);d.initialize();RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(true);RPGConfig.ITEM_REQUIREMENTS.values().forEach(c->c.set(List.of()));
            var fixture=JsonParser.parseString(Files.readString(Path.of(System.getProperty("rpgprogression.equipmentFixture","../../src/test/resources/equipment-requirements-2.3.21.json")))).getAsJsonArray();
            for(var value:fixture){var row=value.getAsJsonObject();String name=row.get("id").getAsString();var id=ResourceLocation.parse(name);
                if(id.getNamespace().equals("epicfight")&&!net.neoforged.fml.ModList.get().isLoaded("epicfight"))continue;
                check(checks,BuiltInRegistries.ITEM.containsKey(id),name+" is present in the real item registry");var stack=new ItemStack(BuiltInRegistries.ITEM.get(id));
                var expected=new EnumMap<RPGAttribute,Integer>(RPGAttribute.class);row.getAsJsonObject("requirements").entrySet().forEach(e->expected.put(RPGAttribute.valueOf(e.getKey().toUpperCase(Locale.ROOT)),e.getValue().getAsInt()));
                check(checks,ItemRequirements.required(p,stack).equals(expected),name+" has exactly the requested attribute map "+expected);
                for(var a:RPGAttribute.values())d.set(a,expected.getOrDefault(a,0));
                check(checks,ItemRequirements.allowed(p,stack)&&ItemRequirements.unmet(p,stack).isEmpty(),name+" is usable at every exact threshold, without unrelated attributes");
                for(var e:expected.entrySet()){
                    d.set(e.getKey(),e.getValue()-1);check(checks,!ItemRequirements.allowed(p,stack)&&ItemRequirements.unmet(p,stack).equals(Map.of(e.getKey(),e.getValue())),name+" rejects one level below "+e.getKey()+" and hides the other fulfilled attributes");d.set(e.getKey(),e.getValue());
                }
            }
            for(var a:RPGAttribute.values())d.set(a,100);d.set(RPGAttribute.VITALITY,19);d.set(RPGAttribute.ENDURANCE,20);
            p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));RequirementEvents.tick(new PlayerTickEvent.Pre(p));check(checks,p.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),"forced diamond armor is removed when Vitality alone is below twenty");
            d.set(RPGAttribute.VITALITY,20);p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));RequirementEvents.tick(new PlayerTickEvent.Pre(p));check(checks,p.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),"diamond armor remains equipped at both exact thresholds");
            d.set(RPGAttribute.ENDURANCE,19);RequirementEvents.tick(new PlayerTickEvent.Pre(p));check(checks,p.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),"forced diamond armor is removed when Endurance alone is below twenty");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));d.set(RPGAttribute.AGILITY,14);var use=new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND);NeoForge.EVENT_BUS.post(use);check(checks,use.isCanceled(),"the real right-click event blocks bow use below Agility fifteen");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.MACE));d.set(RPGAttribute.STRENGTH,29);d.set(RPGAttribute.ENDURANCE,30);var attack=new AttackEntityEvent(p,EntityType.ZOMBIE.create(p.serverLevel()));NeoForge.EVENT_BUS.post(attack);check(checks,attack.isCanceled(),"the real attack event blocks a mace below Strength thirty");
            RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(false);RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(List.of("minecraft:mace=4"));
            check(checks,ItemRequirements.required(new ItemStack(Items.MACE)).equals(Map.of(RPGAttribute.STRENGTH,4))&&ItemRequirements.required(new ItemStack(Items.DIAMOND_CHESTPLATE)).isEmpty(),"disabling defaults removes the new equipment balance while retaining custom rules");
            RPGConfig.ITEM_REQUIREMENTS.values().forEach(c->c.set(List.of()));RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(true);
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.VITALITY).set(List.of("minecraft:diamond_chestplate=7"));RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.ENDURANCE).set(List.of("minecraft:diamond_chestplate=0"));
            check(checks,ItemRequirements.required(new ItemStack(Items.DIAMOND_CHESTPLATE)).equals(Map.of(RPGAttribute.VITALITY,7)),"existing config rules can lower Vitality and remove only the automatic Endurance requirement");
            RPGConfig.ITEM_REQUIREMENTS.values().forEach(c->c.set(List.of()));
            if(net.neoforged.fml.ModList.get().isLoaded("epicfight"))EquipmentEpicChecks.run(p,checks);
            p.setGameMode(GameType.CREATIVE);for(var a:RPGAttribute.values())d.set(a,0);check(checks,ItemRequirements.allowed(p,new ItemStack(Items.MACE)),"creative mode retains its established exemption");
        }catch(java.io.IOException e){throw new IllegalStateException(e);}finally{
            RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(defaults);rules.forEach((a,r)->RPGConfig.ITEM_REQUIREMENTS.get(a).set(r));p.setItemInHand(InteractionHand.MAIN_HAND,held);p.setItemSlot(EquipmentSlot.CHEST,armor);d.load(saved);p.setGameMode(mode);
        }
    }
    private EquipmentBalanceChecks(){}
}
