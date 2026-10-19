package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.block.*;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.skint.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;

public final class SkintActivationChecks {
    public static final BlockPos SAVED_OFF=new BlockPos(130,190,30),SAVED_ACTIVE=SAVED_OFF.offset(12,0,0),SAVED_START=SAVED_OFF.offset(24,0,0);
    private static void check(JsonArray checks,boolean ok,String text){SkintTravelChecks.check(checks,ok,text);}
    public static TravelSkintBlockEntity inactive(ServerLevel level,BlockPos pos){
        SkintTravelChecks.point(level,pos);level.removeBlock(pos,false);level.setBlock(pos,ModBlocks.TRAVELSKINT.get().defaultBlockState(),3);
        var entity=(TravelSkintBlockEntity)level.getBlockEntity(pos);entity.onLoad();return entity;
    }
    public static void run(ServerPlayer p,JsonArray checks){
        var level=p.serverLevel();var pos=new BlockPos(110,190,10);long time=level.getGameTime();var oldPos=p.position();
        try{
            SkintManager.close(p,false);p.setPos(Vec3.atBottomCenterOf(pos).add(0,0,2));var entity=inactive(level,pos);var network=SkintNetwork.get(level);
            check(checks,!TravelSkintBlock.active(level.getBlockState(pos))&&level.getBlockState(pos).getLightEmission(level,pos)==0&&network.pointAt(level,pos)==null,"new skints start inactive, unlit and outside the travel network");
            SkintManager.use(p,pos);check(checks,SkintManager.session(p)==null&&!SkintManager.canProgress(p)&&SkintManager.manaMultiplier(p)==1,"inactive skints reject direct menu/progression requests and provide no mana aura");
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false);
            level.getBlockState(pos).useWithoutItem(level,p,hit);long began=entity.activationStarted();
            check(checks,began==time&&SkintManager.session(p)==null,"the real first right-click starts turn_on without opening the hub");
            ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time+8);level.getBlockState(pos).useWithoutItem(level,p,hit);
            check(checks,entity.activationStarted()==began&&!entity.beginActivation(),"repeated activation requests do not restart the shared clock");
            var nbt=entity.saveWithFullMetadata(level.registryAccess());var copy=(TravelSkintBlockEntity)BlockEntity.loadStatic(pos,level.getBlockState(pos),nbt,level.registryAccess());copy.setLevel(level);
            check(checks,copy.activationStarted()==began&&Math.abs(copy.animationSeconds(.5f)-.425f)<.0001,"block entity NBT and partial ticks preserve the in-progress animation clock");
            var update=entity.getUpdateTag(level.registryAccess());check(checks,update.getLong("activationStarted")==began&&update.getBoolean("activationInitialized")&&entity.getUpdatePacket()!=null,"tracking clients receive activation state through native chunk tags and update packets");
            ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time+24);TravelSkintBlockEntity.tick(level,pos,level.getBlockState(pos),entity);
            check(checks,!TravelSkintBlock.active(level.getBlockState(pos))&&network.pointAt(level,pos)==null,"24 ticks cannot enable a 25-tick activation early");
            ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time+25);TravelSkintBlockEntity.tick(level,pos,level.getBlockState(pos),entity);
            var point=network.pointAt(level,pos);
            check(checks,TravelSkintBlock.active(level.getBlockState(pos))&&point!=null&&entity.activationStarted()==-1&&entity.animationSeconds(0)==1.25f,"the full 1.25-second clip enables the block and registers exactly one point");
            check(checks,level.getBlockState(pos).getLightEmission(level,pos)==15&&SkintManager.manaMultiplier(p)==2,"activated skints regain light 15 and the double mana aura");
            SkintManager.use(p,pos);check(checks,SkintManager.canProgress(p)&&ProgressionManager.get(p).discoveredSkints.contains(point.id()),"the next click opens the normal seated hub and discovers the enabled point");
            var remote=inactive(level,pos.offset(12,0,0));var remotePoint=network.ensure(level,remote.getBlockPos());remote.loadWithComponents(new CompoundTag(),level.registryAccess());
            ProgressionManager.get(p).discoveredSkints.add(remotePoint.id());ProgressionManager.get(p).spark=25;
            check(checks,SkintManager.travel(p,remotePoint.id())&&TravelSkintBlock.active(remote.getBlockState())&&network.point(remotePoint.id())!=null,"remote arrival validation migrates an old skint before rejecting its newly loaded default state");SkintManager.close(p,false);
            p.setPos(Vec3.atBottomCenterOf(pos).add(0,0,2));SkintManager.use(p,pos);
            var disabled=inactive(level,pos.offset(24,0,0));var disabledPoint=network.ensure(level,disabled.getBlockPos());ProgressionManager.get(p).discoveredSkints.add(disabledPoint.id());double spark=ProgressionManager.get(p).spark;
            check(checks,!SkintManager.travel(p,disabledPoint.id())&&!SkintManager.pending(p)&&ProgressionManager.get(p).spark==spark&&!TravelSkintBlock.active(disabled.getBlockState()),"a forged discovered ID cannot teleport to a new inactive skint or auto-activate it");SkintManager.close(p,false);
            var saved=entity.saveWithFullMetadata(level.registryAccess());var activeCopy=(TravelSkintBlockEntity)BlockEntity.loadStatic(pos,level.getBlockState(pos),saved,level.registryAccess());
            check(checks,TravelSkintBlock.active(activeCopy.getBlockState())&&activeCopy.activationStarted()==-1,"completed activation survives the block entity save roundtrip");
            var removed=inactive(level,pos.offset(0,0,10));removed.beginActivation();level.removeBlock(removed.getBlockPos().below(),false);
            ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time+60);TravelSkintBlockEntity.tick(level,removed.getBlockPos(),level.getBlockState(removed.getBlockPos()),removed);
            check(checks,level.getBlockState(removed.getBlockPos()).isAir()&&network.pointAt(level,removed.getBlockPos())==null,"floor removal during activation cancels it without a ghost waypoint");
            var legacy=inactive(level,pos.offset(12,0,0));var legacyPoint=network.ensure(level,legacy.getBlockPos());legacy.loadWithComponents(new CompoundTag(),level.registryAccess());legacy.migrateLegacy();
            check(checks,TravelSkintBlock.active(legacy.getBlockState())&&network.pointAt(level,legacy.getBlockPos()).id().equals(legacyPoint.id()),"old registered skints migrate to active without changing their saved identity");
            var untouched=inactive(level,pos.offset(24,0,0));var tag=untouched.saveWithFullMetadata(level.registryAccess());untouched.loadWithComponents(tag,level.registryAccess());network.ensure(level,untouched.getBlockPos());untouched.migrateLegacy();
            check(checks,!TravelSkintBlock.active(untouched.getBlockState()),"new inactive NBT is never mistaken for a legacy active block");network.remove(level,untouched.getBlockPos());
        }finally{SkintManager.close(p,false);((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time);p.setPos(oldPos);}
    }
    public static void persist(ServerLevel level){
        inactive(level,SAVED_OFF);SkintTravelChecks.point(level,SAVED_ACTIVE);inactive(level,SAVED_START).beginActivation();
    }
    public static void restart(ServerLevel level,JsonArray checks){
        level.getChunkAt(SAVED_OFF);level.getChunkAt(SAVED_ACTIVE);level.getChunkAt(SAVED_START);
        var off=(TravelSkintBlockEntity)level.getBlockEntity(SAVED_OFF);var on=(TravelSkintBlockEntity)level.getBlockEntity(SAVED_ACTIVE);var started=(TravelSkintBlockEntity)level.getBlockEntity(SAVED_START);
        check(checks,off!=null&&!TravelSkintBlock.active(off.getBlockState())&&off.activationStarted()==-1,"full dedicated-server restart preserves an untouched inactive skint");
        check(checks,on!=null&&TravelSkintBlock.active(on.getBlockState())&&SkintNetwork.get(level).pointAt(level,SAVED_ACTIVE)!=null,"full dedicated-server restart preserves active skints and network identities");
        long before=level.getGameTime();
        check(checks,started!=null&&(started.activationStarted()>=0||TravelSkintBlock.active(started.getBlockState())),"full dedicated-server restart preserves an activation already in progress");
        if(started!=null&&!TravelSkintBlock.active(started.getBlockState())){
            ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(Math.max(before,started.activationStarted()+25));TravelSkintBlockEntity.tick(level,SAVED_START,started.getBlockState(),started);((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(before);
        }
        check(checks,TravelSkintBlock.active(level.getBlockState(SAVED_START)),"saved in-progress activation finishes without a second right-click");
    }
    private SkintActivationChecks(){}
}
