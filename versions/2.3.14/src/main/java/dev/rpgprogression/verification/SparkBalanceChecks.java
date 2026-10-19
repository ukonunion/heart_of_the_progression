package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.effect.ModEffects;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.resource.SparkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;

/** Exercise the actual player resource state, effect aliases and persisted threshold latch. */
public final class SparkBalanceChecks {
    private static void check(JsonArray checks,boolean passed,String name){SkintTravelChecks.check(checks,passed,name);}
    public static void run(ServerPlayer p,JsonArray checks){
        var d=ProgressionManager.get(p);var saved=d.save();var mode=p.gameMode.getGameModeForPlayer();
        try{
            p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();d.set(RPGAttribute.SPARK_POTENTIAL,10);d.chorusFoodStacks=0;d.chorusFoodTicks=0;
            check(checks,RPGConfig.SPARK_RECOVERY_SECONDS.get()==2400&&RPGConfig.RULES_VERSION.get()==11,"default configuration migrates to forty-minute recovery and rules version eleven");
            check(checks,d.maxSpark()==25&&d.sparkRecoveryTicks()==48000,"starting potential requires 48000 online ticks for full recovery");
            d.spark=0;d.sparkDepletionNotified=false;
            for(int i=0;i<24000;i++)SparkManager.tick(p,d);
            check(checks,Math.abs(d.spark-12.5)<1e-7,"twenty minutes now restores only half of the starting reservoir");
            for(int i=0;i<24000;i++)SparkManager.tick(p,d);
            check(checks,Math.abs(d.spark-25)<1e-7,"forty minutes restores the full starting reservoir");
            p.removeAllEffects();SparkManager.set(p,d,25);SparkManager.charge(p,5);
            check(checks,d.spark==20&&!p.hasEffect(ModEffects.SPARK_EXHAUSTION),"travel above ten percent costs five without applying exhaustion");
            SparkManager.set(p,d,7.5);SparkManager.charge(p,5);
            var effect=p.getEffect(ModEffects.SPARK_EXHAUSTION);
            check(checks,d.spark==2.5&&effect!=null&&effect.getDuration()==400,"a charge reaching exactly ten percent applies twenty seconds of exhaustion");
            check(checks,p.hasEffect(MobEffects.BLINDNESS)&&p.hasEffect(MobEffects.CONFUSION)&&p.getActiveEffects().size()==1,"one exhaustion icon still supplies blindness and nausea");
            for(int i=0;i<20;i++){effect.tick(p,()->{});SparkManager.checkDepletion(p,d);}
            SparkManager.set(p,d,0);
            check(checks,effect.getDuration()==380,"remaining below the threshold or emptying does not refresh the effect");
            var low=d.save();var copy=new PlayerProgression();copy.load(low);
            check(checks,copy.sparkDepletionNotified,"the threshold latch survives save and reload");
            p.removeAllEffects();d.load(low);SparkManager.checkDepletion(p,d);
            check(checks,!p.hasEffect(ModEffects.SPARK_EXHAUSTION),"loading an already triggered low reservoir does not apply the effect again");
            SparkManager.set(p,d,2.5);SparkManager.tick(p,d);
            check(checks,d.spark>2.5&&!d.sparkDepletionNotified,"recovery above ten percent immediately rearms the next crossing");
            SparkManager.charge(p,.01);
            check(checks,p.getEffect(ModEffects.SPARK_EXHAUSTION).getDuration()==400,"spending back below the recovered threshold triggers twenty seconds again");
            p.removeAllEffects();SparkManager.set(p,d,25);SparkManager.charge(p);
            check(checks,d.spark==0&&p.getEffect(ModEffects.SPARK_EXHAUSTION).getDuration()==400,"an ordinary dimension charge still costs twenty-five and exhausts only its low result");
            p.removeAllEffects();d.set(RPGAttribute.SPARK_POTENTIAL,50);SparkManager.set(p,d,6.50001);
            check(checks,!p.hasEffect(ModEffects.SPARK_EXHAUSTION),"a larger reservoir does not exhaust above its own ten-percent threshold");
            SparkManager.set(p,d,6.5);
            check(checks,p.getEffect(ModEffects.SPARK_EXHAUSTION).getDuration()==400,"the threshold follows the actual larger reservoir, not a fixed spark amount");
            p.removeAllEffects();SparkManager.set(p,d,d.maxSpark());p.setGameMode(GameType.CREATIVE);double balance=d.spark;SparkManager.charge(p,5);
            check(checks,d.spark==balance&&!p.hasEffect(ModEffects.SPARK_EXHAUSTION),"creative travel remains free and does not create unconditional exhaustion");
        }finally{p.removeAllEffects();d.load(saved);p.setGameMode(mode);}
    }
    private SparkBalanceChecks(){}
}
