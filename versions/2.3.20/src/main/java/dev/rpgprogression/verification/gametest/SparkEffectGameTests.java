package dev.rpgprogression.verification.gametest;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.effect.ModEffects;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.resource.SparkManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;

public final class SparkEffectGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);p.removeAllEffects();d.set(RPGAttribute.SPARK_POTENTIAL,11);d.spark=26;d.sparkDepletionNotified=false;
        SparkManager.charge(p);
        h.assertTrue(d.spark==1&&p.getEffect(ModEffects.SPARK_EXHAUSTION).getDuration()==400,"transition into the lowest ten percent lasts twenty seconds");
        h.assertTrue(p.hasEffect(MobEffects.BLINDNESS)&&p.hasEffect(MobEffects.CONFUSION),"one status activates both vanilla behaviours");
        h.assertTrue(p.getActiveEffects().size()==1,"no duplicate blindness or nausea icons");
        h.assertFalse(p.getEffect(MobEffects.CONFUSION).endsWithin(60),"status builds vanilla nausea");
        p.removeAllEffects();SparkManager.set(p,d,25);SparkManager.charge(p);
        var effect=p.getEffect(ModEffects.SPARK_EXHAUSTION);
        h.assertTrue(d.spark==0&&effect.getDuration()==400,"emptying transition lasts twenty seconds");
        for(int i=0;i<20;i++){effect.tick(p,()->{});SparkManager.checkDepletion(p,d);}
        h.assertTrue(effect.getDuration()==380,"low reservoir does not reset duration every tick");
        var saved=d.save();var copy=new PlayerProgression();copy.load(saved);
        h.assertTrue(copy.sparkDepletionNotified,"one-shot marker survives NBT save/load");
        Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET),p.level(),p);
        h.assertTrue(p.hasEffect(MobEffects.BLINDNESS)&&p.hasEffect(MobEffects.CONFUSION),"milk cannot remove spark exhaustion");
        p.removeEffect(ModEffects.SPARK_EXHAUSTION);
        d.load(saved);SparkManager.checkDepletion(p,d);
        h.assertFalse(p.hasEffect(ModEffects.SPARK_EXHAUSTION),"saved empty state does not reapply after clearing");
        SparkManager.set(p,d,5);SparkManager.set(p,d,0);
        h.assertTrue(p.getEffect(ModEffects.SPARK_EXHAUSTION).getDuration()==400,"refill above ten percent rearms the next depletion");
        SparkManager.set(p,d,26);SparkManager.charge(p,5);
        h.assertTrue(p.getEffect(ModEffects.SPARK_EXHAUSTION).getDuration()==400,"an above-threshold transition never shortens the active twenty-second status");
        p.removeAllEffects();p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,400));ModEffects.exhaust(p,60);
        h.assertTrue(p.getEffect(MobEffects.BLINDNESS).getDuration()==400,"existing longer blindness preserved");
        p.removeEffect(ModEffects.SPARK_EXHAUSTION);
        h.assertTrue(p.hasEffect(MobEffects.BLINDNESS)&&!p.hasEffect(MobEffects.CONFUSION),"removing spark status preserves independent effects");
        p.removeAllEffects();h.assertFalse(p.hasEffect(MobEffects.BLINDNESS)||p.hasEffect(MobEffects.CONFUSION),"clean removal leaves no aliases");
    }
    private SparkEffectGameTests(){}
}
