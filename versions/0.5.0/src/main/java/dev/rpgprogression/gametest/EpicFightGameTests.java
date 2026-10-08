package dev.rpgprogression.gametest;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public final class EpicFightGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);
        h.assertTrue(patch!=null,"native Epic Fight capability attached");
        var d=ProgressionManager.get(p);
        near(h,patch.getMaxStamina(),20,"100 RPG to 20 Epic Fight units");
        near(h,patch.getStamina(),20,"shared initial current value");
        h.assertTrue(Skill.Resource.STAMINA.predicate.canExecute(null,patch,3),"native skill predicate");
        Skill.Resource.STAMINA.consumer.consume(null,patch,3);
        near(h,d.stamina,85,"native skill consumer charges shared pool exactly once");
        h.assertTrue(d.regenCooldown==30,"native spending resets RPG regen delay");
        patch.setStaminaRegenAwaitTicks(0);
        patch.serverTick(new LivingEvent.LivingTickEvent(p));
        near(h,d.stamina,85,"Epic Fight tick cannot regenerate a second pool");
        StaminaManager.spend(p,d,10);near(h,patch.getStamina(),15,"RPG spending visible in Epic Fight");
        d.set(RPGAttribute.ENDURANCE,10);near(h,patch.getMaxStamina(),22,"Endurance adjusts native maximum");
        EpicFightBridge.clientRequest(patch,9999,true);EpicFightBridge.clientRequest(patch,Float.NaN,true);
        EpicFightBridge.clientRequest(patch,-1,true);near(h,d.stamina,75,"invalid/untrusted client refills rejected");
        EpicFightBridge.clientRequest(patch,14,true);near(h,d.stamina,70,"native client debit accepted");
        patch.setStamina(16);near(h,d.stamina,80,"trusted server skill refund");
        patch.setStamina(0);h.assertFalse(patch.hasStamina(1),"exhaustion denies native skill");
        d.stamina=10;StaminaManager.updateExhaustion(p,d);h.assertFalse(patch.hasStamina(1),"recovery hysteresis shared");
        d.stamina=17;StaminaManager.updateExhaustion(p,d);h.assertTrue(patch.hasStamina(1),"skill restored at shared recovery threshold");
        double old=d.stamina;RPGConfig.EPICFIGHT_COMPAT.set(false);
        try{patch.setStamina(3);near(h,d.stamina,old,"disabled adapter leaves RPG data alone");}
        finally{RPGConfig.EPICFIGHT_COMPAT.set(true);}
        if(ParCoolCompat.enabled())near(h,com.alrex.parcool.common.Parkourability.get(p).getStamina().value(),d.stamina,"all three mods use the same pool");
    }
    private static void near(GameTestHelper h,double value,double expected,String message){h.assertTrue(Math.abs(value-expected)<0.001,message+": "+value+" != "+expected);}
    private EpicFightGameTests(){}
}
