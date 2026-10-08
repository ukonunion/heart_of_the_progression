package dev.rpgprogression.stamina;

import dev.rpgprogression.attribute.AttributeManager;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class StaminaManager {
    public static boolean exempt(Player p) { return p.isSpectator() || (p.isCreative()&&!RPGConfig.CREATIVE_COST.get()); }
    public static void spend(Player p,PlayerProgression d,double cost) {
        if(exempt(p)||!Double.isFinite(cost)||cost<=0)return;
        d.stamina=Math.max(0,d.stamina-cost);
        d.regenCooldown=(int)Math.ceil(RPGConfig.REGEN_DELAY.get()*20);
        updateExhaustion(p,d);
    }
    public static void updateExhaustion(Player p,PlayerProgression d) {
        boolean was=d.exhausted;
        if(exempt(p)) d.exhausted=false;
        else if(d.stamina<=0) d.exhausted=true;
        else if(d.stamina>=d.maxStamina()*RPGConfig.RECOVERY.get()) d.exhausted=false;
        if(d.exhausted)p.setSprinting(false);
        if(was!=d.exhausted){ d.dirty=true; AttributeManager.apply(p,d); }
    }
    public static void tick(ServerPlayer p,PlayerProgression d) {
        if(d.attackCooldown>0)d.attackCooldown--;
        if(d.regenCooldown>0)d.regenCooldown--;
        if(!p.isAlive())return;
        if(d.exhausted)p.setSprinting(false);
        boolean sprint=p.isSprinting()&&!p.isPassenger()&&!p.isFallFlying()&&!p.getAbilities().flying&&!p.isInWater();
        if(sprint){
            if(!dev.rpgprogression.compat.ParCoolCompat.replacesSprint(p))spend(p,d,RPGConfig.SPRINT_COST.get()/20.0);
        }
        else if(d.regenCooldown==0)d.stamina=Math.min(d.maxStamina(),d.stamina+RPGConfig.REGEN.get()/20.0);
        updateExhaustion(p,d);
    }
    private StaminaManager() {}
}
