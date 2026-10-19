package dev.rpgprogression.compat;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Item policy has no Iron's class references and does not depend on the mana adapter setting. */
public final class DisabledIronsWeapons {
    private static final Map<Item,Boolean> cache=new ConcurrentHashMap<>();
    public static boolean blocked(ItemStack stack){return !stack.isEmpty()&&blocked(stack.getItem());}
    public static boolean blocked(Item item){
        var id=BuiltInRegistries.ITEM.getKey(item);
        if(id==null||!id.getNamespace().equals("irons_spellbooks"))return false;
        return cache.computeIfAbsent(item,DisabledIronsWeapons::weapon);
    }
    private static boolean weapon(Item item){
        // Lightning Rod and Artificer's Cane do not have "staff" in their registry ID.
        for(Class<?> type=item.getClass();type!=null;type=type.getSuperclass()){
            String name=type.getName();
            if(name.equals("io.redspace.ironsspellbooks.item.weapons.StaffItem")
                    ||name.equals("io.redspace.ironsspellbooks.item.weapons.StaffOfTheNines"))return false;
        }
        String name=item.getClass().getName();
        boolean[] damage={false};
        item.getDefaultInstance().forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)->{
            if(attribute.equals(Attributes.ATTACK_DAMAGE))damage[0]=true;
        });
        return name.startsWith("io.redspace.ironsspellbooks.item.weapons.")
                ||name.startsWith("io.redspace.ironsspellbooks.api.item.weapons.")
                ||item instanceof SwordItem||item instanceof BowItem||item instanceof CrossbowItem||item instanceof TridentItem
                ||damage[0];
    }
    public static boolean recipe(Recipe<?> recipe){
        // Static item outputs (including Iron's smithing recipes) use the item registry only.
        // A dynamic recipe may require a world's registries; its eventual output is still barred
        // by the inventory/use/loot gates, so never break an unrelated mod's resource reload.
        try{return blocked(recipe.getResultItem(RegistryAccess.EMPTY));}
        catch(RuntimeException dynamicOutput){return false;}
    }
    public static boolean recipe(net.minecraft.world.item.crafting.RecipeHolder<?> recipe){return recipe(recipe.value());}
    private DisabledIronsWeapons(){}
}
