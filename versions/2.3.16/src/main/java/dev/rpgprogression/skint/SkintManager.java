package dev.rpgprogression.skint;

import java.util.*;
import dev.rpgprogression.block.ModBlocks;
import dev.rpgprogression.block.TravelSkintBlock;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.resource.SparkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="rpgprogression")
public final class SkintManager {
    public record Session(UUID token,SkintNetwork.Point origin){}
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private record PendingTravel(Session session,UUID destination,UUID seat,long fadeBegan){}
    private record Arrival(SkintNetwork.Point point,ServerLevel level,Vec3 position){}
    private static final Map<UUID,PendingTravel> PENDING=new HashMap<>();
    public static boolean pending(ServerPlayer p){return PENDING.containsKey(p.getUUID());}
    private static final Set<UUID> TRANSIT=new HashSet<>();
    public static boolean travelling(ServerPlayer p){return TRANSIT.contains(p.getUUID());}
    public static Session session(ServerPlayer p){return SESSIONS.get(p.getUUID());}
    public static boolean canProgress(ServerPlayer p){
        var session=session(p);if(session==null||p.isRemoved()||!p.isAlive()||p.isSpectator()||p.isSleeping())return false;
        var origin=session.origin();var level=p.serverLevel();
        return SkintSeating.seated(p,origin.pos())&&origin.dimension().equals(level.dimension())&&p.position().distanceToSqr(Vec3.atCenterOf(origin.pos()))<=36
            &&level.hasChunkAt(origin.pos())&&TravelSkintBlock.active(level,origin.pos())
            &&level.getBlockState(origin.pos()).canSurvive(level,origin.pos())&&origin.equals(SkintNetwork.get(level).point(origin.id()));
    }
    public static void use(ServerPlayer p,BlockPos pos){use(p,pos,false);}
    private static void use(ServerPlayer p,BlockPos pos,boolean arrival){
        if(pending(p))return;var level=p.serverLevel();if(!p.isAlive()||p.isSpectator()||p.isSleeping()||p.position().distanceToSqr(Vec3.atCenterOf(pos))>36
            ||!level.hasChunkAt(pos)||!TravelSkintBlock.active(level,pos)||!level.getBlockState(pos).canSurvive(level,pos))return;
        var point=SkintNetwork.get(level).ensure(level,pos);var data=ProgressionManager.get(p);
        if(data.discoveredSkints.removeIf(id->SkintNetwork.get(level).point(id)==null))data.dirty=true;
        if(!data.discoveredSkints.contains(point.id())&&data.discoveredSkints.size()>=4096){p.displayClientMessage(tr("limit"),true);return;}
        if(data.discoveredSkints.add(point.id())){data.dirty=true;p.displayClientMessage(tr("activated"),true);}
        if(!SkintSeating.sit(p,pos)){close(p,true);p.displayClientMessage(tr("no_seat"),true);return;}
        SESSIONS.put(p.getUUID(),new Session(UUID.randomUUID(),point));snapshot(p,arrival?3:0);
    }
    public static void snapshot(ServerPlayer p,int mode){
        var session=session(p);if(session==null)return;var tag=new CompoundTag();tag.putInt("mode",mode);tag.putUUID("token",session.token());tag.put("origin",session.origin().tag());
        var entries=new ListTag();for(var point:SkintNetwork.get(p.serverLevel()).discovered(ProgressionManager.get(p).discoveredSkints))entries.add(point.tag());tag.put("points",entries);
        ModNetworking.skintSnapshot(p,tag);
    }
    public static void action(ServerPlayer p,int action,UUID token,UUID target){
        var session=session(p);if(session==null||!session.token().equals(token))return;
        if(action==2){
            PENDING.remove(p.getUUID());SESSIONS.remove(p.getUUID());
            if(p.getVehicle() instanceof SkintSeat seat)seat.beginStanding();
            return;
        }
        if(action==4){completeTravel(p);return;}
        if(pending(p))return;
        if(!canProgress(p)){close(p,true);return;}
        switch(action){
            case 0->{ModNetworking.sync(p);dev.rpgprogression.skill.SkillTreeManager.sync(p);snapshot(p,1);}
            case 1->travel(p,target);
            case 3->snapshot(p,0);
            default->{}
        }
    }
    /** Accept a destination, play stand in place, and transfer only when the full clip has finished. */
    public static boolean travel(ServerPlayer p,UUID id){
        if(pending(p)||!canProgress(p)||!SparkManager.canTravel(p,SparkManager.skintTravelCost())||arrival(p,id).isEmpty())return false;
        var seat=(SkintSeat)p.getVehicle();PENDING.put(p.getUUID(),new PendingTravel(session(p),id,seat.getUUID(),-1));
        seat.beginStanding();snapshot(p,2);return true;
    }
    private static Optional<Arrival> arrival(ServerPlayer p,UUID id){
        var data=ProgressionManager.get(p);var network=SkintNetwork.get(p.serverLevel());var destination=network.point(id);var session=session(p);
        if(destination==null||!data.discoveredSkints.contains(id)||session==null||session.origin().id().equals(id)){p.displayClientMessage(tr("unavailable"),true);return Optional.empty();}
        var level=p.getServer().getLevel(destination.dimension());
        if(level==null||!level.getWorldBorder().isWithinBounds(destination.pos())||level.isOutsideBuildHeight(destination.pos())){p.displayClientMessage(tr("unavailable"),true);return Optional.empty();}
        level.getChunkAt(destination.pos());
        if(!TravelSkintBlock.active(level,destination.pos())||!level.getBlockState(destination.pos()).canSurvive(level,destination.pos())){
            network.remove(level,destination.pos());p.displayClientMessage(tr("unavailable"),true);snapshot(p,0);return Optional.empty();
        }
        var adjacent=BonfireTravel.attemptToPlaceNextToBonfire(destination.pos(),level,p);
        var safe=adjacent.flatMap(pos->SkintSeating.sittingLocation(p,level,destination.pos(),pos));
        if(safe.isEmpty()){p.displayClientMessage(tr("unsafe"),true);return Optional.empty();}
        return Optional.of(new Arrival(destination,level,safe.get()));
    }
    private static boolean validDeparture(ServerPlayer p,PendingTravel pending){
        var origin=pending.session.origin();var level=p.serverLevel();
        return p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&pending.session.equals(session(p))&&level.dimension().equals(origin.dimension())
            &&p.getVehicle() instanceof SkintSeat seat&&seat.getUUID().equals(pending.seat)&&seat.standing()&&seat.belongsTo(p,origin.pos())
            &&p.position().distanceToSqr(Vec3.atCenterOf(origin.pos()))<=36&&level.hasChunkAt(origin.pos())
            &&TravelSkintBlock.active(level,origin.pos())&&level.getBlockState(origin.pos()).canSurvive(level,origin.pos())
            &&origin.equals(SkintNetwork.get(level).point(origin.id()));
    }
    public static void finishedStanding(ServerPlayer p,SkintSeat seat){
        var pending=PENDING.get(p.getUUID());if(pending==null){SkintSeating.stand(p);return;}
        if(!validDeparture(p,pending)){close(p,true);return;}
        if(pending.fadeBegan>=0)return;
        PENDING.put(p.getUUID(),new PendingTravel(pending.session,pending.destination,pending.seat,p.serverLevel().getGameTime()));
        snapshot(p,4);
    }
    /** Acknowledge a fully rendered black frame; never move on a timer alone. */
    private static void completeTravel(ServerPlayer p){
        var pending=PENDING.get(p.getUUID());
        if(pending==null||pending.fadeBegan<0)return;
        if(!validDeparture(p,pending)){close(p,true);return;}
        var seat=(SkintSeat)p.getVehicle();
        var target=arrival(p,pending.destination);PENDING.remove(p.getUUID());
        // The destination may have been removed or obstructed during the animation.
        double cost=SparkManager.skintTravelCost();
        if(target.isEmpty()||!SparkManager.canTravel(p,cost)){seat.resumeSitting();snapshot(p,0);return;}
        var destination=target.get();var previous=p.serverLevel();var from=seat.position();var origin=pending.session.origin();
        SkintSeating.stand(p);boolean success;
        TRANSIT.add(p.getUUID());try{success=BonfireTravel.travelToBonfire(p,destination.level,destination.position);}finally{TRANSIT.remove(p.getUUID());}
        if(!success){p.displayClientMessage(tr("unavailable"),true);close(p,true);use(p,origin.pos());return;}
        SparkManager.charge(p,cost);
        previous.playSound(null,from.x,from.y,from.z,SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);
        destination.level.playSound(null,p.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);
        // Revoke the old token without sending an intermediate close packet after the new menu opens.
        SESSIONS.remove(p.getUUID());use(p,destination.point.pos(),true);
        if(session(p)==null){var tag=new CompoundTag();tag.putInt("mode",-1);ModNetworking.skintSnapshot(p,tag);}
    }
    public static double manaMultiplier(ServerPlayer p){
        if(!p.isAlive())return 1;var level=p.serverLevel();var center=p.blockPosition();
        for(var pos:BlockPos.betweenClosed(center.offset(-3,-3,-3),center.offset(3,3,3))){
            if(p.position().distanceToSqr(Vec3.atCenterOf(pos))>9+1e-8||!level.hasChunkAt(pos))continue;
            var state=level.getBlockState(pos);if(TravelSkintBlock.active(level,pos)&&state.canSurvive(level,pos))return 2;
        }return 1;
    }
    public static void close(ServerPlayer p,boolean notify){
        PENDING.remove(p.getUUID());SkintSeating.stand(p);
        if(SESSIONS.remove(p.getUUID())!=null&&notify){var tag=new CompoundTag();tag.putInt("mode",-1);ModNetworking.skintSnapshot(p,tag);}
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer p){var pending=PENDING.get(p.getUUID());
            if(pending!=null){
                if(!validDeparture(p,pending))close(p,true);
                else if(pending.fadeBegan>=0&&p.serverLevel().getGameTime()-pending.fadeBegan>200){
                    PENDING.remove(p.getUUID());((SkintSeat)p.getVehicle()).resumeSitting();snapshot(p,0);
                }
            }
            else if(session(p)!=null&&!canProgress(p))close(p,true);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)close(p,false);}
    @SubscribeEvent public static void chunkSent(net.neoforged.neoforge.event.level.ChunkWatchEvent.Sent event){SkintNetwork.get(event.getLevel()).refreshLight(event.getLevel(),event.getPos());}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){SESSIONS.clear();PENDING.clear();TRANSIT.clear();}
    public static Component tr(String key,Object...args){return Component.translatable("rpgprogression.skint."+key,args);}
    private SkintManager(){}
}
