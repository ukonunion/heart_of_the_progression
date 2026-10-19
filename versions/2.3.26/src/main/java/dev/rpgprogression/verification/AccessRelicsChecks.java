package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.*;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.requirements.ItemRequirements;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
import java.util.*;

public final class AccessRelicsChecks {
    public static void run(ServerPlayer p,JsonArray checks)throws Exception{
        var d=ProgressionManager.get(p);ItemStack first=ItemStack.EMPTY;int artifacts=0,materials=0;
        for(var entry:BuiltInRegistries.ITEM.entrySet())if(entry.getKey().location().getNamespace().equals("relics")){
            var stack=new ItemStack(entry.getValue());String id=entry.getKey().location().toString();
            if(RelicsRequirements.artifact(stack)){artifacts++;if(first.isEmpty())first=stack;AccessRulesChecks.check(checks,ItemRequirements.required(stack).equals(Map.of(RPGAttribute.INTELLIGENCE,15)),id+" is an actual Relics artifact and requires exactly Intelligence fifteen");d.set(RPGAttribute.INTELLIGENCE,14);AccessRulesChecks.check(checks,!ItemRequirements.allowed(p,stack),id+" is unusable at Intelligence fourteen");d.set(RPGAttribute.INTELLIGENCE,15);AccessRulesChecks.check(checks,ItemRequirements.allowed(p,stack),id+" is usable at Intelligence fifteen");}
            else{materials++;AccessRulesChecks.check(checks,!ItemRequirements.required(stack).containsKey(RPGAttribute.INTELLIGENCE),id+" is not an artifact and does not receive the Relics artifact requirement");}
        }
        AccessRulesChecks.check(checks,artifacts>20,"checked every installed Relics item: "+artifacts+" artifacts and "+materials+" other items");d.set(RPGAttribute.INTELLIGENCE,14);var context=new SlotContext("charm",p,0,false,true);var denied=new CurioCanEquipEvent(first,context);NeoForge.EVENT_BUS.post(denied);AccessRulesChecks.check(checks,denied.getEquipResult()==TriState.FALSE,"the actual Curios equip event rejects Relics below Intelligence fifteen");d.set(RPGAttribute.INTELLIGENCE,15);var allowed=new CurioCanEquipEvent(first,context);NeoForge.EVENT_BUS.post(allowed);AccessRulesChecks.check(checks,allowed.getEquipResult()!=TriState.FALSE,"Curios requirement guards allow Relics at Intelligence fifteen");
        var curios=CuriosApi.getCuriosInventory(p).orElseThrow();var slots=curios.getCurios().values().stream().map(v->v.getStacks()).filter(s->s.getSlots()>0).findFirst().orElseThrow();var old=slots.getStackInSlot(0).copy();try{slots.setStackInSlot(0,first.copy());d.set(RPGAttribute.INTELLIGENCE,14);CurioRequirementGuards.validate(p);AccessRulesChecks.check(checks,slots.getStackInSlot(0).isEmpty(),"a forcibly equipped Relics artifact is removed below Intelligence fifteen, including without Iron's");}finally{slots.setStackInSlot(0,old);}
        var type=Class.forName("it.hurts.sskirillss.relics.items.relics.base.IRelicItem");var method=type.getMethod("canPlayerUseAbility",Player.class,ItemStack.class,String.class);AccessRulesChecks.check(checks,Boolean.FALSE.equals(method.invoke(first.getItem(),p,first,"rpg_requirement_probe")),"the native Relics ability entry point rejects a direct ability request below Intelligence fifteen");
    }
    private AccessRelicsChecks(){}
}
