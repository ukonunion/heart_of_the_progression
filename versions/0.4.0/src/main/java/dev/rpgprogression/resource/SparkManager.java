package dev.rpgprogression.resource;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="rpgprogression")
public final class SparkManager {
    private static final Map<ServerPlayer,Long> lastDenied=new WeakHashMap<>();
    private static boolean exempt(ServerPlayer p){return p.isCreative()||p.isSpectator();}
    public static void tick(ServerPlayer p,PlayerProgression d){if(p.isAlive())d.spark=ResourceMath.recover(d.spark,d.maxSpark(),d.maxSpark()/d.sparkRecoveryTicks());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void beforeTravel(EntityTravelToDimensionEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||p.level().dimension().equals(e.getDimension())||exempt(p))return;
        var d=ProgressionManager.get(p);d.initialize();
        if(d.spark+1e-8<RPGConfig.SPARK_TRAVEL_COST.get()){
            e.setCanceled(true);long now=p.level().getGameTime();
            if(now-lastDenied.getOrDefault(p,-100L)>=20){p.displayClientMessage(Component.translatable("rpgprogression.spark.travel_denied",RPGConfig.SPARK_TRAVEL_COST.get().intValue()),true);lastDenied.put(p,now);}
        }
    }
    public static void charge(ServerPlayer p){
        if(exempt(p))return;
        var d=ProgressionManager.get(p);d.spark=Math.max(0,d.spark-RPGConfig.SPARK_TRAVEL_COST.get());d.dirty=true;
        ModNetworking.resources(p);
    }
    @SubscribeEvent public static void travelled(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p&&!e.getFrom().equals(e.getTo()))charge(p);}
    /** Vanilla End credits complete via respawn instead of PlayerChangedDimensionEvent. Death is free. */
    @SubscribeEvent public static void endCredits(PlayerEvent.PlayerRespawnEvent e){if(e.isEndConquered()&&e.getEntity() instanceof ServerPlayer p)charge(p);}
    private SparkManager(){}
}
