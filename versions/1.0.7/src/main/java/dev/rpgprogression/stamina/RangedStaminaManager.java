package dev.rpgprogression.stamina;

import dev.rpgprogression.compat.EpicFightCompat;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.WeakHashMap;
import java.util.Map;

/** One resource for native ranged actions, with optional Epic Fight weight modifiers. */
public final class RangedStaminaManager {
    public static double cost(Player player,String category){
        return StaminaManager.cost(player,baseCost(player,category));
    }
    private static double baseCost(Player player,String category){
        if(player!=null&&StaminaManager.exempt(player))return 0;
        if(EpicFightCompat.battle(player))return EpicFightCompat.rangedCost(player,category);
        return switch(category){case "bow"->RPGConfig.BOW_COST.get();case "crossbow"->RPGConfig.CROSSBOW_CHARGE_COST.get();case "spear"->RPGConfig.TRIDENT_THROW_COST.get();default->throw new IllegalArgumentException("Unknown ranged category: "+category);};
    }
    public static boolean can(ServerPlayer player,String category){
        double cost=cost(player,category);var data=ProgressionManager.get(player);
        return cost==0||!data.exhausted&&data.stamina+.0001>=cost;
    }
    public static boolean spend(ServerPlayer player,String category){
        if(!can(player,category))return false;
        StaminaManager.spend(player,ProgressionManager.get(player),baseCost(player,category));ModNetworking.stamina(player);return true;
    }
    private record Shot(int tick,String category,boolean allowed){}
    private static final Map<ServerPlayer,Shot> shots=new WeakHashMap<>();
    public static boolean projectile(ServerPlayer player,String category){
        // One bow action can spawn multiple projectiles through an optional addon.
        var previous=shots.get(player);
        if(previous!=null&&previous.tick()==player.tickCount&&previous.category().equals(category))return previous.allowed();
        boolean allowed=spend(player,category);shots.put(player,new Shot(player.tickCount,category,allowed));return allowed;
    }
    private RangedStaminaManager(){}
}
