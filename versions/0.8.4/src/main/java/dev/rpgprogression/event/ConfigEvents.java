package dev.rpgprogression.event;

import dev.rpgprogression.RPGProgression;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.recipe.IntelligenceRecipeManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid=RPGProgression.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigEvents {
    private static volatile net.minecraftforge.fml.config.ModConfig pendingConfig;
    @SubscribeEvent public static void loaded(ModConfigEvent.Loading e) {
        if(e.getConfig().getSpec()==RPGConfig.CLIENT){
            var data=e.getConfig().getConfigData();
            if(((Number)data.get("hudLayoutVersion")).intValue()<1){
                int old=((Number)data.get("centeredHudBottomOffset")).intValue(),next=old==39||old==45?31:old;
                // Root-level set auto-saves. Batch both keys so the file watcher cannot
                // reload an intermediate offset/version pair between the two mutations.
                data.valueMap().putAll(java.util.Map.of("centeredHudBottomOffset",next,"hudLayoutVersion",1));
                RPGConfig.CLIENT.afterReload();e.getConfig().save();
                com.mojang.logging.LogUtils.getLogger().info("RPG_HUD_UPGRADE old={}, new={}",old,next);
            }
            return;
        }
        if(e.getConfig().getSpec()!=RPGConfig.SERVER)return;
        pendingConfig=e.getConfig();
    }
    public static void upgradeServerConfig(){
        var config=pendingConfig;if(config==null)return;pendingConfig=null;
        if(RPGConfig.RULES_VERSION.get()>=6)return;
        // Migrate known release defaults only; custom inventory thresholds remain intact.
        var legacy=java.util.stream.IntStream.range(0,27).map(i->12+(i/3)*2).boxed().toList();
        var previous=java.util.stream.IntStream.rangeClosed(10,36).boxed().toList();
        var data=config.getConfigData();
        // Mutate the nested sections, then save once. ConfigValue.set auto-saves each individual
        // change and the file watcher can reload an intermediate version during a multi-key upgrade.
        com.electronwill.nightconfig.core.Config general=data.get("general"),spark=data.get("spark"),inventory=data.get("inventory");
        if(RPGConfig.RULES_VERSION.get()<5){
            if(RPGConfig.START.get()==9)general.set("startingAttributeValue",10);
            if(RPGConfig.SPARK_START.get()==1)spark.set("startingPotential",10);
            if(RPGConfig.SLOT_THRESHOLDS.get().equals(legacy)||RPGConfig.SLOT_THRESHOLDS.get().equals(previous))
                inventory.set("slotEnduranceThresholds",java.util.stream.IntStream.rangeClosed(11,37).boxed().toList());
            general.remove("maxLevel");
            com.electronwill.nightconfig.core.Config mana=data.get("mana");
            for(String key:new String[]{"baseMana","manaPerIntelligence","regenPerSecond","regenPerIntelligence"})mana.remove(key);
        }
        // Replace the previous release default; intentionally customized XP curves are retained.
        if(RPGConfig.BASE_XP.get()==810){com.electronwill.nightconfig.core.Config experience=data.get("experience");experience.set("baseLevelXP",100.0);}
        general.set("progressionRulesVersion",6);RPGConfig.SERVER.afterReload();config.save();
        com.mojang.logging.LogUtils.getLogger().info("RPG_RULES_UPGRADE start={}, spark={}, version={}",RPGConfig.START.get(),RPGConfig.SPARK_START.get(),RPGConfig.RULES_VERSION.get());
    }
    @SubscribeEvent public static void reloaded(ModConfigEvent.Reloading e) {
        if(e.getConfig().getSpec()==RPGConfig.SERVER)IntelligenceRecipeManager.reloadPending=true;
    }
}
