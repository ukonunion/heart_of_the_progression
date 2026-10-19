package dev.rpgprogression.verification.client;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import dev.rpgprogression.block.ModBlocks;
import dev.rpgprogression.client.*;
import dev.rpgprogression.client.gui.SkintScreen;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.skint.*;
import dev.rpgprogression.verification.SkintTravelChecks;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Native inputs and actual camera frames. Excluded from the production JAR. */
@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class SkintReturnClientSmoke {
    private record Pose(Vec3 position,float yaw,float pitch){}
    private record Frame(NativeImage pixels){}
    private static final JsonArray checks=new JsonArray();
    private static final JsonObject captures=new JsonObject(),motions=new JsonObject();
    private static final List<String> shots=new ArrayList<>();
    private static final List<Frame> frames=new ArrayList<>();
    private static JsonArray trace;
    private static String traceName;
    private static Pose normal,traceSource,lastPose;
    private static long traceBegan,closedAt;
    private static int boot,stage,age;
    private static boolean loading,creating,finished,failed,reopenPending,reopenVerified;
    private static CompletableFuture<?> pending,reload;
    private static BlockPos a;
    private static boolean enabled(){return Boolean.getBoolean("rpgprogression.skintReturnSmoke");}
    private static Path directory(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/skint-2.3.3/all");}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static Object field(Object object,String name)throws Exception{var f=object instanceof Class<?> c?c.getDeclaredField(name):object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object instanceof Class<?>?null:object);}
    private static void invoke(Object object,String name,Class<?>[] types,Object...args)throws Exception{var f=object.getClass().getDeclaredMethod(name,types);f.setAccessible(true);f.invoke(object,args);}
    private static Pose pose(Minecraft mc){var camera=mc.gameRenderer.getMainCamera();return new Pose(camera.getPosition(),camera.getYRot(),camera.getXRot());}
    private static boolean same(Pose a,Pose b){return a.position().distanceTo(b.position())<.02&&Math.abs(Mth.wrapDegrees(a.yaw()-b.yaw()))<.1&&Math.abs(a.pitch()-b.pitch())<.1;}
    private static void check(boolean ok,String text){SkintTravelChecks.check(checks,ok,text);if(!ok)failed=true;}
    private static void advance(){stage++;age=0;com.mojang.logging.LogUtils.getLogger().info("SKINT_RETURN_CLIENT stage {}",stage);}
    private static void server(Minecraft mc,Runnable action){pending=CompletableFuture.runAsync(action,mc.getSingleplayerServer());}
    private static void use(Minecraft mc){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(a),net.minecraft.core.Direction.SOUTH,a,false));}
    private static void exit(Minecraft mc){var screen=mc.screen;double scale=Math.min(screen.width/640.0,screen.height/360.0);double x=(screen.width-640*scale)/2+100*scale,y=(screen.height-360*scale)/2+186*scale;screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);}
    private static void escape(Minecraft mc){check(mc.screen.keyPressed(256,0,0),"the actual Escape key is handled by the hub");}
    private static void startTrace(Minecraft mc,String name){traceName=name;trace=new JsonArray();traceSource=pose(mc);traceBegan=System.nanoTime();}
    private static boolean hub(Minecraft mc){return mc.screen instanceof SkintScreen&&SkintCamera.settled()&&age>=24;}
    private static boolean returned(Minecraft mc){return !SkintCamera.active()&&!SkintCamera.returning()&&mc.screen==null;}
    private static void finish(Minecraft mc){
        if(finished)return;finished=true;
        try{var report=new JsonObject();report.addProperty("version","2.3.3");report.addProperty("result",failed?"FAILED":"PASSED");report.addProperty("world",System.getProperty("rpgprogression.smokeWorld"));report.add("checks",checks);report.add("screenshots",captures);report.add("motions",motions);
            var mods=new JsonArray();for(var mod:ModList.get().getMods()){var row=new JsonObject();row.addProperty("id",mod.getModId());row.addProperty("version",mod.getVersion().toString());mods.add(row);}report.add("loadedMods",mods);
            Files.createDirectories(directory(mc));Files.writeString(directory(mc).resolve("first-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("SKINT_RETURN_CLIENT report failed",error);}
        mc.options.hideGui=false;SkintCamera.end();mc.options.setCameraType(CameraType.FIRST_PERSON);com.mojang.logging.LogUtils.getLogger().info("SKINT_RETURN_CLIENT_RESULT {}",failed?"FAILED":"PASSED");mc.stop();
    }
    private static void flow(Minecraft mc)throws Exception{
        if(++boot>6000)throw new IllegalStateException("World startup timeout");String world=System.getProperty("rpgprogression.smokeWorld","HeartSkint233");
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var previous=mc.screen;
            if(Files.isDirectory(mc.gameDirectory.toPath().resolve("saves").resolve(world)))mc.createWorldOpenFlows().openWorld(world,()->mc.setScreen(previous));
            else{creating=true;CreateWorldScreen.openFresh(mc,mc.screen);}return;}
        if(creating&&mc.screen instanceof CreateWorldScreen){var state=field(mc.screen,"uiState");invoke(state,"setName",new Class[]{String.class},world);creating=false;invoke(mc.screen,"onCreate",new Class[]{});}
    }
    private static void prepare(Minecraft mc){
        var p=player(mc);a=p.blockPosition().offset(0,10,-2);SkintTravelChecks.point(p.serverLevel(),a);
        p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.getAbilities().invulnerable=true;p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.removeAllEffects();p.setDeltaMovement(Vec3.ZERO);
        p.connection.teleport(a.getX()+.5,a.getY(),a.getZ()+2.5,180,10);p.serverLevel().setDayTime(6000);p.serverLevel().setWeatherParameters(10000,0,false,false);ModNetworking.sync(p);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!enabled()||finished)return;var mc=Minecraft.getInstance();
        try{
            if(mc.player==null||mc.getSingleplayerServer()==null){flow(mc);return;}
            if(reload!=null){if(!reload.isDone())return;reload.join();reload=null;}
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(mc.getOverlay()!=null)return;if(mc.isPaused()){mc.setScreen(null);return;}
            if(++age>400)throw new IllegalStateException("Stage "+stage+" timed out, screen="+mc.screen);
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
            switch(stage){
                case 0->{mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.languageCode="ru_ru";mc.getLanguageManager().setSelected("ru_ru");reload=mc.reloadResourcePacks();mc.options.setCameraType(CameraType.FIRST_PERSON);server(mc,()->prepare(mc));advance();}
                case 1->{if(age<40)return;normal=pose(mc);use(mc);advance();}
                case 2->{if(!hub(mc))return;check(ClientSkintData.active(),"right-click still opens the authorized hub with the entry animation");shots.add("01-hub.png");advance();}
                case 3->{if(age<5)return;startTrace(mc,"exit-button");exit(mc);check(mc.screen==null&&!ClientSkintData.active()&&SkintCamera.returning(),"Exit closes the UI and revokes access immediately while the camera starts returning");advance();}
                case 4->{if(!returned(mc)||!motions.has("exit-button"))return;check(same(pose(mc),normal)&&mc.options.getCameraType()==CameraType.FIRST_PERSON,"Exit smoothly restores the original first-person position and angles");shots.add("02-after-exit.png");server(mc,()->check(SkintManager.session(player(mc))==null,"the server session is closed after the exit request"));advance();}
                case 5->{if(age<10)return;use(mc);advance();}
                case 6->{if(!hub(mc))return;startTrace(mc,"escape");escape(mc);check(mc.screen==null&&!ClientSkintData.active()&&SkintCamera.returning(),"Escape starts the same smooth return and closes authorization");advance();}
                case 7->{if(!returned(mc)||!motions.has("escape"))return;check(same(pose(mc),normal),"Escape restores the same original first-person view");shots.add("03-after-escape.png");advance();}
                case 8->{if(age<10)return;use(mc);advance();}
                case 9->{if(!hub(mc))return;exit(mc);closedAt=System.nanoTime();advance();}
                case 10->{if(System.nanoTime()-closedAt<200_000_000L)return;check(SkintCamera.returning(),"the camera is still returning when the player reopens the hub early");reopenPending=true;use(mc);advance();}
                case 11->{if(!hub(mc))return;check(reopenVerified,"rapid reopening continues from the last visible camera pose");shots.add("04-rapid-reopen.png");exit(mc);advance();}
                case 12->{if(!returned(mc))return;check(mc.options.getCameraType()==CameraType.FIRST_PERSON&&same(pose(mc),normal),"rapid reopening preserves the original perspective for the next return");mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);advance();}
                case 13->{if(age<10)return;normal=pose(mc);use(mc);advance();}
                case 14->{if(!hub(mc))return;escape(mc);check(SkintCamera.returning(),"the mirrored third-person view also uses a smooth return");advance();}
                case 15->{if(!returned(mc))return;check(mc.options.getCameraType()==CameraType.THIRD_PERSON_FRONT&&same(pose(mc),normal),"the original mirrored third-person position and orientation are restored");shots.add("05-mirrored-return.png");mc.options.setCameraType(CameraType.FIRST_PERSON);advance();}
                case 16->{if(age<10)return;normal=pose(mc);use(mc);advance();}
                case 17->{if(!(mc.screen instanceof SkintScreen)||age<2)return;check(!SkintCamera.settled(),"the early-exit case interrupts the entry animation");exit(mc);check(SkintCamera.returning(),"exiting during entry reverses smoothly from the current view");advance();}
                case 18->{if(!returned(mc))return;check(same(pose(mc),normal),"an interrupted entry returns to the exact normal view");use(mc);advance();}
                case 19->{if(!hub(mc))return;exit(mc);server(mc,()->{var p=player(mc);p.connection.teleport(p.getX()+1,p.getY(),p.getZ(),90,0);});advance();}
                case 20->{if(!returned(mc)||age<20)return;check(pose(mc).position().distanceTo(mc.player.getEyePosition())<.02&&Math.abs(Mth.wrapDegrees(pose(mc).yaw()-mc.player.getYRot()))<.1,"the return follows the live player destination after movement and rotation");server(mc,()->player(mc).connection.teleport(a.getX()+.5,a.getY(),a.getZ()+2.5,180,10));advance();}
                case 21->{if(age<20)return;use(mc);advance();}
                case 22->{if(!hub(mc))return;exit(mc);server(mc,()->{var p=player(mc);p.connection.teleport(p.getX()+16,p.getY(),p.getZ(),180,10);});advance();}
                case 23->{if(!returned(mc)||age<20)return;check(pose(mc).position().distanceTo(mc.player.getEyePosition())<.02&&mc.options.getCameraType()==CameraType.FIRST_PERSON,"a distant teleport cancels the return and immediately uses the new normal camera");shots.add("06-after-teleport-cancel.png");advance();}
                case 24->{if(age>10&&shots.isEmpty())finish(mc);}
                default->{}
            }
        }catch(Throwable error){failed=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_RETURN_CLIENT failed at stage "+stage,error);finish(mc);}
    }
    @SubscribeEvent public static void render(RenderFrameEvent.Post event){
        if(!enabled()||finished)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        try{
            if(reopenPending&&mc.screen instanceof SkintScreen){
                var source=(SkintCamera.View)field(SkintCamera.class,"start");reopenVerified=lastPose!=null&&source.position().distanceTo(lastPose.position())<.02;
                check(field(SkintCamera.class,"previous")==CameraType.FIRST_PERSON,"reopening remembers the actual original perspective instead of the temporary cinematic mode");reopenPending=false;
            }
            lastPose=pose(mc);
            if(traceName!=null){
                long elapsed=System.nanoTime()-traceBegan;var pose=pose(mc);var sample=new JsonObject();sample.addProperty("nanos",elapsed);sample.addProperty("x",pose.position().x);sample.addProperty("y",pose.position().y);sample.addProperty("z",pose.position().z);sample.addProperty("yaw",pose.yaw());sample.addProperty("pitch",pose.pitch());sample.addProperty("returning",SkintCamera.returning());trace.add(sample);
                try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){var small=new NativeImage(640,360,false);pixels.resizeSubRectTo(0,0,pixels.getWidth(),pixels.getHeight(),small);frames.add(new Frame(small));}
                if(!SkintCamera.active()){
                    var first=trace.get(0).getAsJsonObject();var firstPos=new Vec3(first.get("x").getAsDouble(),first.get("y").getAsDouble(),first.get("z").getAsDouble());double distance=traceSource.position().distanceTo(normal.position());
                    check(trace.size()>=4&&firstPos.distanceTo(traceSource.position())<distance*.35,traceName+": first return frame stays near the visible menu camera");
                    check(trace.asList().stream().map(t->t.getAsJsonObject().get("x").getAsDouble()).distinct().count()>=4,traceName+": several intermediate camera positions are actually rendered");
                    check(same(pose,normal),traceName+": the final rendered frame matches the original normal camera");
                    check(elapsed>=SkintCamera.TRANSITION_NANOS&&elapsed<SkintCamera.TRANSITION_NANOS+600_000_000L,traceName+": the visual return completes in approximately 0.7 seconds");
                    var folder=directory(mc).resolve(traceName);Files.createDirectories(folder);for(int i=0;i<frames.size();i++)try(var pixels=frames.get(i).pixels()){pixels.writeToFile(folder.resolve("%03d.png".formatted(i)));}frames.clear();motions.add(traceName,trace);traceName=null;trace=null;
                }
            }
            if(!shots.isEmpty()){
                var batch=List.copyOf(shots);shots.clear();try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(directory(mc));for(var name:batch){pixels.writeToFile(directory(mc).resolve(name));var state=new JsonObject();state.addProperty("stage",stage);state.addProperty("screen",mc.screen==null?"World":mc.screen.getClass().getSimpleName());state.addProperty("returning",SkintCamera.returning());state.addProperty("language",mc.getLanguageManager().getSelected());captures.add(name,state);}}
            }
        }catch(Exception error){failed=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_RETURN_CLIENT frame failed",error);finish(mc);}
    }
    private SkintReturnClientSmoke(){}
}
