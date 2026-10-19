package dev.rpgprogression.requirements;

import dev.rpgprogression.attribute.RPGAttribute;
import java.util.*;

/** Automatic equipment balance; configured per-attribute rules are applied afterwards. */
public final class EquipmentRequirements {
    public static Map<RPGAttribute,Integer> defaultsFor(String id,int toolMinimum,int armorMinimum){
        var result=new EnumMap<RPGAttribute,Integer>(RPGAttribute.class);
        if(toolMinimum>0)result.put(RPGAttribute.ENDURANCE,toolMinimum);
        if(armorMinimum>0){result.put(RPGAttribute.ENDURANCE,armorMinimum);result.put(RPGAttribute.VITALITY,armorMinimum);}
        if(id.startsWith("epicfight:")&&toolMinimum>0){
            result.put(RPGAttribute.INTELLIGENCE,switch(toolMinimum){case 15->13;case 20->15;default->17;});
            if(id.endsWith("_dagger")){result.remove(RPGAttribute.ENDURANCE);result.put(RPGAttribute.AGILITY,toolMinimum);}
            else if(id.endsWith("_greatsword")||id.endsWith("_spear")){result.remove(RPGAttribute.ENDURANCE);result.put(RPGAttribute.STRENGTH,toolMinimum);}
            else if(id.endsWith("_tachi")||id.endsWith("_longsword")){
                boolean tachi=id.endsWith("_tachi");result.remove(RPGAttribute.ENDURANCE);
                int high=switch(toolMinimum){case 15->13;case 20->15;default->18;},low=switch(toolMinimum){case 15->12;case 20->15;default->17;};
                result.put(RPGAttribute.AGILITY,tachi?high:low);result.put(RPGAttribute.STRENGTH,tachi?low:high);
            }
        }
        if(id.equals("epicfight:uchigatana")){result.put(RPGAttribute.AGILITY,15);result.put(RPGAttribute.ENDURANCE,15);}
        if(id.startsWith("minecraft:")&&id.endsWith("_axe")&&toolMinimum>0)
            result.put(RPGAttribute.STRENGTH,switch(toolMinimum){case 15->13;case 20->15;default->17;});
        if(id.equals("minecraft:mace")){result.put(RPGAttribute.STRENGTH,30);result.put(RPGAttribute.ENDURANCE,30);}
        if(id.equals("minecraft:bow")||id.equals("minecraft:crossbow"))result.put(RPGAttribute.AGILITY,15);
        return result;
    }
    private EquipmentRequirements(){}
}
