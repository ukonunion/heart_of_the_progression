package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.block.*;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.skint.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class Skint2318Checks {
    private static void check(JsonArray c,boolean ok,String text){SkintTravelChecks.check(c,ok,text);}
    public static void wetPoint(ServerLevel level,BlockPos pos){
        SkintTravelChecks.point(level,pos);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            level.setBlock(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            for(int y=0;y<5;y++)if(x!=0||z!=0||y!=0)level.setBlock(pos.offset(x,y,z),Blocks.WATER.defaultBlockState(),2);
        }
        level.setBlock(pos,level.getBlockState(pos).setValue(TravelSkintBlock.WATERLOGGED,true),3);
    }
    public static void run(ServerLevel level,JsonArray c){
        var p=ConfigurationChecks.player(level);var wet=new BlockPos(600,190,150);var dry=wet.offset(20,0,0);long time=level.getGameTime();
        try{
            SkintManager.close(p,false);p.removeAllEffects();p.setNoGravity(true);wetPoint(level,wet);SkintTravelChecks.point(level,dry);p.setPos(Vec3.atBottomCenterOf(wet).add(0,0,2));
            check(c,!SkintSafety.canBreatheWater(p)&&BonfireTravel.attemptToPlaceNextToBonfire(wet,level,p).isEmpty()&&!SkintSeating.sit(p,wet),"water without either breathing effect rejects arrival and seating");
            p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,600));
            check(c,SkintSafety.canBreatheWater(p)&&BonfireTravel.attemptToPlaceNextToBonfire(wet,level,p).isPresent(),"water breathing permits a water-filled destination with a floor");
            SkintManager.use(p,wet);check(c,SkintManager.canProgress(p)&&p.getVehicle() instanceof SkintSeat,"water breathing permits a normal seated hub underwater");
            p.removeEffect(MobEffects.WATER_BREATHING);SkintManager.tick(new PlayerTickEvent.Post(p));
            check(c,SkintManager.session(p)==null&&!(p.getVehicle() instanceof SkintSeat),"expiration of breathing closes an underwater session and releases the seat");
            p.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER,600));p.setPos(Vec3.atBottomCenterOf(wet).add(0,0,2));SkintManager.use(p,wet);
            check(c,SkintSafety.canBreatheWater(p)&&SkintManager.canProgress(p)&&BonfireTravel.attemptToPlaceNextToBonfire(wet,level,p).isPresent(),"conduit power independently permits both underwater seating and arrival");
            var lavaPos=wet.offset(40,0,0);level.setBlock(lavaPos,Blocks.LAVA.defaultBlockState(),2);
            var box=p.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(Vec3.atBottomCenterOf(lavaPos));
            check(c,!SkintSafety.fluidsSafe(p,level,box),"conduit power does not make lava safe");
            SkintManager.close(p,false);p.removeAllEffects();p.setPos(Vec3.atBottomCenterOf(dry).add(0,0,2));SkintManager.use(p,dry);
            var d=ProgressionManager.get(p);d.discoveredSkints.add(SkintNetwork.get(level).pointAt(level,wet).id());d.spark=d.maxSpark();double before=d.spark;
            check(c,!SkintManager.travel(p,SkintNetwork.get(level).pointAt(level,wet).id())&&d.spark==before&&!SkintManager.pending(p),"a forged underwater route without breathing is rejected before standing or charging spark");
            p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,600));
            check(c,SkintManager.travel(p,SkintNetwork.get(level).pointAt(level,wet).id()),"the same discovered underwater route is accepted with water breathing");
            var token=SkintManager.session(p).token();((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time+30);((SkintSeat)p.getVehicle()).tick();
            p.removeEffect(MobEffects.WATER_BREATHING);SkintManager.action(p,4,token,new java.util.UUID(0,0));
            check(c,!SkintManager.pending(p)&&SkintManager.canProgress(p)&&d.spark==before&&p.position().distanceToSqr(Vec3.atCenterOf(dry))<36,"arrival rechecks breathing after standing/fade and cancels an unsafe transfer without spending spark");
            SkintManager.close(p,false);p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,600));
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=0;y<5;y++)if(x!=0||z!=0||y!=0)level.setBlock(wet.offset(x,y,z),Blocks.BEDROCK.defaultBlockState(),2);
            check(c,BonfireTravel.attemptToPlaceNextToBonfire(wet,level,p).isEmpty(),"breathing effects never bypass blocked standing space");
            var off=SkintActivationChecks.inactive(level,new BlockPos(640,190,150));
            check(c,off.beginActivation(p),"a server activation records its initiating player");
            var saved=off.saveWithFullMetadata(level.registryAccess());check(c,saved.hasUUID("activationOwner")&&saved.getUUID("activationOwner").equals(p.getUUID()),"the activation owner survives native NBT serialization");
            var copy=(TravelSkintBlockEntity)BlockEntity.loadStatic(off.getBlockPos(),off.getBlockState(),saved,level.registryAccess());
            check(c,copy.saveWithFullMetadata(level.registryAccess()).getUUID("activationOwner").equals(p.getUUID())&&!off.beginActivation(),"reload preserves the owner and repeated clicks cannot replace or restart activation");
            ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time+60);TravelSkintBlockEntity.tick(level,off.getBlockPos(),off.getBlockState(),off);
            check(c,TravelSkintBlock.active(off.getBlockState())&&!off.saveWithFullMetadata(level.registryAccess()).hasUUID("activationOwner"),"completion consumes the one-shot notification owner");
        }finally{SkintManager.close(p,false);p.removeAllEffects();((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time);}
    }
}
