package dev.rpgprogression.compat;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.ArrayList;

@EventBusSubscriber(modid="rpgprogression")
public final class DisabledWeaponCreative {
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void contents(BuildCreativeModeTabContentsEvent e){
        var remove=new ArrayList<ItemStack>();
        for(var entry:e.getParentEntries())if(DisabledIronsWeapons.blocked(entry))remove.add(entry);
        for(var entry:e.getSearchEntries())if(DisabledIronsWeapons.blocked(entry))remove.add(entry);
        remove.forEach(stack->e.remove(stack,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }
    private DisabledWeaponCreative(){}
}
