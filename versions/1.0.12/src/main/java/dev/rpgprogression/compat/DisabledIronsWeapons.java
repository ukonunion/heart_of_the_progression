package dev.rpgprogression.compat;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Item policy has no Iron's class references and does not depend on the mana adapter setting. */
public final class DisabledIronsWeapons {
    private static final Map<Item,Boolean> cache=new ConcurrentHashMap<>();
    public static boolean blocked(ItemStack stack){return !stack.isEmpty()&&blocked(stack.getItem());}
    public static boolean blocked(Item item){
        var id=ForgeRegistries.ITEMS.getKey(item);
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
        return name.startsWith("io.redspace.ironsspellbooks.item.weapons.")
                ||name.startsWith("io.redspace.ironsspellbooks.api.item.weapons.")
                ||item instanceof SwordItem||item instanceof BowItem||item instanceof CrossbowItem||item instanceof TridentItem
                ||!item.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty();
    }
    public static boolean recipe(Recipe<?> recipe){
        // Static item outputs (including Iron's smithing recipes) use the item registry only.
        // A dynamic recipe may require a world's registries; its eventual output is still barred
        // by the inventory/use/loot gates, so never break an unrelated mod's resource reload.
        try{return blocked(recipe.getResultItem(RegistryAccess.EMPTY));}
        catch(RuntimeException dynamicOutput){return false;}
    }
    private DisabledIronsWeapons(){}
}
