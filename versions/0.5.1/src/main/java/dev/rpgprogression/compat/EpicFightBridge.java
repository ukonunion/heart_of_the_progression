package dev.rpgprogression.compat;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class EpicFightBridge {
    /** Only server-authored setters may restore stamina (skill refunds, commands, etc.). */
    public static void set(PlayerPatch<?> patch,float value){
        var p=patch.getOriginal();
        if(!(p instanceof ServerPlayer server)||!Float.isFinite(value))return;
        var d=ProgressionManager.get(p);
        double next=Math.max(0,Math.min(d.maxStamina(),value*RPGConfig.EPICFIGHT_UNIT.get()));
        double cost=d.stamina-next;
        if(cost>0.0001)StaminaManager.spend(p,d,cost);
        else if(cost<0){d.stamina=next;StaminaManager.updateExhaustion(p,d);}
        ModNetworking.stamina(server);
    }
    /** Epic Fight's native packet contains an absolute value; never accept a client refill or NaN. */
    public static void clientRequest(PlayerPatch<?> patch,float value,boolean reset){
        if(!Float.isFinite(value)||value<0||value>patch.getStamina())return;
        set(patch,value);
        if(reset)patch.resetActionTick();
    }
    private EpicFightBridge(){}
}
