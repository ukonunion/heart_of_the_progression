package dev.rpgprogression.verification;

import com.google.gson.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.xp.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;

@EventBusSubscriber(modid="rpgprogression")
public final class ProgressionExperienceChecks {
    public static void check(JsonArray checks,boolean ok,String text){SkintTravelChecks.check(checks,ok,text);}
    private static java.util.List<ProgressionOrb> orbs(ServerPlayer p){return p.serverLevel().getEntitiesOfClass(ProgressionOrb.class,new AABB(100,150,100,220,300,220),e->!e.isRemoved());}
    private static void clear(ServerPlayer p){orbs(p).forEach(Entity::discard);}
    public static void run(ServerPlayer p,JsonArray checks)throws Exception {
        ProgressionOrbChecks.run(p,checks);
        var saved=ProgressionManager.get(p).save();var mode=p.gameMode.getGameModeForPlayer();var pos=p.position();boolean creative=RPGConfig.CREATIVE_XP.get();
        try{
            p.connection.teleport(160,220,160,0,0);p.setGameMode(GameType.CREATIVE);RPGConfig.CREATIVE_XP.set(false);clear(p);
            var victim=EntityType.ZOMBIE.create(p.serverLevel());victim.setPos(163,220,160);long before=ProgressionManager.get(p).xp;
            XPManager.killed(p,victim);check(checks,!orbs(p).isEmpty()&&ProgressionManager.get(p).xp==before,"creative kills drop RPG stars even with creativeGainsXP=false, without immediate credit");clear(p);
            p.serverLevel().addFreshEntity(victim);victim.hurt(p.damageSources().playerAttack(p),10000);
            check(checks,victim.isDeadOrDying()&&!orbs(p).isEmpty(),"a real creative LivingDeathEvent drops progression stars");victim.discard();clear(p);
            p.setGameMode(GameType.SPECTATOR);XPManager.killed(p,victim);check(checks,orbs(p).isEmpty(),"spectator kills still do not create rewards");p.setGameMode(GameType.SURVIVAL);
            var age=ProgressionOrb.class.getDeclaredField("age");age.setAccessible(true);var delay=ProgressionOrb.class.getDeclaredField("pickupDelay");delay.setAccessible(true);var target=ProgressionOrb.class.getDeclaredField("target");target.setAccessible(true);
            ProgressionOrb.award(p.serverLevel(),p.position().add(12,0,0),7);var distant=orbs(p).getFirst();age.setInt(distant,21);delay.setInt(distant,0);target.set(distant,p);distant.setNoGravity(true);distant.setDeltaMovement(Vec3.ZERO);double x=distant.getX();distant.tick();
            check(checks,distant.getX()<x&&distant.getDeltaMovement().horizontalDistance()>0&&distant.getDeltaMovement().horizontalDistance()<.02,"stars twelve blocks away move toward the player with weak attraction");
            distant.setPos(p.position().add(17,0,0));distant.setDeltaMovement(Vec3.ZERO);target.set(distant,p);age.setInt(distant,21);distant.tick();
            check(checks,distant.getDeltaMovement().horizontalDistance()==0&&target.get(distant)==null,"stars beyond the sixteen-block attraction radius remain unclaimed");
            distant.setPos(p.position().add(12,0,0));distant.setDeltaMovement(1,0,0);target.set(distant,p);age.setInt(distant,21);distant.tick();check(checks,distant.getDeltaMovement().horizontalDistance()<=.16,"the previous slow horizontal speed cap is preserved");clear(p);
        }finally{clear(p);RPGConfig.CREATIVE_XP.set(creative);ProgressionManager.get(p).load(saved);p.setGameMode(mode);p.connection.teleport(pos.x,pos.y,pos.z,180,0);}
    }
    public static void report(Path file,JsonArray checks,Throwable error)throws Exception {
        var json=new JsonObject();json.addProperty("version","2.3.28");json.addProperty("result",error==null&&checks.asList().stream().allMatch(x->x.getAsJsonObject().get("passed").getAsBoolean())?"PASSED":"FAILED");if(error!=null)json.addProperty("error",error.toString());json.add("checks",checks);Files.createDirectories(file.toAbsolutePath().getParent());Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(json));
    }
    @SubscribeEvent public static void server(ServerStartedEvent e){
        if(!Boolean.getBoolean("rpgprogression.experienceServerSmoke"))return;var server=e.getServer();server.execute(()->{var checks=new JsonArray();Throwable failure=null;try{run(ConfigurationChecks.player(server.overworld()),checks);}catch(Throwable t){failure=t;com.mojang.logging.LogUtils.getLogger().error("EXPERIENCE_SERVER",t);}try{report(Path.of("experience-2.3.28-result.json"),checks,failure);}catch(Exception t){throw new IllegalStateException(t);}server.halt(false);});
    }
    private ProgressionExperienceChecks(){}
}
