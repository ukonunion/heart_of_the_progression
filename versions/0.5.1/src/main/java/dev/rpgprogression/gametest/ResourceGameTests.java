package dev.rpgprogression.gametest;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.resource.SparkManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
public final class ResourceGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);
        near(h,d.spark,25,"initial reservoir");
        var permitted=new EntityTravelToDimensionEvent(p,Level.NETHER);MinecraftForge.EVENT_BUS.post(permitted);
        h.assertFalse(permitted.isCanceled(),"full reservoir permits travel");near(h,d.spark,25,"permission alone never charges");
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(p,Level.OVERWORLD,Level.NETHER));
        near(h,d.spark,0,"successful transition costs exactly25");
        var denied=new EntityTravelToDimensionEvent(p,Level.NETHER);MinecraftForge.EVENT_BUS.post(denied);
        h.assertTrue(denied.isCanceled(),"empty reservoir blocks travel");
        var same=new EntityTravelToDimensionEvent(p,p.level().dimension());MinecraftForge.EVENT_BUS.post(same);
        h.assertFalse(same.isCanceled(),"same dimension teleport is free");
        for(int i=0;i<24000;i++)SparkManager.tick(p,d);near(h,d.spark,25,"full online day recovery");
        d.set(RPGAttribute.SPARK_POTENTIAL,2);near(h,d.maxSpark(),30,"potential adds5");
        d.spark=0;for(int i=0;i<21819;i++)SparkManager.tick(p,d);near(h,d.spark,30,"higher potential fills larger reservoir sooner");
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(p,false));near(h,d.spark,30,"death does not charge travel");
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(p,true));near(h,d.spark,5,"End credits transition charged");
        d.mana=17.5;d.manaInitialized=true;d.manaCapacity=120;
        var copy=new PlayerProgression();copy.load(d.save());near(h,copy.spark,5,"spark persists");near(h,copy.mana,17.5,"fractional mana persists");
        var legacy=d.save();legacy.remove("spark");legacy.remove("spark_potential");legacy.remove("manaInitialized");
        copy.load(legacy);h.assertTrue(copy.get(RPGAttribute.SPARK_POTENTIAL)==1&&!copy.manaInitialized,"old players gain new defaults");near(h,copy.spark,25,"legacy reservoir filled once");
    }
    private static void near(GameTestHelper h,double actual,double expected,String what){h.assertTrue(Math.abs(actual-expected)<.001,what+": "+actual+" != "+expected);}
}
