package dev.rpgprogression.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid="rpgprogression",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class DisabledWeaponCreative {
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void contents(BuildCreativeModeTabContentsEvent e){
        var remove=new ArrayList<ItemStack>();
        for(var entry:e.getEntries())if(DisabledIronsWeapons.blocked(entry.getKey()))remove.add(entry.getKey());
        remove.forEach(e.getEntries()::remove);
    }
    private DisabledWeaponCreative(){}
}
