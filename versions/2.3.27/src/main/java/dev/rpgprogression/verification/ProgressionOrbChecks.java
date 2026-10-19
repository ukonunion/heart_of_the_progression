package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.xp.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

public final class ProgressionOrbChecks {
    private static void check(JsonArray checks,boolean ok,String text){SkintTravelChecks.check(checks,ok,text);}
    private static List<ProgressionOrb> orbs(ServerPlayer p){return p.serverLevel().getEntitiesOfClass(ProgressionOrb.class,new AABB(-300,-100,-300,300,400,300),e->!e.isRemoved());}
    private static void clear(ServerPlayer p){for(var orb:orbs(p))orb.discard();}
    public static void run(ServerPlayer p,JsonArray checks)throws Exception {
        var d=ProgressionManager.get(p);var saved=d.save();var position=p.position();var mode=p.gameMode.getGameModeForPlayer();
        var overrides=RPGConfig.COMBAT_OVERRIDES.get();double multiplier=RPGConfig.COMBAT_XP_MULTIPLIER.get();
        try{
            // A detached verifier player does not keep chunks entity-accessible like a connected player.
            var entityManager=net.minecraft.server.level.ServerLevel.class.getDeclaredField("entityManager");entityManager.setAccessible(true);
            @SuppressWarnings("unchecked") var manager=(net.minecraft.world.level.entity.PersistentEntitySectionManager<Entity>)entityManager.get(p.serverLevel());
            for(var chunk:List.of(new ChunkPos(8,8),new ChunkPos(10,10),new ChunkPos(11,10))){p.serverLevel().getChunk(chunk.x,chunk.z);manager.updateChunkStatus(chunk,net.minecraft.world.level.entity.Visibility.TRACKED);}
            p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.setNoGravity(true);p.connection.teleport(128.5,220,128.5,180,0);p.serverLevel().getChunk(new BlockPos(128,220,128));clear(p);
            RPGConfig.COMBAT_OVERRIDES.set(List.of("minecraft:zombie=20"));RPGConfig.COMBAT_XP_MULTIPLIER.set(1.0);
            var victim=EntityType.ZOMBIE.create(p.serverLevel());victim.setPos(132,220,128);d.level=1;d.xp=0;long before=d.xp;
            XPManager.killed(p,victim);var spawned=orbs(p);long reward=spawned.stream().mapToLong(ProgressionOrb::value).sum();
            com.mojang.logging.LogUtils.getLogger().info("ORB_FIXTURE xp={} before={} orbs={} sum={} expectedCentre={} creative={}",d.xp,before,spawned.size(),reward,XPManager.mobReward(victim,.5),p.isCreative());
            check(checks,d.xp==before&&!spawned.isEmpty()&&reward>=15&&reward<=25,"a mob kill drops the full varied RPG reward without immediately crediting the player");
            check(checks,spawned.size()<=12&&spawned.stream().allMatch(o->o.distanceToSqr(victim)<1),"RPG experience appears at the mob and each kill spawns at most twelve stars");
            int vanillaBefore=p.totalExperience;for(var orb:spawned){orb.playerTouch(p);}
            check(checks,d.xp==before,"fresh orbs cannot be picked up during the one-second delay or from a distance");
            p.connection.teleport(160,220,160,180,0);for(int i=0;i<50;i++)for(var orb:spawned)if(!orb.isRemoved())orb.tick();
            check(checks,d.xp==before,"mature dropped experience does not remotely credit an absent killer");
            for(var orb:spawned)if(!orb.isRemoved()){orb.setPos(p.position());orb.playerTouch(p);orb.playerTouch(p);}
            com.mojang.logging.LogUtils.getLogger().info("ORB_PICKUP expected={} actual={} remaining={}",reward,d.xp,spawned.stream().filter(o->!o.isRemoved()).map(ProgressionOrb::value).toList());
            check(checks,d.xp==reward&&spawned.stream().allMatch(Entity::isRemoved),"each picked-up RPG orb credits its exact value once and disappears");
            check(checks,p.totalExperience==vanillaBefore,"picking up RPG stars never adds vanilla XP or invokes Mending");
            clear(p);ProgressionOrb.award(p.serverLevel(),p.position().add(4,0,0),1000);var original=orbs(p).getFirst();
            var tag=new CompoundTag();original.saveWithoutId(tag);var copy=ProgressionOrb.TYPE.get().create(p.serverLevel());copy.load(tag);
            var second=new CompoundTag();copy.saveWithoutId(second);
            check(checks,copy.value()==original.value()&&second.getInt("PickupDelay")==tag.getInt("PickupDelay")&&second.getInt("Age")==tag.getInt("Age"),"long RPG value, pickup delay and lifetime round-trip through native entity NBT");
            original.discard();copy.discard();clear(p);
            ProgressionOrb.award(p.serverLevel(),p.position().add(20,0,0),ProgressionMath.XP_LIMIT);var large=orbs(p);
            check(checks,large.size()==12&&large.stream().mapToLong(ProgressionOrb::value).sum()==ProgressionMath.XP_LIMIT,"even the maximum configured reward is conserved using only twelve entities");clear(p);
            var point=p.position().add(4,0,0);ProgressionOrb.award(p.serverLevel(),point,7);ProgressionOrb.award(p.serverLevel(),point,17);var merge=orbs(p);long total=merge.stream().mapToLong(ProgressionOrb::value).sum();
            var field=ProgressionOrb.class.getDeclaredField("age");field.setAccessible(true);for(var orb:merge){field.setInt(orb,19);orb.setDeltaMovement(Vec3.ZERO);}merge.getFirst().tick();
            check(checks,orbs(p).size()==1&&orbs(p).getFirst().value()==total,"nearby RPG stars merge without losing experience or merging with vanilla orbs");clear(p);
            ProgressionOrb.award(p.serverLevel(),p.position(),7);var rejected=orbs(p).getFirst();var delay=ProgressionOrb.class.getDeclaredField("pickupDelay");delay.setAccessible(true);delay.setInt(rejected,0);
            var restricted=ConfigurationChecks.player(p.serverLevel());restricted.setPos(p.position());restricted.setGameMode(GameType.SPECTATOR);long savedXP=d.xp;rejected.playerTouch(restricted);check(checks,!rejected.isRemoved()&&d.xp==savedXP,"spectators cannot consume dropped progression experience");
            restricted.setGameMode(GameType.CREATIVE);boolean creative=RPGConfig.CREATIVE_XP.get();RPGConfig.CREATIVE_XP.set(false);rejected.playerTouch(restricted);check(checks,!rejected.isRemoved()&&d.xp==savedXP,"creative XP restrictions leave orbs available for eligible players");RPGConfig.CREATIVE_XP.set(creative);clear(p);
            ProgressionOrb.award(p.serverLevel(),p.position().add(20,0,0),1);var expired=orbs(p).getFirst();field.setInt(expired,5999);expired.tick();check(checks,expired.isRemoved(),"uncollected stars expire after the vanilla five-minute lifetime");clear(p);
            ProgressionOrb.award(p.serverLevel(),p.position().add(4,0,0),7);var moving=orbs(p).getFirst();delay.setInt(moving,0);moving.setNoGravity(true);moving.setDeltaMovement(Vec3.ZERO);field.setInt(moving,21);
            var target=ProgressionOrb.class.getDeclaredField("target");target.setAccessible(true);target.set(moving,p);moving.tick();
            check(checks,moving.getDeltaMovement().horizontalDistance()>0&&moving.getDeltaMovement().horizontalDistance()<.02&&ProgressionOrbMath.attraction(4)<.025,"new stars use much weaker attraction than vanilla XP");clear(p);
            ProgressionOrb.award(p.serverLevel(),p.position(),0);check(checks,orbs(p).isEmpty(),"zero rewards create no empty experience entities");
            var actual=EntityType.ZOMBIE.create(p.serverLevel());actual.setPos(p.position().add(3,0,0));actual.setNoAi(true);
            manager.updateChunkStatus(new ChunkPos(actual.blockPosition()),net.minecraft.world.level.entity.Visibility.TRACKED);
            p.serverLevel().addFreshEntity(actual);long xpBeforeKill=d.xp;
            actual.hurt(p.damageSources().playerAttack(p),10000);
            // Epic Fight's death reaction can move the victim across a chunk boundary before drops spawn.
            manager.updateChunkStatus(new ChunkPos(actual.blockPosition()),net.minecraft.world.level.entity.Visibility.TRACKED);
            com.mojang.logging.LogUtils.getLogger().info("ORB_DEATH health={} dead={} drops={} xp={} before={} creative={}",actual.getHealth(),actual.isDeadOrDying(),orbs(p).size(),d.xp,xpBeforeKill,p.isCreative());
            SkintTravelChecks.check(checks,actual.isDeadOrDying()&&!orbs(p).isEmpty()&&d.xp==xpBeforeKill,"the real LivingDeathEvent spawns RPG stars without immediate progression credit");actual.discard();
        }finally{clear(p);RPGConfig.COMBAT_OVERRIDES.set(overrides);RPGConfig.COMBAT_XP_MULTIPLIER.set(multiplier);d.load(saved);p.setGameMode(mode);p.connection.teleport(position.x,position.y,position.z,180,0);}
    }
    public static void scene(ServerPlayer p){
        var base=new BlockPos(144,220,144);p.serverLevel().getChunk(base);for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)p.serverLevel().setBlock(base.offset(x,0,z),Blocks.DEEPSLATE_TILES.defaultBlockState(),3);
        p.serverLevel().setWeatherParameters(6000,0,false,false);p.serverLevel().setDayTime(18000);p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);
        p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setInvulnerable(true);p.setHealth(p.getMaxHealth());p.getInventory().clearContent();p.connection.teleport(144.5,221,148.5,180,13);
        clear(p);for(int i=0;i<6;i++)ProgressionOrb.award(p.serverLevel(),new Vec3(142.5+i*.7,221.7,144.5),17+i*4);
    }
    private ProgressionOrbChecks(){}
}
