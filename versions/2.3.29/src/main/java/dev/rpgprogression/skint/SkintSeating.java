package dev.rpgprogression.skint;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class SkintSeating {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"rpgprogression");
    public static final DeferredHolder<EntityType<?>,EntityType<SkintSeat>> SEAT=ENTITIES.register("skint_seat",()->EntityType.Builder.<SkintSeat>of(SkintSeat::new,MobCategory.MISC)
        .sized(.01f,.01f).noSave().noSummon().fireImmune().clientTrackingRange(8).updateInterval(1).build("rpgprogression:skint_seat"));
    public static void register(IEventBus bus){ENTITIES.register(bus);}
    public static boolean seated(ServerPlayer p,BlockPos skint){return p.getVehicle() instanceof SkintSeat seat&&!seat.standing()&&seat.belongsTo(p,skint)&&safe(p,seat.position());}
    public static boolean sit(ServerPlayer p,BlockPos skint){
        if(p.getVehicle() instanceof SkintSeat current&&current.belongsTo(p,skint)&&safe(p,current.position())){current.resumeSitting();return true;}
        var location=sittingLocation(p,skint);if(location.isEmpty())return false;
        stand(p);if(p.isPassenger())p.stopRiding();
        var seat=SEAT.get().create(p.serverLevel());if(seat==null)return false;
        seat.configure(p,skint,location.get());if(!p.serverLevel().addFreshEntity(seat))return false;
        p.setDeltaMovement(Vec3.ZERO);p.setShiftKeyDown(false);p.fallDistance=0;
        if(!p.startRiding(seat,true)){seat.discard();return false;}
        return true;
    }
    public static void stand(ServerPlayer p){
        if(p.getVehicle() instanceof SkintSeat seat){p.stopRiding();seat.discard();p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
            if(!p.isRemoved())p.connection.teleport(p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot());}
    }
    private static Optional<Vec3> sittingLocation(ServerPlayer p,BlockPos point){
        return sittingLocation(p,p.serverLevel(),point,p.position());
    }
    static Optional<Vec3> sittingLocation(ServerPlayer p,ServerLevel level,BlockPos point,Vec3 current){
        var center=Vec3.atBottomCenterOf(point);double horizontal=current.subtract(center).multiply(1,0,1).length();
        if(Math.abs(current.y-center.y)<.15&&horizontal>=1.4&&horizontal<=2.7&&safe(p,level,current))return Optional.of(current);
        var direction=current.subtract(center).multiply(1,0,1);if(direction.lengthSqr()<.01)direction=new Vec3(0,0,1);direction=direction.normalize();
        for(double radius:new double[]{1.65,2.0,2.4})for(int degrees:new int[]{0,45,-45,90,-90,135,-135,180}){
            double angle=Math.toRadians(degrees);var rotated=new Vec3(direction.x*Math.cos(angle)-direction.z*Math.sin(angle),0,direction.x*Math.sin(angle)+direction.z*Math.cos(angle));
            var candidate=center.add(rotated.scale(radius));if(safe(p,level,candidate))return Optional.of(candidate);
        }
        return Optional.empty();
    }
    public static Vec3 standLocation(ServerPlayer p,Vec3 around){
        if(safe(p,around))return around;
        var base=BlockPos.containing(around);
        for(int y:new int[]{0,1,-1,2,-2})for(int radius=1;radius<=3;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;
            var candidate=Vec3.atBottomCenterOf(base.offset(x,y,z));if(safe(p,candidate))return candidate;
        }
        return around;
    }
    private static boolean hazard(net.minecraft.world.level.block.state.BlockState state){return state.is(BlockTags.FIRE)||state.is(BlockTags.CAMPFIRES)||state.is(Blocks.MAGMA_BLOCK)||state.is(Blocks.CACTUS)||state.is(Blocks.POWDER_SNOW)||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.WITHER_ROSE);}
    static boolean safe(ServerPlayer p,Vec3 location){
        return safe(p,p.serverLevel(),location);
    }
    private static boolean safe(ServerPlayer p,ServerLevel level,Vec3 location){
        var feet=BlockPos.containing(location);if(level.isOutsideBuildHeight(feet)||level.isOutsideBuildHeight(feet.above(2))||!level.hasChunkAt(feet))return false;
        var floor=level.getBlockState(feet.below());if(!floor.isFaceSturdy(level,feet.below(),Direction.UP)||hazard(floor))return false;
        var box=p.getDimensions(Pose.STANDING).makeBoundingBox(location);
        if(!level.getWorldBorder().isWithinBounds(box)||!level.noCollision(p,box)||!SkintSafety.fluidsSafe(p,level,box))return false;
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(hazard(level.getBlockState(pos)))return false;
        // Reserve space in front of the hips for the seated legs as well as room to stand back up.
        var legs=new AABB(location.x-.6,location.y,location.z-.6,location.x+.6,location.y+.6,location.z+.6);
        return level.noCollision(p,legs)&&SkintSafety.fluidsSafe(p,level,legs);
    }
    private SkintSeating(){}
}
