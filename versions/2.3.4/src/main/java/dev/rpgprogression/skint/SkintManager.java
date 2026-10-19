package dev.rpgprogression.skint;

import java.util.*;
import dev.rpgprogression.block.ModBlocks;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
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
    private static final Set<UUID> TRANSIT=new HashSet<>();
    public static boolean travelling(ServerPlayer p){return TRANSIT.contains(p.getUUID());}
    public static Session session(ServerPlayer p){return SESSIONS.get(p.getUUID());}
    public static boolean canProgress(ServerPlayer p){
        var session=session(p);if(session==null||p.isRemoved()||!p.isAlive()||p.isSpectator()||p.isSleeping())return false;
        var origin=session.origin();var level=p.serverLevel();
        return SkintSeating.seated(p,origin.pos())&&origin.dimension().equals(level.dimension())&&p.position().distanceToSqr(Vec3.atCenterOf(origin.pos()))<=36
            &&level.hasChunkAt(origin.pos())&&level.getBlockState(origin.pos()).is(ModBlocks.TRAVELSKINT.get())
            &&level.getBlockState(origin.pos()).canSurvive(level,origin.pos())&&origin.equals(SkintNetwork.get(level).point(origin.id()));
    }
    public static void use(ServerPlayer p,BlockPos pos){
        var level=p.serverLevel();if(!p.isAlive()||p.isSpectator()||p.isSleeping()||p.position().distanceToSqr(Vec3.atCenterOf(pos))>36
            ||!level.hasChunkAt(pos)||!level.getBlockState(pos).is(ModBlocks.TRAVELSKINT.get())||!level.getBlockState(pos).canSurvive(level,pos))return;
        var point=SkintNetwork.get(level).ensure(level,pos);var data=ProgressionManager.get(p);
        if(data.discoveredSkints.removeIf(id->SkintNetwork.get(level).point(id)==null))data.dirty=true;
        if(!data.discoveredSkints.contains(point.id())&&data.discoveredSkints.size()>=4096){p.displayClientMessage(tr("limit"),true);return;}
        if(data.discoveredSkints.add(point.id())){data.dirty=true;p.displayClientMessage(tr("activated"),true);}
        if(!SkintSeating.sit(p,pos)){close(p,true);p.displayClientMessage(tr("no_seat"),true);return;}
        SESSIONS.put(p.getUUID(),new Session(UUID.randomUUID(),point));snapshot(p,0);
    }
    public static void snapshot(ServerPlayer p,int mode){
        var session=session(p);if(session==null)return;var tag=new CompoundTag();tag.putInt("mode",mode);tag.putUUID("token",session.token());tag.put("origin",session.origin().tag());
        var entries=new ListTag();for(var point:SkintNetwork.get(p.serverLevel()).discovered(ProgressionManager.get(p).discoveredSkints))entries.add(point.tag());tag.put("points",entries);
        ModNetworking.skintSnapshot(p,tag);
    }
    public static void action(ServerPlayer p,int action,UUID token,UUID target){
        var session=session(p);if(session==null||!session.token().equals(token))return;
        if(action==2){close(p,false);return;}
        if(!canProgress(p)){close(p,true);return;}
        switch(action){
            case 0->{ModNetworking.sync(p);dev.rpgprogression.skill.SkillTreeManager.sync(p);snapshot(p,1);}
            case 1->travel(p,target);
            case 3->snapshot(p,0);
            default->{}
        }
    }
    public static boolean travel(ServerPlayer p,UUID id){
        if(!canProgress(p))return false;
        var data=ProgressionManager.get(p);var network=SkintNetwork.get(p.serverLevel());var destination=network.point(id);
        if(destination==null||!data.discoveredSkints.contains(id)||session(p).origin().id().equals(id)){p.displayClientMessage(tr("unavailable"),true);return false;}
        var level=p.getServer().getLevel(destination.dimension());
        if(level==null||!level.getWorldBorder().isWithinBounds(destination.pos())||level.isOutsideBuildHeight(destination.pos())){p.displayClientMessage(tr("unavailable"),true);return false;}
        level.getChunkAt(destination.pos());
        if(!level.getBlockState(destination.pos()).is(ModBlocks.TRAVELSKINT.get())||!level.getBlockState(destination.pos()).canSurvive(level,destination.pos())){
            network.remove(level,destination.pos());p.displayClientMessage(tr("unavailable"),true);snapshot(p,0);return false;
        }
        var safe=BonfireTravel.attemptToPlaceNextToBonfire(destination.pos(),level,p);
        if(safe.isEmpty()){p.displayClientMessage(tr("unsafe"),true);return false;}
        var previous=p.serverLevel();var from=p.position();boolean success;
        TRANSIT.add(p.getUUID());try{success=BonfireTravel.travelToBonfire(p,level,safe.get());}finally{TRANSIT.remove(p.getUUID());}
        if(!success){p.displayClientMessage(tr("unavailable"),true);return false;}
        previous.playSound(null,from.x,from.y,from.z,SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);
        level.playSound(null,p.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);
        close(p,true);return true;
    }
    public static double manaMultiplier(ServerPlayer p){
        if(!p.isAlive())return 1;var level=p.serverLevel();var center=p.blockPosition();
        for(var pos:BlockPos.betweenClosed(center.offset(-3,-3,-3),center.offset(3,3,3))){
            if(p.position().distanceToSqr(Vec3.atCenterOf(pos))>9+1e-8||!level.hasChunkAt(pos))continue;
            var state=level.getBlockState(pos);if(state.is(ModBlocks.TRAVELSKINT.get())&&state.canSurvive(level,pos))return 2;
        }return 1;
    }
    public static void close(ServerPlayer p,boolean notify){
        SkintSeating.stand(p);
        if(SESSIONS.remove(p.getUUID())!=null&&notify){var tag=new CompoundTag();tag.putInt("mode",-1);ModNetworking.skintSnapshot(p,tag);}
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(event.getEntity() instanceof ServerPlayer p&&session(p)!=null&&!canProgress(p))close(p,true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)close(p,false);}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){SESSIONS.clear();TRANSIT.clear();}
    public static Component tr(String key,Object...args){return Component.translatable("rpgprogression.skint."+key,args);}
    private SkintManager(){}
}
