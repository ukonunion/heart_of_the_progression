package dev.rpgprogression.verification.client;

import com.google.gson.*;
import dev.rpgprogression.block.*;
import dev.rpgprogression.client.TravelSkintRenderer;
import dev.rpgprogression.client.gui.SkintScreen;
import dev.rpgprogression.skint.*;
import dev.rpgprogression.verification.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class SkintActivationClientSmoke {
    private static final BlockPos POS=new BlockPos(102,200,-16);
    private static final JsonArray checks=new JsonArray(),trace=new JsonArray();
    private static boolean loading,done;private static int stage,age,boot;private static CompletableFuture<?> pending;
    private static String capture;private static int shot;
    private static final float[] TIMES={.15f,.65f,1.05f};
    private static boolean enabled(){return Boolean.getBoolean("rpgprogression.skintActivationClientSmoke")&&!done;}
    private static Path directory(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/skint-2.3.16/activation");}
    private static void check(boolean value,String name){SkintTravelChecks.check(checks,value,name);}
    private static void use(Minecraft mc){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(POS),Direction.SOUTH,POS,false));}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!enabled())return;var mc=Minecraft.getInstance();
        try{
            if(mc.player==null||mc.getSingleplayerServer()==null){
                if(++boot>6000)throw new IllegalStateException("Startup timeout");
                if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var previous=mc.screen;mc.createWorldOpenFlows().openWorld(System.getProperty("rpgprogression.smokeWorld"),()->mc.setScreen(previous));}return;
            }
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(mc.getOverlay()!=null)return;age++;if(age>400)throw new IllegalStateException("Activation stage timeout "+stage);
            if(stage==0){
                if(age<60)return;mc.setScreen(null);mc.options.guiScale().set(2);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=true;mc.resizeDisplay();
                pending=CompletableFuture.runAsync(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var level=p.serverLevel();SkintActivationChecks.run(p,checks);
                    SkintActivationChecks.inactive(level,POS);SkintActivationChecks.inactive(level,POS.offset(12,0,0));
                    level.setDayTime(1000);level.setWeatherParameters(10000,0,false,false);p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();p.removeAllEffects();p.connection.teleport(POS.getX()+.5,POS.getY(),POS.getZ()+4.5,180,15);
                },mc.getSingleplayerServer());stage=1;age=0;return;
            }
            if(!(mc.level.getBlockEntity(POS) instanceof TravelSkintBlockEntity entity))return;
            if(stage==1&&age>40){
                check(!TravelSkintBlock.active(entity.getBlockState())&&entity.animationSeconds(0)==0,"the real client initially receives the inactive pose");
                check(mc.getBlockEntityRenderDispatcher().getRenderer(entity) instanceof TravelSkintRenderer,"native renderer dispatch selects the animated skint renderer");
                capture="01-inactive.png";stage=2;age=0;return;
            }
            if(stage==2&&capture==null&&age>4){use(mc);stage=3;age=0;return;}
            if(stage==3){
                float seconds=entity.animationSeconds(0);var row=new JsonObject();row.addProperty("tick",age);row.addProperty("seconds",seconds);row.addProperty("active",TravelSkintBlock.active(entity.getBlockState()));trace.add(row);
                check(mc.screen==null&&!mc.player.isPassenger(),"activation tick "+age+" keeps the hub closed and player standing");
                if(shot<TIMES.length&&seconds>=TIMES[shot]&&capture==null){capture="0"+(shot+2)+"-activating.png";shot++;}
                if(TravelSkintBlock.active(entity.getBlockState())&&age>60&&capture==null){
                    check(shot==3&&trace.size()>20,"native client observes intermediate animation poses before activation completes");
                    check(seconds==1.25f&&mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,POS)==15,"native client holds the active endpoint with light level 15");
                    capture="05-active.png";stage=4;age=0;
                }return;
            }
            if(stage==4&&capture==null&&age>4){use(mc);stage=5;age=0;return;}
            if(stage==5&&mc.screen instanceof SkintScreen&&SkintCameraReady()&&age>35){
                check(mc.player.getVehicle() instanceof SkintSeat,"a second actual right-click opens the usual seated hub");capture="06-activated-menu.png";stage=6;age=0;return;
            }
            if(stage==6&&capture==null&&age>4){mc.screen.onClose();stage=7;age=0;return;}
            if(stage==7&&age>45){
                check(mc.screen==null&&!mc.player.isPassenger(),"normal exit still stands up after activating a new skint");
                var report=new JsonObject();report.addProperty("version","2.3.16");report.addProperty("result",checks.asList().stream().allMatch(c->c.getAsJsonObject().get("passed").getAsBoolean())?"PASSED":"FAILED");report.add("checks",checks);report.add("trace",trace);
                Files.createDirectories(directory(mc));Files.writeString(directory(mc).resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));done=true;if(Boolean.getBoolean("rpgprogression.skintActivationOnly"))mc.stop();else System.setProperty("rpgprogression.skintChargeClientSmoke","true");
            }
        }catch(Throwable error){done=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_ACTIVATION_CLIENT failed",error);mc.stop();}
    }
    private static boolean SkintCameraReady(){return dev.rpgprogression.client.SkintCamera.settled();}
    @SubscribeEvent public static void render(RenderFrameEvent.Post event){
        if(!enabled()||capture==null)return;var mc=Minecraft.getInstance();
        try{Files.createDirectories(directory(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(directory(mc).resolve(capture));}capture=null;}
        catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("SKINT_ACTIVATION_CLIENT capture failed",error);}
    }
    @SubscribeEvent public static void frameStart(RenderFrameEvent.Pre event){if(enabled())Minecraft.getInstance().mouseHandler.releaseMouse();}
    private SkintActivationClientSmoke(){}
}
