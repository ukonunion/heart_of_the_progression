package dev.rpgprogression.compat;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class EpicFightBridge {
    /** Apply native equipped-skill modifiers to the RPG base, in Minecraft operation order. */
    public static double maximum(net.minecraft.world.entity.player.Player p,double base){
        var attribute=p.getAttribute(yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes.MAX_STAMINA.get());
        if(attribute==null)return base;
        double unit=RPGConfig.EPICFIGHT_UNIT.get(),added=base/unit;
        for(var modifier:attribute.getModifiers(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION))added+=modifier.getAmount();
        double value=added;
        for(var modifier:attribute.getModifiers(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE))value+=added*modifier.getAmount();
        for(var modifier:attribute.getModifiers(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL))value*=1+modifier.getAmount();
        value=attribute.getAttribute().sanitizeValue(value);
        return Double.isFinite(value)?Math.max(0,value*unit):base;
    }
    public static double regeneration(net.minecraft.world.entity.player.Player p){
        var attribute=p.getAttribute(yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes.STAMINA_REGEN.get());
        if(attribute==null)return 1;
        double base=attribute.getAttribute().getDefaultValue(),value=attribute.getValue();
        return base>0&&Double.isFinite(value)?Math.max(0,value/base):1;
    }
    /** Adrenaline's native kill window also prevents our spending delay from restarting. */
    public static int regenerationDelay(net.minecraft.world.entity.player.Player p,int regular){
        var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(p,PlayerPatch.class);
        if(patch==null)return regular;
        for(var container:patch.getSkillCapability().skillContainers){
            if(container.isEmpty()||!container.getSkill().getRegistryName().toString().equals("epicfight:adrenaline_fiend"))continue;
            int killed=container.getDataManager().getDataValue(yesman.epicfight.skill.SkillDataKeys.TICK_RECORD.get());
            if(killed>0&&killed+30>p.tickCount)return 0;
        }
        return regular;
    }
    public static boolean battle(net.minecraft.world.entity.player.Player p){
        var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(p,PlayerPatch.class);
        return patch!=null&&patch.isEpicFightMode();
    }
    public static double attackCost(PlayerPatch<?> patch,net.minecraft.world.InteractionHand hand){
        String category=patch.getHoldingItemCapability(hand).getWeaponCategory().toString();
        return categoryCost(patch,category);
    }
    /** Hovering an inventory item must not equip it or modify the live player's capability. */
    public static boolean weapon(net.minecraft.world.item.ItemStack stack){
        var item=yesman.epicfight.world.capabilities.EpicFightCapabilities.getItemStackCapability(stack);
        return item instanceof yesman.epicfight.world.capabilities.item.WeaponCapability
                ||item instanceof yesman.epicfight.world.capabilities.item.RangedWeaponCapability;
    }
    public static double previewAttackCost(net.minecraft.world.entity.player.Player p,net.minecraft.world.item.ItemStack stack){
        var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(p,PlayerPatch.class);
        return categoryCost(patch,yesman.epicfight.world.capabilities.EpicFightCapabilities.getItemStackCapability(stack).getWeaponCategory().toString());
    }
    public static double previewRangedCost(net.minecraft.world.entity.player.Player p,String category){
        return categoryCost(yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(p,PlayerPatch.class),category);
    }
    public static double categoryCost(PlayerPatch<?> patch,String category){
        double base=WeaponAttackCost.get(category,RPGConfig.EPICFIGHT_WEAPON_COSTS.get(),RPGConfig.EPICFIGHT_ATTACK_DEFAULT.get());
        if(base==0)return 0;
        double cost=patch.getModifiedStaminaConsume((float)(base/RPGConfig.EPICFIGHT_UNIT.get()))*RPGConfig.EPICFIGHT_UNIT.get();
        // A patch awaiting onJoinWorld still has zero weight; do not turn positive costs into free attacks.
        return Double.isFinite(cost)&&cost>0?cost:base;
    }
    public static boolean canAttack(PlayerPatch<?> patch,double cost){
        return StaminaManager.canSpend(patch.getOriginal(),cost);
    }
    public static boolean strike(PlayerPatch<?> patch,net.minecraft.world.InteractionHand hand){return !DisabledIronsWeapons.blocked(patch.getOriginal().getItemInHand(hand))&&spendAttack(patch,attackCost(patch,hand));}
    public static boolean spendAttack(PlayerPatch<?> patch,double cost){
        if(!canAttack(patch,cost))return false;
        var p=patch.getOriginal();StaminaManager.spend(p,ProgressionManager.get(p),cost);
        if(p instanceof ServerPlayer server)ModNetworking.stamina(server);
        return true;
    }
    /** Only server-authored setters may restore stamina (skill refunds, commands, etc.). */
    public static void set(PlayerPatch<?> patch,float value){
        var p=patch.getOriginal();
        if(!(p instanceof ServerPlayer server)||!Float.isFinite(value))return;
        var d=ProgressionManager.get(p);
        // Native skill consumers pass current - raw cost. Retain negative targets:
        // the discounted action may be affordable even when the raw price is not.
        double requested=value*RPGConfig.EPICFIGHT_UNIT.get();
        double cost=d.stamina-requested;
        if(cost>0.0001)StaminaManager.spend(p,d,cost);
        else if(cost<0){d.stamina=Math.max(0,Math.min(d.maxStamina(),requested));StaminaManager.updateExhaustion(p,d);}
        ModNetworking.stamina(server);
    }
    /** Epic Fight's native packet contains an absolute value; never accept a client refill or NaN. */
    public static void clientRequest(PlayerPatch<?> patch,float value,boolean reset){
        if(!Float.isFinite(value)||value>patch.getStamina())return;
        double debit=ProgressionManager.get(patch.getOriginal()).stamina-value*RPGConfig.EPICFIGHT_UNIT.get();
        if(!StaminaManager.canSpend(patch.getOriginal(),debit))return;
        set(patch,value);
        if(reset)patch.resetActionTick();
    }
    public static void releaseRegenerationDelay(PlayerPatch<?> patch,int ticks){
        if(ticks<=0&&patch.getOriginal() instanceof ServerPlayer p)ProgressionManager.get(p).regenCooldown=0;
    }
    private EpicFightBridge(){}
}
