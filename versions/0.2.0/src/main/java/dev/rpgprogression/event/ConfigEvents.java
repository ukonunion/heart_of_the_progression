package dev.rpgprogression.event;

import dev.rpgprogression.RPGProgression;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.recipe.IntelligenceRecipeManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid=RPGProgression.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigEvents {
    @SubscribeEvent public static void loaded(ModConfigEvent.Loading e) {
        if(e.getConfig().getSpec()!=RPGConfig.SERVER)return;
        // Upgrade only the exact unmodified 1.0 inventory/start defaults, preserving custom balance.
        var legacy=java.util.stream.IntStream.range(0,27).map(i->12+(i/3)*2).boxed().toList();
        if(RPGConfig.START.get()==10&&RPGConfig.SLOT_THRESHOLDS.get().equals(legacy)) {
            RPGConfig.START.set(9);
            RPGConfig.SLOT_THRESHOLDS.set(java.util.stream.IntStream.rangeClosed(10,36).boxed().toList());
            e.getConfig().save();
        }
    }
    @SubscribeEvent public static void reloaded(ModConfigEvent.Reloading e) {
        if(e.getConfig().getSpec()==RPGConfig.SERVER)IntelligenceRecipeManager.reloadPending=true;
    }
}
