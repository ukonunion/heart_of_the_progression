package dev.rpgprogression.xp;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.*;
import java.util.Comparator;

/** A saved, server-owned RPG reward. Vanilla XP and Mending never consume these orbs. */
public final class ProgressionOrb extends Entity {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"rpgprogression");
    public static final DeferredHolder<EntityType<?>,EntityType<ProgressionOrb>> TYPE=ENTITIES.register("progression_orb",()->EntityType.Builder.<ProgressionOrb>of(ProgressionOrb::new,MobCategory.MISC)
            .sized(.25f,.25f).noSummon().clientTrackingRange(8).updateInterval(3).build("rpgprogression:progression_orb"));
    private static final EntityDataAccessor<Long> VALUE=SynchedEntityData.defineId(ProgressionOrb.class,EntityDataSerializers.LONG);
    private int age,pickupDelay=20,health=5;
    private Player target;
    private int interpolationSteps;
    private double targetX,targetY,targetZ;
    public ProgressionOrb(EntityType<? extends ProgressionOrb> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(VALUE,0L);}
    public long value(){return entityData.get(VALUE);}
    private void value(long value){entityData.set(VALUE,Math.max(0,Math.min(ProgressionMath.XP_LIMIT,value)));}
    public static void award(ServerLevel level,Vec3 position,long amount){
        for(long value:ProgressionOrbMath.split(amount)){
            var orb=TYPE.get().create(level);if(orb==null)continue;
            orb.value(value);orb.setPos(position);orb.setDeltaMovement((orb.random.nextDouble()-.5)*.12,.07+orb.random.nextDouble()*.08,(orb.random.nextDouble()-.5)*.12);
            level.addFreshEntity(orb);
        }
    }
    private static boolean canCollect(Player player){return player.isAlive()&&!player.isSpectator()&&(!player.isCreative()||RPGConfig.CREATIVE_XP.get());}
    @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
    @Override public boolean isAttackable(){return false;}
    @Override public boolean isPickable(){return false;}
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps){targetX=x;targetY=y;targetZ=z;interpolationSteps=steps;}
    @Override public void tick(){
        super.tick();
        if(level().isClientSide){
            if(interpolationSteps>0){setPos(getX()+(targetX-getX())/interpolationSteps,getY()+(targetY-getY())/interpolationSteps,getZ()+(targetZ-getZ())/interpolationSteps);interpolationSteps--;}
            return;
        }
        if(value()<=0||++age>=6000){discard();return;}
        if(pickupDelay>0)pickupDelay--;
        if(target!=null&&(!canCollect(target)||target.level()!=level()||distanceToSqr(target)>ProgressionOrbMath.ATTRACTION_RANGE*ProgressionOrbMath.ATTRACTION_RANGE))target=null;
        if(age%5==1&&pickupDelay==0)target=level().players().stream().filter(ProgressionOrb::canCollect).filter(p->distanceToSqr(p)<ProgressionOrbMath.ATTRACTION_RANGE*ProgressionOrbMath.ATTRACTION_RANGE)
                .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if(!level().noCollision(getBoundingBox()))moveTowardsClosestSpace(getX(),getY()+.125,getZ());
        var movement=getDeltaMovement();
        if(isInWater())movement=movement.multiply(.96,.96,.96).add(0,.006,0);
        else if(level().getFluidState(blockPosition()).is(FluidTags.LAVA))movement=movement.multiply(.9,.9,.9).add(0,.02,0);
        else if(!isNoGravity())movement=movement.add(0,-.015,0);
        if(target!=null){
            var offset=target.position().add(0,.35,0).subtract(position());double distance=offset.length();
            if(distance>.05)movement=movement.add(offset.scale(ProgressionOrbMath.attraction(distance)/distance));
        }
        double horizontal=movement.horizontalDistance();if(horizontal>.16)movement=new Vec3(movement.x*.16/horizontal,movement.y,movement.z*.16/horizontal);
        movement=new Vec3(movement.x,Math.max(-.25,Math.min(.16,movement.y)),movement.z);
        setDeltaMovement(movement);move(MoverType.SELF,movement);
        double friction=onGround()?.65:.94;setDeltaMovement(movement.multiply(friction,.96,friction));
        if(onGround()&&movement.y<0)setDeltaMovement(getDeltaMovement().multiply(1,-.35,1));
        if(age%20==0)mergeNearby();
        if(target!=null&&distanceToSqr(target)<1.25)playerTouch(target);
    }
    private void mergeNearby(){
        for(var other:level().getEntitiesOfClass(ProgressionOrb.class,getBoundingBox().inflate(.5),o->o!=this&&!o.isRemoved()&&o.getId()>getId())){
            long incoming=other.value();if(incoming<=0||value()>ProgressionMath.XP_LIMIT-incoming)continue;
            value(value()+incoming);age=Math.min(age,other.age);pickupDelay=Math.max(pickupDelay,other.pickupDelay);other.value(0);other.discard();
        }
    }
    @Override public void playerTouch(Player player){
        if(!(player instanceof ServerPlayer server)||isRemoved()||pickupDelay>0||value()<=0||!canCollect(player)||distanceToSqr(player)>2.25)return;
        long reward=value();value(0);server.take(this,1);discard();ProgressionManager.addXP(server,reward);
        level().playSound(null,getX(),getY(),getZ(),SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.PLAYERS,.1f,.9f+random.nextFloat()*.2f);
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(amount<=0||isInvulnerableTo(source))return false;if(level().isClientSide)return true;
        health-=Math.max(1,(int)Math.ceil(amount));if(health<=0)discard();return true;
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putLong("Value",value());tag.putInt("Age",age);tag.putInt("PickupDelay",pickupDelay);tag.putInt("Health",health);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){value(tag.getLong("Value"));age=Math.max(0,Math.min(6000,tag.getInt("Age")));pickupDelay=Math.max(0,Math.min(20,tag.getInt("PickupDelay")));health=Math.max(1,Math.min(5,tag.getInt("Health")));}
}
