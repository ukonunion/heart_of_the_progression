package dev.rpgprogression.verification.client;

import com.google.gson.*;
import dev.rpgprogression.client.SkintChargeParticle;
import dev.rpgprogression.effect.ModEffects;
import dev.rpgprogression.mob.SkintCharge;
import dev.rpgprogression.verification.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class SkintChargeClientSmoke {
    private static boolean loading,done,capture;
    private static int stage,age,boot,particleFrames,maxParticles;
    private static JsonArray checks=new JsonArray();
    private static CompletableFuture<?> pending;
    private static final Set<UUID> fixtures=new HashSet<>();
    private static Path directory(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/skint-2.3.19/charged");}
    private static boolean enabled(){return Boolean.getBoolean("rpgprogression.skintChargeClientSmoke")&&!done;}
    private static Object field(Object object,String name)throws Exception{var f=(object instanceof Class<?> c?c:object.getClass()).getDeclaredField(name);f.setAccessible(true);return f.get(object instanceof Class<?>?null:object);}
    private static int particles(Minecraft mc)throws Exception{
        int count=0;for(var queue:((Map<?,?>)field(mc.particleEngine,"particles")).values())for(var p:(Queue<?>)queue)if(p instanceof SkintChargeParticle)count++;
        return count;
    }
    private static void prepare(Minecraft mc){
        var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var level=p.serverLevel();
        checks=SkintChargeChecks.run(level,new BlockPos(85,190,5));ConfigurationChecks.run(ConfigurationChecks.player(level),checks);
        for(int x=62;x<=78;x++)for(int z=-2;z<=14;z++)for(int y=199;y<=207;y++)level.setBlock(new BlockPos(x,y,z),y==199?net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        level.setBlock(new BlockPos(70,200,7),dev.rpgprogression.block.ModBlocks.TRAVELSKINT.get().defaultBlockState().setValue(dev.rpgprogression.block.TravelSkintBlock.ACTIVE,true),3);
        level.setDayTime(18000);level.setWeatherParameters(10000,0,false,false);level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,level.getServer());
        for(var entity:level.getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(62,199,-2,79,208,15)))if(entity.getTags().contains("heart-charge-fixture"))entity.discard();
        int i=0;
        for(var type:new EntityType<?>[]{EntityType.ZOMBIE,EntityType.COW,EntityType.WITHER}){
            var mob=(Mob)type.create(level);mob.setNoAi(true);mob.setNoGravity(true);mob.setInvulnerable(true);mob.setPersistenceRequired();mob.addTag("heart-charge-fixture");
            mob.moveTo(i==0?68.5:i==1?72.5:70.5,i==2?202:200,i==2?2:7.5,0,0);SkintCharge.charge(mob);mob.getPersistentData().putBoolean(SkintCharge.CHECKED,true);
            level.addFreshEntity(mob);fixtures.add(mob.getUUID());i++;
        }
        var data=dev.rpgprogression.progression.ProgressionManager.get(p);data.spark=20;p.removeAllEffects();
        p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();p.connection.teleport(70.5,200,12.5,180,8);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!enabled())return;var mc=Minecraft.getInstance();
        try{
            if(mc.player==null||mc.getSingleplayerServer()==null){
                if(++boot>6000)throw new IllegalStateException("Client startup timeout");
                if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var screen=mc.screen;mc.createWorldOpenFlows().openWorld(System.getProperty("rpgprogression.smokeWorld"),()->mc.setScreen(screen));}return;
            }
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(mc.getOverlay()!=null)return;if(mc.screen!=null)mc.setScreen(null);age++;
            if(stage==0){
                if(age<60)return;
                mc.options.guiScale().set(2);mc.options.particles().set(ParticleStatus.ALL);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();
                pending=CompletableFuture.runAsync(()->prepare(mc),mc.getSingleplayerServer());stage=1;age=0;return;
            }
            if(stage==1){
                if(age<80)return;
                var accessor=(net.minecraft.network.syncher.EntityDataAccessor<java.util.List<net.minecraft.core.particles.ParticleOptions>>)field(LivingEntity.class,"DATA_EFFECT_PARTICLES");
                int synced=0;for(var mob:mc.level.entitiesForRendering())if(fixtures.contains(mob.getUUID())&&mob.getEntityData().get(accessor).contains(dev.rpgprogression.particle.ModParticles.SKINT_CHARGED.get()))synced++;
                SkintTravelChecks.check(checks,synced==3,"the client receives synchronized star emitters for a charged zombie, cow and wither");
                SkintTravelChecks.check(checks,particleFrames>=20&&maxParticles>=3,"the native client continuously renders custom star particles from synchronized effects; frames="+particleFrames+", peak="+maxParticles);
                capture=true;stage=2;age=0;return;
            }
            if(stage==2&&age>5&&!capture){
                var report=new JsonObject();report.addProperty("version","2.3.19");report.addProperty("result",checks.asList().stream().allMatch(c->c.getAsJsonObject().get("passed").getAsBoolean())?"PASSED":"FAILED");report.add("checks",checks);report.addProperty("particleFrames",particleFrames);report.addProperty("maxParticles",maxParticles);
                Files.createDirectories(directory(mc));Files.writeString(directory(mc).resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
                done=true;System.setProperty("rpgprogression.skintFlowSmoke","true");
            }
        }catch(Throwable error){done=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_CHARGE_CLIENT failed",error);mc.stop();}
    }
    @SubscribeEvent public static void render(RenderFrameEvent.Post event){
        if(!enabled())return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        try{
            int stars=particles(mc);if(stars>0)particleFrames++;maxParticles=Math.max(maxParticles,stars);
            if(capture){Files.createDirectories(directory(mc));try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels.writeToFile(directory(mc).resolve("01-charged-mobs.png"));}capture=false;}
        }catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("SKINT_CHARGE_CLIENT frame failed",error);}
    }
    @SubscribeEvent public static void frameStart(RenderFrameEvent.Pre event){if(enabled())Minecraft.getInstance().mouseHandler.releaseMouse();}
    private SkintChargeClientSmoke(){}
}
