package dev.rpgprogression.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public final class RPGConfig {
    public static final ForgeConfigSpec SERVER, CLIENT;
    public static final ForgeConfigSpec.IntValue START, POINTS, MAX_LEVEL, MAX_ATTRIBUTE;
    public static final ForgeConfigSpec.DoubleValue BASE_XP, XP_MULTIPLIER, RUN_DISTANCE, RUN_MAX_SPEED;
    public static final ForgeConfigSpec.IntValue RUN_WINDOW_CAP;
    public static final ForgeConfigSpec.DoubleValue BLOCK_BASE, HARDNESS_XP, ORE_XP, RARE_ORE_XP, PLACED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MOB_HEALTH_XP, PASSIVE_MULT, HOSTILE_MULT, BOSS_MULT;
    public static final ForgeConfigSpec.IntValue FISH_XP, JUNK_XP, TREASURE_XP;
    public static final ForgeConfigSpec.DoubleValue BASE_STAMINA, ENDURANCE_STAMINA, SPRINT_COST, ATTACK_COST,
            MINING_BASE_COST, MINING_HARDNESS_COST, MINING_MAX_COST, REGEN, REGEN_DELAY, RECOVERY,
            EXHAUSTED_MINING, EXHAUSTED_ATTACK, STAMINA_SYNC_DELTA;
    public static final ForgeConfigSpec.IntValue STAMINA_SYNC_TICKS;
    public static final ForgeConfigSpec.BooleanValue CREATIVE_COST, CREATIVE_XP, CREATIVE_LOCKS;
    public static final ForgeConfigSpec.DoubleValue STRENGTH_DAMAGE, AGILITY_SPEED, VITALITY_HP, DRAW_FACTOR, MIN_DRAW;
    public static final ForgeConfigSpec.DoubleValue DEFENSE_CAP, PHYSICAL, ELEMENTAL, FIRE, POISON, EXHAUSTION;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> SLOT_THRESHOLDS;
    public static final ForgeConfigSpec.BooleanValue PARCOOL_COMPAT;
    public static final ForgeConfigSpec.DoubleValue PARCOOL_COST;
    public static final ForgeConfigSpec.BooleanValue EPICFIGHT_COMPAT;
    public static final ForgeConfigSpec.DoubleValue EPICFIGHT_UNIT;
    public static final java.util.Map<dev.rpgprogression.attribute.RPGAttribute,ForgeConfigSpec.ConfigValue<List<? extends String>>> ITEM_REQUIREMENTS=new java.util.EnumMap<>(dev.rpgprogression.attribute.RPGAttribute.class);
    public static final ForgeConfigSpec.BooleanValue SHOW_HUD;
    public static final ForgeConfigSpec.IntValue HUD_BOTTOM;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        START = b.defineInRange("startingAttributeValue", 9, 0, 1000);
        POINTS = b.defineInRange("attributePointsPerLevel", 1, 0, 100);
        MAX_LEVEL = b.defineInRange("maxLevel", 100, 2, 1000);
        MAX_ATTRIBUTE = b.defineInRange("maxAttribute", 100, 10, 10000);
        CREATIVE_XP = b.define("creativeGainsXP", false);
        b.pop().push("experience");
        BASE_XP = d(b,"baseLevelXP",810,1,1e12);
        XP_MULTIPLIER = d(b,"levelXPMultiplier",1.18,1,10);
        RUN_DISTANCE = d(b,"runningDistancePerXP",20,1,10000);
        RUN_MAX_SPEED = d(b,"maximumRunningBlocksPerTick",1.5,0.1,10);
        RUN_WINDOW_CAP = b.comment("Maximum running XP per rolling minute; airborne, submerged, riding and stationary movement is excluded.")
                .defineInRange("maximumRunningXPPerMinute",30,0,10000);
        b.pop().push("miningXP");
        BLOCK_BASE=d(b,"baseXP",1,0,10000); HARDNESS_XP=d(b,"hardnessMultiplier",0.5,0,100);
        ORE_XP=d(b,"oreMultiplier",3,0,100); RARE_ORE_XP=d(b,"rareOreMultiplier",6,0,100);
        PLACED_MULTIPLIER=d(b,"playerPlacedMultiplier",0,0,1);
        b.pop().push("combatXP");
        MOB_HEALTH_XP=d(b,"xpPerMaxHealth",0.5,0,100);
        PASSIVE_MULT=d(b,"passiveMultiplier",0.5,0,100); HOSTILE_MULT=d(b,"hostileMultiplier",2,0,100);
        BOSS_MULT=d(b,"bossMultiplier",10,0,1000);
        b.pop().push("fishingXP");
        FISH_XP=b.defineInRange("fish",8,0,10000); JUNK_XP=b.defineInRange("junk",3,0,10000);
        TREASURE_XP=b.defineInRange("treasure",25,0,10000);
        b.pop().push("stamina");
        BASE_STAMINA=d(b,"baseStamina",100,1,100000); ENDURANCE_STAMINA=d(b,"staminaPerEndurance",10,0,1000);
        SPRINT_COST=d(b,"sprintCostPerSecond",4,0,10000); ATTACK_COST=d(b,"attackCost",12,0,10000);
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
        VITALITY_HP=d(b,"vitalityHealthPerPoint",2,0,100);
        DRAW_FACTOR=d(b,"agilityDrawFactor",0.025,0,1); MIN_DRAW=d(b,"minimumDrawTimeRatio",0.25,0.1,1);
        b.pop().push("defense");
        DEFENSE_CAP=d(b,"maxDefensePercent",75,0,95);
        PHYSICAL=d(b,"physicalPerVitality",0.4,0,10); ELEMENTAL=d(b,"elementalPerIntelligence",0.4,0,10);
        FIRE=d(b,"burningPerEndurance",0.4,0,10); POISON=d(b,"poisoningPerVitality",0.4,0,10);
        EXHAUSTION=d(b,"exhaustionPerEndurance",0.5,0,10);
        b.pop().push("inventory");
        SLOT_THRESHOLDS=b.comment("One Endurance requirement for each main inventory slot, left to right, top to bottom. Exactly 27 ascending entries.")
                .defineList("slotEnduranceThresholds",List.of(10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36),
                        x -> x instanceof Integer i && i >= 0 && i <= 10000);
        CREATIVE_LOCKS=b.define("lockCreativeInventory",false);
        b.pop().push("compatibility");
        PARCOOL_COMPAT=b.comment("Share RPG stamina with ParCool 4.x when installed. No ParCool dependency is required.").define("parcoolSharedStamina",true);
        PARCOOL_COST=d(b,"parcoolCostMultiplier",0.1,0,100);
        EPICFIGHT_COMPAT=b.comment("Share RPG stamina with Epic Fight 20.14.x when installed; optional.").define("epicFightSharedStamina",true);
        EPICFIGHT_UNIT=b.comment("RPG stamina per Epic Fight unit. 5 maps the initial 100 RPG stamina to 20 Epic Fight stamina.")
                .defineInRange("epicFightStaminaUnit",5.0,0.001,1000.0);
        b.pop().push("itemRequirements");
        for(var attribute:dev.rpgprogression.attribute.RPGAttribute.values())
            ITEM_REQUIREMENTS.put(attribute,b.comment("Minimum attribute for using/equipping items. Example: minecraft:diamond_sword=20 or #minecraft:swords=15. All matching requirements apply. Empty means unrestricted.")
                    .defineListAllowEmpty(java.util.List.of(attribute.id()),java.util.List.<String>of(),x->x instanceof String s&&dev.rpgprogression.requirements.RequirementRule.valid(s)));
        b.pop(); SERVER=b.build();
        b=new ForgeConfigSpec.Builder();
        SHOW_HUD=b.define("showStaminaHUD",true);
        HUD_BOTTOM=b.comment("Distance from bottom edge to the first centered status row. Additional rows stack upwards.").defineInRange("centeredHudBottomOffset",39,29,2000);
        CLIENT=b.build();
    }
    private static ForgeConfigSpec.DoubleValue d(ForgeConfigSpec.Builder b,String key,double value,double min,double max) {
        return b.defineInRange(key,value,min,max);
    }
    private RPGConfig() {}
}
