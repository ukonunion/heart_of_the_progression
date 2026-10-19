package dev.rpgprogression.event;

import dev.rpgprogression.RPGProgression;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.recipe.IntelligenceRecipeManager;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid=RPGProgression.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class ConfigEvents {
    private static volatile net.neoforged.fml.config.ModConfig pendingConfig;
    @SubscribeEvent public static void loaded(ModConfigEvent.Loading e) {
        if(e.getConfig().getSpec()==RPGConfig.CLIENT){
            var data=e.getConfig().getLoadedConfig().config();
            if(((Number)data.get("hudLayoutVersion")).intValue()<1){
                int old=((Number)data.get("centeredHudBottomOffset")).intValue(),next=old==39||old==45?31:old;
                // Root-level set auto-saves. Batch both keys so the file watcher cannot
                // reload an intermediate offset/version pair between the two mutations.
                data.valueMap().putAll(java.util.Map.of("centeredHudBottomOffset",next,"hudLayoutVersion",1));
                RPGConfig.CLIENT.afterReload();e.getConfig().getLoadedConfig().save();
                com.mojang.logging.LogUtils.getLogger().info("RPG_HUD_UPGRADE old={}, new={}",old,next);
            }
            return;
        }
        if(e.getConfig().getSpec()!=RPGConfig.SERVER)return;
        pendingConfig=e.getConfig();
    }
    public static void upgradeServerConfig(){
        var config=pendingConfig;if(config==null)return;pendingConfig=null;
        if(RPGConfig.RULES_VERSION.get()>=10)return;
        // Migrate known release defaults only; custom inventory thresholds remain intact.
        var legacy=java.util.stream.IntStream.range(0,27).map(i->12+(i/3)*2).boxed().toList();
        var previous=java.util.stream.IntStream.rangeClosed(10,36).boxed().toList();
        var data=config.getLoadedConfig().config();
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
        com.electronwill.nightconfig.core.Config experience=data.get("experience");
        if(RPGConfig.RULES_VERSION.get()<9&&(RPGConfig.BASE_XP.get()==100||(RPGConfig.RULES_VERSION.get()<6&&RPGConfig.BASE_XP.get()==810)))experience.set("baseLevelXP",500.0);
        // Version 7 changes the known 1.18 release default to 819/810; custom rates remain intact.
        if(RPGConfig.RULES_VERSION.get()<7&&RPGConfig.XP_MULTIPLIER.get()==1.18)experience.set("levelXPMultiplier",dev.rpgprogression.progression.ProgressionMath.DEFAULT_MULTIPLIER);
        // Existing custom exponential rates keep their behavior. Recognized release defaults
        // and fresh worlds use the staged curve; base XP and player progress are untouched.
        double rate=((Number)experience.get("levelXPMultiplier")).doubleValue();
        if(rate!=dev.rpgprogression.progression.ProgressionMath.DEFAULT_MULTIPLIER)experience.set("levelXPCurve","EXPONENTIAL");
        // Upgrade recognized previous release defaults; keep custom server values.
        if(RPGConfig.RULES_VERSION.get()<9&&RPGConfig.SPARK_PER_LEVEL.get()==5)spark.set("reservoirPerPotential",1.0);
        com.electronwill.nightconfig.core.Config attributes=data.get("attributes");
        if(RPGConfig.RULES_VERSION.get()<9&&RPGConfig.VITALITY_HP.get()==2)attributes.set("vitalityHealthPerPoint",1.0);
        general.set("progressionRulesVersion",10);RPGConfig.SERVER.afterReload();config.getLoadedConfig().save();
        com.mojang.logging.LogUtils.getLogger().info("RPG_RULES_UPGRADE start={}, spark={}, version={}",RPGConfig.START.get(),RPGConfig.SPARK_START.get(),RPGConfig.RULES_VERSION.get());
    }
    @SubscribeEvent public static void reloaded(ModConfigEvent.Reloading e) {
        if(e.getConfig().getSpec()==RPGConfig.SERVER)IntelligenceRecipeManager.reloadPending=true;
    }
}
