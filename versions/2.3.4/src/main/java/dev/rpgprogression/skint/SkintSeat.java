package dev.rpgprogression.skint;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Invisible, nonpersistent carrier: vanilla and Epic Fight both use their native seated pose. */
public final class SkintSeat extends Entity {
    private UUID owner;
    private BlockPos skint;
    public SkintSeat(EntityType<? extends SkintSeat> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);setInvisible(true);}
    public void configure(ServerPlayer player,BlockPos source,Vec3 feet){owner=player.getUUID();skint=source.immutable();setPos(feet);setYRot(player.getYRot());}
    public boolean belongsTo(ServerPlayer p,BlockPos point){return !isRemoved()&&p.getUUID().equals(owner)&&point.equals(skint);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
    @Override protected void readAdditionalSaveData(CompoundTag tag){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public boolean shouldBeSaved(){return false;}
    @Override public boolean isPickable(){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public Vec3 getPassengerRidingPosition(Entity passenger){return position();}
    @Override protected boolean canAddPassenger(Entity passenger){return getPassengers().isEmpty()&&passenger.getUUID().equals(owner);}
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){
        // Never drag a player back after an external teleport or dimension change.
        if(passenger.level()!=level()||passenger.position().distanceToSqr(position())>16)return passenger.position();
        return passenger instanceof ServerPlayer player?SkintSeating.standLocation(player,position()):position();
    }
    @Override public void tick(){
        super.tick();setDeltaMovement(Vec3.ZERO);if(level().isClientSide)return;
        if(getFirstPassenger() instanceof ServerPlayer player){
            var session=SkintManager.session(player);
            if(session==null||!belongsTo(player,session.origin().pos())||!SkintManager.canProgress(player))SkintManager.close(player,true);
        }else discard();
    }
}
