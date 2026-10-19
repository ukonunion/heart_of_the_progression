package dev.rpgprogression.verification.gametest;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.xp.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import java.util.*;

public final class MobVariationChecks {
    public static void run(GameTestHelper h,ServerPlayer p){
        var data=ProgressionManager.get(p);data.level=100000;data.xp=0;
        p.getRandom().setSeed(109);var zombie=EntityType.ZOMBIE.create(p.level());var rewards=new HashSet<Long>();
        for(int i=0;i<1000;i++){data.xp=0;XPManager.killed(p,zombie);h.assertTrue(data.xp>=15&&data.xp<=25,"native zombie +/-25% range");rewards.add(data.xp);}
        h.assertTrue(rewards.size()==11&&rewards.contains(15L)&&rewards.contains(25L),"fixed-seed sample covers all zombie rewards: "+rewards);
        var cow=EntityType.COW.create(p.level());data.xp=0;XPManager.killed(p,cow);h.assertTrue(data.xp>=2&&data.xp<=4,"passive mob variation");
        var boss=EntityType.WITHER.create(p.level());data.xp=0;XPManager.killed(p,boss);h.assertTrue(data.xp>=1125&&data.xp<=1875,"boss variation");
        int checked=0;
        for(var type:net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE){
            if(!(type.create(p.serverLevel()) instanceof Mob mob))continue;
            data.xp=0;XPManager.killed(p,mob);
            double category=type.is(XPManager.BOSSES)?RPGConfig.BOSS_MULT.get():mob instanceof net.minecraft.world.entity.monster.Enemy?RPGConfig.HOSTILE_MULT.get():RPGConfig.PASSIVE_MULT.get();
            long center=XPOverrides.reward(mob.getMaxHealth()*RPGConfig.MOB_HEALTH_XP.get()*category*RPGConfig.COMBAT_XP_MULTIPLIER.get());
            h.assertTrue(data.xp>=MobXP.reward(center,0)&&data.xp<=MobXP.reward(center,1),"registered mob follows range, including zero base XP: "+net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type));checked++;
        }
        h.assertTrue(checked>=50,"all registered vanilla and optional mob types checked: "+checked);
        var overrides=RPGConfig.COMBAT_OVERRIDES.get();double multiplier=RPGConfig.COMBAT_XP_MULTIPLIER.get();
        try{
            RPGConfig.COMBAT_OVERRIDES.set(List.of("minecraft:zombie=13"));RPGConfig.COMBAT_XP_MULTIPLIER.set(2.0);
            for(int i=0;i<100;i++){data.xp=0;XPManager.killed(p,zombie);h.assertTrue(data.xp>=20&&data.xp<=33,"override and multiplier before variation");}
            RPGConfig.COMBAT_OVERRIDES.set(List.of("minecraft:zombie=0"));data.xp=0;XPManager.killed(p,zombie);h.assertTrue(data.xp==0,"zero override remains zero");
        }finally{RPGConfig.COMBAT_OVERRIDES.set(overrides);RPGConfig.COMBAT_XP_MULTIPLIER.set(multiplier);}
        data.xp=0;XPManager.fished(p,List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COD)));h.assertTrue(data.xp==8,"fishing remains fixed");
        com.mojang.logging.LogUtils.getLogger().info("HEART_MOB_XP_VERIFIED registered_mobs={}, zombie_range={}",checked,rewards);
    }
    private MobVariationChecks(){}
}
