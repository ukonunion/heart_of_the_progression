package dev.rpgprogression.progression;

import dev.rpgprogression.attribute.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.inventory.InventoryLockManager;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.recipe.IntelligenceRecipeManager;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.player.Player;

public final class ProgressionManager {
    public static PlayerProgression get(Player p) { return p.getCapability(PlayerProgressionProvider.CAPABILITY).orElseThrow(()->new IllegalStateException("Missing RPG player capability")); }
    public static void addXP(ServerPlayer p,long amount) {
        if(amount<=0||p.isSpectator()||(p.isCreative()&&!RPGConfig.CREATIVE_XP.get()))return;
        addXPAdministrative(p,amount);
    }
    public static void addXPAdministrative(ServerPlayer p,long amount) {
        PlayerProgression d=get(p); d.xp=ProgressionMath.add(d.xp,amount); int old=d.level;
        while(d.level<RPGConfig.MAX_LEVEL.get()&&d.xp>=d.requiredXP()) {
            d.xp-=d.requiredXP(); d.level++;
            d.points=(int)Math.min(Integer.MAX_VALUE,(long)d.points+RPGConfig.POINTS.get());
        }
        if(d.level>=RPGConfig.MAX_LEVEL.get())d.xp=0;
        d.dirty=true;
        if(d.level>old) {
            p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.PLAYER_LEVELUP,SoundSource.PLAYERS,0.7f,1.0f);
            p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,p.getX(),p.getY()+1,p.getZ(),18,0.5,0.7,0.5,0);
            ModNetworking.levelUp(p,d.level,d.points);
        }
    }
    public static void spend(ServerPlayer p,int index) {
        if(index<0||index>=RPGAttribute.values().length||!p.isAlive()||p.isSpectator())return;
        PlayerProgression d=get(p); RPGAttribute a=RPGAttribute.values()[index];
        if(d.points<=0||d.get(a)>=RPGConfig.MAX_ATTRIBUTE.get())return;
        int oldInt=d.get(RPGAttribute.INTELLIGENCE); d.points--; d.set(a,d.get(a)+1);
        refresh(p,oldInt,true);
    }
    public static void refresh(ServerPlayer p,int oldInt,boolean notify) {
        PlayerProgression d=get(p); d.initialize();
        d.level=Math.min(d.level,RPGConfig.MAX_LEVEL.get());
        if(d.level>=RPGConfig.MAX_LEVEL.get())d.xp=0;
        else if(d.xp>=d.requiredXP())addXPAdministrative(p,0);
        for(RPGAttribute a:RPGAttribute.values())d.set(a,Math.min(d.get(a),RPGConfig.MAX_ATTRIBUTE.get()));
        AttributeManager.apply(p,d); StaminaManager.updateExhaustion(p,d);
        d.spark=Math.min(d.spark,d.maxSpark());dev.rpgprogression.compat.IronsCompat.refresh(p,d);
        InventoryLockManager.sanitize(p);
        IntelligenceRecipeManager.updateBook(p,oldInt,notify);
        p.containerMenu.slotsChanged(p.getInventory());
        ModNetworking.sync(p);
    }
    private ProgressionManager() {}
}
