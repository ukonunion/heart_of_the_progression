package dev.rpgprogression.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public final class RPGConfig {
    public static final ModConfigSpec SERVER, CLIENT;
    public static final ModConfigSpec.IntValue START, POINTS, MAX_ATTRIBUTE;
    public static final ModConfigSpec.DoubleValue BASE_XP, XP_MULTIPLIER, RUN_DISTANCE, RUN_MAX_SPEED;
    public static final ModConfigSpec.EnumValue<dev.rpgprogression.progression.ProgressionMath.Curve> XP_CURVE;
    public static final ModConfigSpec.IntValue RUN_WINDOW_CAP;
    public static final ModConfigSpec.DoubleValue BLOCK_BASE, HARDNESS_XP, ORE_XP, RARE_ORE_XP, PLACED_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MOB_HEALTH_XP, PASSIVE_MULT, HOSTILE_MULT, BOSS_MULT;
    public static final ModConfigSpec.IntValue FISH_XP, JUNK_XP, TREASURE_XP;
    public static final ModConfigSpec.DoubleValue MINING_XP_MULTIPLIER, COMBAT_XP_MULTIPLIER, FISHING_XP_MULTIPLIER, COOKING_XP_MULTIPLIER, FURNACE_XP, CAMPFIRE_XP;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MINING_OVERRIDES, COMBAT_OVERRIDES, FISHING_OVERRIDES, COOKING_OVERRIDES;
    public static final ModConfigSpec.IntValue SPARK_START;
    public static final ModConfigSpec.DoubleValue SPARK_BASE, SPARK_PER_LEVEL, SPARK_RECOVERY_SECONDS, SPARK_RECOVERY_SPEED, SPARK_TRAVEL_COST;
    public static final ModConfigSpec.BooleanValue IRONS_MANA;
    public static final ModConfigSpec.DoubleValue MANA_REGEN, MANA_REGEN_DELAY;
    public static final ModConfigSpec.DoubleValue BASE_STAMINA, ENDURANCE_STAMINA, SPRINT_COST, ATTACK_COST,
            MINING_BASE_COST, MINING_HARDNESS_COST, MINING_MAX_COST, REGEN, REGEN_DELAY, RECOVERY,
            EXHAUSTED_MINING, EXHAUSTED_ATTACK, STAMINA_SYNC_DELTA;
    public static final ModConfigSpec.IntValue STAMINA_SYNC_TICKS;
    public static final ModConfigSpec.DoubleValue BOW_COST, CROSSBOW_CHARGE_COST, TRIDENT_THROW_COST;
    public static final ModConfigSpec.BooleanValue CREATIVE_COST, CREATIVE_XP, CREATIVE_LOCKS;
    public static final ModConfigSpec.DoubleValue STRENGTH_DAMAGE, AGILITY_SPEED, VITALITY_HP, DRAW_FACTOR, MIN_DRAW;
    public static final ModConfigSpec.DoubleValue DEFENSE_CAP, PHYSICAL, ELEMENTAL, MAGICAL, FIRE, POISON, EXHAUSTION;
    public static final ModConfigSpec.DoubleValue PASSIVE_HEALING,SOUP_FOOD_SECONDS,ENERGY_FOOD_SECONDS,CHORUS_FOOD_SECONDS,GOLDEN_FOOD_SECONDS,ENCHANTED_FOOD_SECONDS,ENERGY_FOOD_BONUS;
    public static final ModConfigSpec.IntValue RULES_VERSION, SKILL_LEVEL_INTERVAL;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SLOT_THRESHOLDS;
    public static final ModConfigSpec.BooleanValue PARCOOL_COMPAT;
    public static final ModConfigSpec.DoubleValue PARCOOL_COST;
    public static final ModConfigSpec.BooleanValue EPICFIGHT_COMPAT;
    public static final ModConfigSpec.DoubleValue EPICFIGHT_UNIT;
    public static final ModConfigSpec.DoubleValue EPICFIGHT_ATTACK_DEFAULT;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EPICFIGHT_WEAPON_COSTS;
    public static final java.util.Map<dev.rpgprogression.attribute.RPGAttribute,ModConfigSpec.ConfigValue<List<? extends String>>> ITEM_REQUIREMENTS=new java.util.EnumMap<>(dev.rpgprogression.attribute.RPGAttribute.class);
    public static final ModConfigSpec.BooleanValue SHOW_HUD;
    public static final ModConfigSpec.IntValue HUD_BOTTOM, HUD_VERSION;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("general");
        RULES_VERSION=b.defineInRange("progressionRulesVersion",0,0,100);
        START = b.defineInRange("startingAttributeValue", 10, 0, 1000);
        POINTS = b.defineInRange("attributePointsPerLevel", 1, 0, 100);
        MAX_ATTRIBUTE = b.defineInRange("maxAttribute", 100, 10, 10000);
        CREATIVE_XP = b.define("creativeGainsXP", false);
        b.pop().push("experience");
        BASE_XP = d(b,"baseLevelXP",500,1,1e12);
        XP_CURVE=b.comment("STAGED: easy levels 1-5, moderate growth through 50, then harder quadratic growth. With base 500: costs at levels 1/5/50/100 are 500/700/7000/41000. EXPONENTIAL retains the old configurable formula.")
                .defineEnum("levelXPCurve",dev.rpgprogression.progression.ProgressionMath.Curve.STAGED);
        XP_MULTIPLIER = b.comment("Used only with levelXPCurve=EXPONENTIAL: round(baseLevelXP * levelXPMultiplier^(current level - 1)).")
                .defineInRange("levelXPMultiplier",dev.rpgprogression.progression.ProgressionMath.DEFAULT_MULTIPLIER,1.0,10.0);
        RUN_DISTANCE = d(b,"runningDistancePerXP",20,1,10000);
        RUN_MAX_SPEED = d(b,"maximumRunningBlocksPerTick",1.5,0.1,10);
        RUN_WINDOW_CAP = b.comment("Shared running/fast-swimming XP limit per 1200 ticks. Riding, flight, stationary movement and slow swimming are excluded.")
                .defineInRange("maximumRunningXPPerMinute",30,0,10000);
        b.pop().push("miningXP");
        BLOCK_BASE=d(b,"baseXP",1,0,10000); HARDNESS_XP=d(b,"hardnessMultiplier",0.5,0,100);
        ORE_XP=d(b,"oreMultiplier",3,0,100); RARE_ORE_XP=d(b,"rareOreMultiplier",6,0,100);
        PLACED_MULTIPLIER=d(b,"playerPlacedMultiplier",0,0,1);
        MINING_XP_MULTIPLIER=d(b,"multiplier",1,0,10000);
        MINING_OVERRIDES=xpRules(b,"blockOverrides");
        b.pop().push("combatXP");
        MOB_HEALTH_XP=d(b,"xpPerMaxHealth",0.5,0,100);
        PASSIVE_MULT=d(b,"passiveMultiplier",0.5,0,100); HOSTILE_MULT=d(b,"hostileMultiplier",2,0,100);
        BOSS_MULT=d(b,"bossMultiplier",10,0,1000);
        COMBAT_XP_MULTIPLIER=d(b,"multiplier",1,0,10000);
        COMBAT_OVERRIDES=xpRules(b,"entityOverrides");
        b.pop().push("fishingXP");
        FISH_XP=b.defineInRange("fish",8,0,10000); JUNK_XP=b.defineInRange("junk",3,0,10000);
        TREASURE_XP=b.defineInRange("treasure",25,0,10000);
        FISHING_XP_MULTIPLIER=d(b,"multiplier",1,0,10000);
        FISHING_OVERRIDES=xpRules(b,"itemOverrides");
        b.pop().push("cookingXP");
        FURNACE_XP=d(b,"furnaceXPPerItem",4,0,1000000);
        CAMPFIRE_XP=d(b,"campfireXPPerItem",3,0,1000000);
        COOKING_XP_MULTIPLIER=d(b,"multiplier",1,0,10000);
        COOKING_OVERRIDES=xpRules(b,"itemOverrides");
        b.pop().push("spark");
        SPARK_START=b.defineInRange("startingPotential",10,1,1000);
        SPARK_BASE=d(b,"baseReservoir",25,1,1000000);
        SPARK_PER_LEVEL=d(b,"reservoirPerPotential",1,0,100000);
        SPARK_TRAVEL_COST=d(b,"dimensionTravelCost",25,0,1000000);
        SPARK_RECOVERY_SECONDS=d(b,"baseFullRecoverySeconds",1200,1,86400);
        SPARK_RECOVERY_SPEED=b.comment("Full recovery time = base seconds / (1 + extra potential * this value). Tick based: 1200 seconds = 24000 ticks; sleeping does not instantly refill it.")
                .defineInRange("recoverySpeedPerPotential",0.1,0,10);
        b.pop().push("mana");
        MANA_REGEN=b.comment("New mana units per second. Capacity is 0..8, one unit per 5 Spark Potential levels above its starting value; equipment never increases it.").defineInRange("unitRegenPerSecond",0.2,0,8);
        MANA_REGEN_DELAY=d(b,"regenDelaySeconds",2,0,60);
        b.pop().push("stamina");
        BASE_STAMINA=d(b,"baseStamina",100,1,100000); ENDURANCE_STAMINA=d(b,"staminaPerEndurance",10,0,1000);
        SPRINT_COST=d(b,"sprintCostPerSecond",4,0,10000); ATTACK_COST=d(b,"attackCost",12,0,10000);
        BOW_COST=d(b,"bowShotCost",8,0,10000);
        CROSSBOW_CHARGE_COST=b.comment("Charged once on successful loading, not when firing. Cancelled/incomplete charging and missing ammo are free.").defineInRange("crossbowChargeCost",10.0,0.0,10000.0);
        TRIDENT_THROW_COST=d(b,"tridentThrowCost",14,0,10000);
        MINING_BASE_COST=d(b,"miningBaseCost",1,0,1000); MINING_HARDNESS_COST=d(b,"miningHardnessCost",1,0,1000);
        MINING_MAX_COST=d(b,"miningMaximumCost",5,0,10000);
        REGEN=d(b,"regenRate",8,0,10000); REGEN_DELAY=d(b,"regenDelaySeconds",1.5,0,60);
        RECOVERY=d(b,"exhaustedRecoveryThreshold",0.15,0.01,1);
        EXHAUSTED_MINING=d(b,"exhaustedMiningSpeed",0.4,0.01,1); EXHAUSTED_ATTACK=d(b,"exhaustedAttackSpeed",0.5,0.01,1);
        STAMINA_SYNC_DELTA=d(b,"syncDelta",0.5,0.05,100);
        STAMINA_SYNC_TICKS=b.defineInRange("syncIntervalTicks",10,2,100);
        CREATIVE_COST=b.define("creativeConsumesStamina",false);
        b.pop().push("attributes");
        STRENGTH_DAMAGE=d(b,"strengthDamageBonusPerPoint",0.02,0,1);
        AGILITY_SPEED=d(b,"agilityMovementBonusPerPoint",0.01,0,0.2);
        VITALITY_HP=d(b,"vitalityHealthPerPoint",1,0,100);
        DRAW_FACTOR=d(b,"agilityDrawFactor",0.025,0,1); MIN_DRAW=d(b,"minimumDrawTimeRatio",0.25,0.1,1);
        b.pop().push("defense");
        DEFENSE_CAP=d(b,"maxDefensePercent",75,0,95);
        PHYSICAL=d(b,"physicalPerVitality",0.4,0,10); ELEMENTAL=d(b,"elementalPerIntelligence",0.4,0,10);
        MAGICAL=d(b,"magicalPerIntelligence",0.4,0,10);
        FIRE=d(b,"burningPerEndurance",0.4,0,10); POISON=d(b,"poisoningPerVitality",0.4,0,10);
        EXHAUSTION=d(b,"exhaustionPerEndurance",0.5,0,10);
        b.pop().push("food");
        PASSIVE_HEALING=b.comment("Constant HP per second, independent of hunger, saturation and naturalRegeneration. One HP is half a heart.").defineInRange("passiveHealthPerSecond",0.1,0,100);
        SOUP_FOOD_SECONDS=d(b,"soupProtectionSeconds",120,1,86400);
        ENERGY_FOOD_SECONDS=d(b,"energyBoostSeconds",60,1,86400);
        ENERGY_FOOD_BONUS=d(b,"energySpeedAndStaminaBonus",0.1,0,10);
        CHORUS_FOOD_SECONDS=d(b,"chorusRecoverySeconds",120,1,86400);
        GOLDEN_FOOD_SECONDS=d(b,"goldenAppleProtectionSeconds",120,1,86400);
        ENCHANTED_FOOD_SECONDS=d(b,"enchantedAppleProtectionSeconds",300,1,86400);
        b.pop().push("inventory");
        SLOT_THRESHOLDS=b.comment("One Endurance requirement for each main inventory slot, left to right, top to bottom. Exactly 27 ascending entries.")
                .defineList("slotEnduranceThresholds",List.of(11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37),
                        x -> x instanceof Integer i && i >= 0 && i <= 10000);
        CREATIVE_LOCKS=b.define("lockCreativeInventory",false);
        b.pop().push("compatibility");
        PARCOOL_COMPAT=b.comment("Share RPG stamina with ParCool 4.x when installed. No ParCool dependency is required.").define("parcoolSharedStamina",true);
        PARCOOL_COST=d(b,"parcoolCostMultiplier",0.1,0,100);
        EPICFIGHT_COMPAT=b.comment("Share RPG stamina with Epic Fight 20.14.x when installed; optional.").define("epicFightSharedStamina",true);
        EPICFIGHT_UNIT=b.comment("RPG stamina per Epic Fight unit. 5 maps the initial 100 RPG stamina to 20 Epic Fight stamina.")
                .defineInRange("epicFightStaminaUnit",5.0,0.001,1000.0);
        EPICFIGHT_ATTACK_DEFAULT=d(b,"epicFightDefaultAttackCost",12,0,10000);
        EPICFIGHT_WEAPON_COSTS=b.comment("RPG stamina per Epic Fight strike, before native armor/skill modifiers. Weapon category=cost; addon category names supported. Multi-target hits charge once per strike.")
                .defineListAllowEmpty(List.of("epicFightWeaponAttackCosts"),List.of("fist=4","dagger=6","sword=12","longsword=18","greatsword=24","axe=16","spear=14","tachi=18","uchigatana=10","hoe=8","pickaxe=14","shovel=10","bow=8","crossbow=10","shield=10","not_weapon=6"),
                        x->x instanceof String s&&dev.rpgprogression.compat.WeaponAttackCost.valid(s));
        IRONS_MANA=b.comment("Replace Iron's Spells n Spellbooks 3.16.x player mana with RPG mana when installed. Mobs remain native.").define("ironsSharedMana",true);
        b.pop().push("skills");
        SKILL_LEVEL_INTERVAL=b.comment("One separate skill point at levels 5, 10, 15... by default. Previously reached milestones are granted once. Each unlock costs one point.")
                .defineInRange("levelsPerSkillPoint",5,1,1000);
        b.pop().push("itemRequirements");
        for(var attribute:dev.rpgprogression.attribute.RPGAttribute.values())
            ITEM_REQUIREMENTS.put(attribute,b.comment("Minimum attribute for using/equipping items. Example: minecraft:diamond_sword=20 or #minecraft:swords=15. All matching requirements apply. Empty means unrestricted.")
                    .defineListAllowEmpty(java.util.List.of(attribute.id()),java.util.List.<String>of(),x->x instanceof String s&&dev.rpgprogression.requirements.RequirementRule.valid(s)));
        b.pop(); SERVER=b.build();
        b=new ModConfigSpec.Builder();
        SHOW_HUD=b.define("showStaminaHUD",true);
        HUD_BOTTOM=b.comment("HP distance from bottom edge. Default 31 places it over the former XP bar, just above the hotbar. Stamina and ridden-animal health stack above HP.").defineInRange("centeredHudBottomOffset",31,31,2000);
        HUD_VERSION=b.defineInRange("hudLayoutVersion",0,0,100);
        CLIENT=b.build();
    }
    private static ModConfigSpec.DoubleValue d(ModConfigSpec.Builder b,String key,double value,double min,double max) {
        return b.defineInRange(key,value,min,max);
    }
    private static ModConfigSpec.ConfigValue<List<? extends String>> xpRules(ModConfigSpec.Builder b,String key){
        return b.comment("Absolute XP before multiplier. Examples: minecraft:zombie=20, minecraft:diamond_ore=15, minecraft:cooked_beef=4; #namespace:tag=amount is supported. Exact ID overrides tags.")
                .defineListAllowEmpty(List.of(key),List.<String>of(),x->x instanceof String s&&dev.rpgprogression.xp.XPRule.valid(s));
    }
    private RPGConfig() {}
}
