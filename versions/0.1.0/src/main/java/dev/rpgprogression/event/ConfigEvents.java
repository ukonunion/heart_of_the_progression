package dev.rpgprogression.event;

import dev.rpgprogression.RPGProgression;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.recipe.IntelligenceRecipeManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid=RPGProgression.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigEvents {
    @SubscribeEvent public static void reloaded(ModConfigEvent.Reloading e) {
        if(e.getConfig().getSpec()==RPGConfig.SERVER)IntelligenceRecipeManager.reloadPending=true;
    }
}
