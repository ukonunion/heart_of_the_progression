package dev.rpgprogression.skint;

import java.util.UUID;
import java.util.ArrayList;
import dev.rpgprogression.animation.SkintAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Invisible carrier with a synchronized, temporary animation timeline. */
public final class SkintSeat extends Entity {
    private static final EntityDataAccessor<Boolean> STANDING=SynchedEntityData.defineId(SkintSeat.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> BEGAN=SynchedEntityData.defineId(SkintSeat.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<CompoundTag> FROM=SynchedEntityData.defineId(SkintSeat.class,EntityDataSerializers.COMPOUND_TAG);
    private UUID owner;
    private BlockPos skint;
    public SkintSeat(EntityType<? extends SkintSeat> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);setInvisible(true);}
    public void configure(ServerPlayer player,BlockPos source,Vec3 feet){owner=player.getUUID();skint=source.immutable();setPos(feet);setYRot(player.getYRot());transition(false,SkintAnimation.SIT.sample(0));}
    public boolean belongsTo(ServerPlayer p,BlockPos point){return !isRemoved()&&p.getUUID().equals(owner)&&point.equals(skint);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(STANDING,false).define(BEGAN,0L).define(FROM,new CompoundTag());}
    public boolean standing(){return entityData.get(STANDING);}
    public double animationSeconds(float partial){return Math.max(0,(level().getGameTime()-entityData.get(BEGAN)+partial)/20.0);}
    public SkintAnimation.Pose animationPose(float partial){
        var rows=entityData.get(FROM).getList("pose",9);var bones=new ArrayList<SkintAnimation.Transform>();
        if(rows.size()!=SkintAnimation.BONES.size())return (standing()?SkintAnimation.STAND:SkintAnimation.SIT).sample(animationSeconds(partial));
        for(var entry:rows){var v=(ListTag)entry;bones.add(new SkintAnimation.Transform(v.getDouble(0),v.getDouble(1),v.getDouble(2),v.getDouble(3),v.getDouble(4),v.getDouble(5)));}
        return SkintAnimation.play(standing(),animationSeconds(partial),new SkintAnimation.Pose(bones));
    }
    private void transition(boolean standing,SkintAnimation.Pose from){
        var tag=new CompoundTag();var rows=new ListTag();for(var bone:from.bones()){var row=new ListTag();for(double value:bone.values())row.add(DoubleTag.valueOf(value));rows.add(row);}tag.put("pose",rows);
        entityData.set(FROM,tag);entityData.set(BEGAN,level().getGameTime());entityData.set(STANDING,standing);
    }
    public void beginStanding(){if(!standing())transition(true,animationPose(0));}
    public void resumeSitting(){if(standing())transition(false,animationPose(0));}
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
            if(standing()){
                if(!player.isAlive()||player.isRemoved()||player.level()!=level()||!SkintSeating.safe(player,position()))SkintManager.close(player,true);
                else if(animationSeconds(0)>=SkintAnimation.SECONDS)SkintManager.finishedStanding(player,this);
                return;
            }
            var session=SkintManager.session(player);
            if(session==null||!belongsTo(player,session.origin().pos())||!SkintManager.canProgress(player))SkintManager.close(player,true);
        }else discard();
    }
}
