package dev.rpgprogression.resource;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

@EventBusSubscriber(modid="rpgprogression")
public final class SparkManager {
    public static double skintTravelCost(){return RPGConfig.SKINT_TRAVEL_COST.get();}
    public static String skintTravelCostText(){return java.math.BigDecimal.valueOf(skintTravelCost()).stripTrailingZeros().toPlainString();}
    public static final int EXHAUSTION_TICKS=20*20;
    private static final Map<ServerPlayer,Long> lastDenied=new WeakHashMap<>();
    private static boolean exempt(ServerPlayer p){return p.isCreative()||p.isSpectator();}
    private static boolean newlyDepleted(PlayerProgression d){
        if(!ResourceMath.sparkDepleted(d.spark,d.maxSpark())){
            if(d.sparkDepletionNotified){d.sparkDepletionNotified=false;d.dirty=true;}
            return false;
        }
        if(d.sparkDepletionNotified)return false;
        d.sparkDepletionNotified=true;d.dirty=true;return true;
    }
    public static void checkDepletion(ServerPlayer p,PlayerProgression d){
        if(p.isAlive()&&newlyDepleted(d))dev.rpgprogression.effect.ModEffects.exhaust(p,EXHAUSTION_TICKS);
    }
    public static void tick(ServerPlayer p,PlayerProgression d){
        if(!p.isAlive())return;
        checkDepletion(p,d);d.spark=ResourceMath.recover(d.spark,d.maxSpark(),d.maxSpark()/d.sparkRecoveryTicks()*dev.rpgprogression.food.FoodManager.sparkMultiplier(d));
        // Rearm as soon as recovery crosses the threshold, before another action can spend it.
        if(d.sparkDepletionNotified&&!ResourceMath.sparkDepleted(d.spark,d.maxSpark())){d.sparkDepletionNotified=false;d.dirty=true;}
    }
    public static void set(ServerPlayer p,PlayerProgression d,double value){
        d.spark=Math.max(0,Math.min(d.maxSpark(),value));checkDepletion(p,d);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void beforeTravel(EntityTravelToDimensionEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||p.level().dimension().equals(e.getDimension())||exempt(p)||dev.rpgprogression.skint.SkintManager.travelling(p))return;
        if(!canTravel(p,RPGConfig.SPARK_TRAVEL_COST.get()))e.setCanceled(true);
    }
    /** One shared affordability check; normal portals and skints choose their own price. */
    public static boolean canTravel(ServerPlayer p,double cost){
        return enough(p,cost,"rpgprogression.spark.travel_denied");
    }
    public static boolean canOpenSkint(ServerPlayer p){return enough(p,RPGConfig.SKINT_OPEN_MINIMUM.get(),"rpgprogression.skint.spark_requirement");}
    private static boolean enough(ServerPlayer p,double cost,String message){
        if(exempt(p))return true;
        var d=ProgressionManager.get(p);d.initialize();
        if(d.spark+1e-8>=cost)return true;
        long now=p.level().getGameTime();
        if(now-lastDenied.getOrDefault(p,-100L)>=20){
            p.displayClientMessage(Component.translatable(message,java.math.BigDecimal.valueOf(cost).stripTrailingZeros().toPlainString()),true);lastDenied.put(p,now);
        }
        return false;
    }
    public static void charge(ServerPlayer p){charge(p,RPGConfig.SPARK_TRAVEL_COST.get());}
    /** Called once after a successful transfer; failed/cancelled transfers never pay. */
    public static void charge(ServerPlayer p,double cost){
        if(dev.rpgprogression.skint.SkintManager.travelling(p))return;
        if(exempt(p))return;
        var d=ProgressionManager.get(p);d.spark=Math.max(0,d.spark-cost);d.dirty=true;
        checkDepletion(p,d);
        ModNetworking.resources(p);
    }
    @SubscribeEvent public static void travelled(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p&&!e.getFrom().equals(e.getTo()))charge(p);}
    /** Vanilla End credits complete via respawn instead of PlayerChangedDimensionEvent. Death is free. */
    @SubscribeEvent public static void endCredits(PlayerEvent.PlayerRespawnEvent e){if(e.isEndConquered()&&e.getEntity() instanceof ServerPlayer p)charge(p);}
    private SparkManager(){}
}
