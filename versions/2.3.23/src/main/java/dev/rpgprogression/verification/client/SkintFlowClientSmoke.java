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
public final class SkintFlowClientSmoke {
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
    private static int nested;
    private static boolean movementSent;
    private static UUID seatId;
    private static long renderedFrames;
    private static boolean textureMatches,poseMatches;
    private static double authoredSeconds;
    private static int nearFrames,nearLeaks,previewFrames;
    private static boolean worldBodyRendered,previewStanding;
    private static long renderedFrame,returnedAt=-1;
    private static UUID sourceToken;
    private static BlockPos localTarget;
    private static boolean drainedDuringStand;
    private static boolean persistedFixturesReady;
    private static boolean fadeInterruptionChecked;
    private static java.util.function.Consumer<ModNetworking.SkintSnapshot> originalReceiver;

    private static final JsonObject persisted=new JsonObject();
    private static boolean relogin(){return Boolean.getBoolean("rpgprogression.skintFlowRelogin")||Boolean.getBoolean("rpgprogression.skintFlowUpgrade");}
    private static boolean sitting(Minecraft mc){return mc.player.getVehicle() instanceof SkintSeat;}
    private static void progression(Minecraft mc){var screen=mc.screen;double scale=Math.min(screen.width/640.0,screen.height/360.0);double x=(screen.width-640*scale)/2+100*scale,y=(screen.height-360*scale)/2+112*scale;screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);}
    private static void tree(Minecraft mc){var button=(net.minecraft.client.gui.components.Button)mc.screen.children().stream().filter(c->c.getClass().getSimpleName().equals("SkillTreeButton")).findFirst().orElseThrow();var screen=mc.screen;double scale=Math.min(1.5,Math.min(screen.width/640.0,screen.height/360.0));double x=(screen.width-640*scale)/2+(button.getX()+button.getWidth()/2.0)*scale,y=(screen.height-360*scale)/2+(button.getY()+button.getHeight()/2.0)*scale;screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);}
    private static String epicMotion(Minecraft mc)throws Exception{
        if(!ModList.get().isLoaded("epicfight"))return "ABSENT";
        var cap=Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");var patchType=Class.forName("yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch");
        var patch=cap.getMethod("getEntityPatch",net.minecraft.world.entity.Entity.class,Class.class).invoke(null,mc.player,patchType);
        return String.valueOf(patch.getClass().getField("currentLivingMotion").get(patch));
    }

    private static boolean enabled(){return Boolean.getBoolean("rpgprogression.skintFlowSmoke")||relogin();}
    private static Path directory(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/skint-2.3.19/"+(Boolean.getBoolean("rpgprogression.skintFlowUpgrade")?"upgrade":ModList.get().isLoaded("epicfight")?"all":"base"));}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static Object field(Object object,String name)throws Exception{var f=object instanceof Class<?> c?c.getDeclaredField(name):object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object instanceof Class<?>?null:object);}
    private static void invoke(Object object,String name,Class<?>[] types,Object...args)throws Exception{var f=object.getClass().getDeclaredMethod(name,types);f.setAccessible(true);f.invoke(object,args);}
    private static Pose pose(Minecraft mc){var camera=mc.gameRenderer.getMainCamera();return new Pose(camera.getPosition(),camera.getYRot(),camera.getXRot());}
    private static boolean same(Pose a,Pose b){boolean same=a.position().distanceTo(b.position())<.02&&Math.abs(Mth.wrapDegrees(a.yaw()-b.yaw()))<.1&&Math.abs(a.pitch()-b.pitch())<.1;if(!same)com.mojang.logging.LogUtils.getLogger().info("SKINT_CAMERA_COMPARISON actual={} expected={}",a,b);return same;}
    private static void check(boolean ok,String text){SkintTravelChecks.check(checks,ok,text);if(!ok)failed=true;}
    private static void advance(){stage++;age=0;com.mojang.logging.LogUtils.getLogger().info("SKINT_SEAT_CLIENT stage {}",stage);}
    private static void server(Minecraft mc,Runnable action){pending=CompletableFuture.runAsync(action,mc.getSingleplayerServer());}
    private static void use(Minecraft mc){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(a),net.minecraft.core.Direction.SOUTH,a,false));}
    private static void exit(Minecraft mc){var screen=mc.screen;double scale=Math.min(screen.width/640.0,screen.height/360.0);double x=(screen.width-640*scale)/2+100*scale,y=(screen.height-360*scale)/2+186*scale;screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);}
    private static void escape(Minecraft mc){check(mc.screen.keyPressed(256,0,0),"the actual Escape key is handled by the hub");}
    private static void startTrace(Minecraft mc,String name){traceName=name;trace=new JsonArray();traceSource=pose(mc);traceBegan=System.nanoTime();}
    private static boolean hub(Minecraft mc){return mc.screen instanceof SkintScreen&&SkintCamera.settled()&&!SkintFade.active()&&age>=24;}
    private static boolean black(NativeImage image){for(int x=0;x<image.getWidth();x+=11)for(int y=0;y<image.getHeight();y+=11)if((image.getPixelRGBA(x,y)&0xffffff)!=0)return false;return true;}
    private static boolean returned(Minecraft mc){
        if(SkintCamera.active()||SkintCamera.returning()||mc.screen!=null||sitting(mc)){returnedAt=-1;return false;}
        if(returnedAt<0)returnedAt=renderedFrame;
        return renderedFrame-returnedAt>=2;
    }
    private static void finish(Minecraft mc){
        if(finished)return;finished=true;failed|=checks.asList().stream().anyMatch(c->!c.getAsJsonObject().get("passed").getAsBoolean());
        try{var report=new JsonObject();report.addProperty("version","2.3.19");report.addProperty("result",failed?"FAILED":"PASSED");report.addProperty("world",System.getProperty("rpgprogression.smokeWorld"));report.add("checks",checks);report.add("screenshots",captures);report.add("motions",motions);report.add("persisted",persisted);report.addProperty("relogin",relogin());
            var mods=new JsonArray();for(var mod:ModList.get().getMods()){var row=new JsonObject();row.addProperty("id",mod.getModId());row.addProperty("version",mod.getVersion().toString());mods.add(row);}report.add("loadedMods",mods);
            Files.createDirectories(directory(mc));Files.writeString(directory(mc).resolve(relogin()?"restart-result.json":"first-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("SKINT_SEAT_CLIENT report failed",error);}
        mc.options.hideGui=false;SkintCamera.end();mc.options.setCameraType(CameraType.FIRST_PERSON);com.mojang.logging.LogUtils.getLogger().info("SKINT_SEAT_CLIENT_RESULT {}",failed?"FAILED":"PASSED");mc.stop();
    }
    private static void flow(Minecraft mc)throws Exception{
        if(++boot>6000)throw new IllegalStateException("World startup timeout");String world=System.getProperty("rpgprogression.smokeWorld","HeartSkint233");
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var previous=mc.screen;
            if(Files.isDirectory(mc.gameDirectory.toPath().resolve("saves").resolve(world)))mc.createWorldOpenFlows().openWorld(world,()->mc.setScreen(previous));
            else{creating=true;CreateWorldScreen.openFresh(mc,mc.screen);}return;}
        if(creating&&mc.screen instanceof CreateWorldScreen){var state=field(mc.screen,"uiState");invoke(state,"setName",new Class[]{String.class},world);creating=false;invoke(mc.screen,"onCreate",new Class[]{});}
    }
    private static void fund(Minecraft mc){var d=dev.rpgprogression.progression.ProgressionManager.get(player(mc));d.spark=20;}
    private static void spent(Minecraft mc,double expected,String name){double value=dev.rpgprogression.progression.ProgressionManager.get(player(mc)).spark;check(value>=expected-1e-7&&value<expected+.4,name+" (remaining "+value+")");}
    private static void prepare(Minecraft mc){
        var p=player(mc);a=new BlockPos(48,200,4);var world=p.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);SkintTravelChecks.point(world,a);world.setDayTime(18000);world.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,world.getServer());
        var data=dev.rpgprogression.progression.ProgressionManager.get(p);double savedSpark=data.spark;data.spark=1000;
        p.teleportTo(world,a.getX()+.5,a.getY(),a.getZ()+2.5,180,10);data.spark=savedSpark;
        p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.getAbilities().invulnerable=true;p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.removeAllEffects();p.setDeltaMovement(Vec3.ZERO);
        p.connection.teleport(a.getX()+.5,a.getY(),a.getZ()+2.5,180,10);p.serverLevel().setDayTime(18000);p.serverLevel().setWeatherParameters(10000,0,false,false);ModNetworking.sync(p);
        dev.rpgprogression.verification.SparkBalanceChecks.run(p,checks);
        dev.rpgprogression.verification.RequirementsChecks.run(p,checks);
        if(dev.rpgprogression.compat.IronsCompat.enabled()){
            dev.rpgprogression.verification.IronsIntelligenceChecks.run(p,checks);
            data.set(dev.rpgprogression.attribute.RPGAttribute.INTELLIGENCE,50);data.set(dev.rpgprogression.attribute.RPGAttribute.SPARK_POTENTIAL,10);
            dev.rpgprogression.progression.ProgressionManager.refresh(p,10,false);data.mana=4;data.manaRegenCooldown=20000;ModNetworking.resources(p);
        }
        var recipe=p.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.withDefaultNamespace("stick")).orElseThrow();
        p.resetRecipes(List.of(recipe));p.awardRecipes(List.of(recipe));check(p.getRecipeBook().contains(recipe),"recipes still unlock on the server while their popup is disabled");
    }
    private static void restart(Minecraft mc)throws Exception{
        switch(stage){
            case 0->{if(age<20)return;
                if(Boolean.getBoolean("rpgprogression.skintChargeClientRestart")&&!persistedFixturesReady){server(mc,()->persistedFixturesReady=dev.rpgprogression.verification.SkintChargeChecks.persistedFixturesReady(player(mc).serverLevel()));return;}
                var first=JsonParser.parseString(Files.readString((Boolean.getBoolean("rpgprogression.skintFlowUpgrade")?directory(mc).resolveSibling("all"):directory(mc)).resolve("first-result.json"))).getAsJsonObject();persisted.asMap().putAll(first.getAsJsonObject("persisted").asMap());a=new BlockPos(persisted.get("skintX").getAsInt(),persisted.get("skintY").getAsInt(),persisted.get("skintZ").getAsInt());
                if(Boolean.getBoolean("rpgprogression.skintFlowUpgrade"))check(mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,a)==15&&mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,a.above())>=14,"a skint placed and saved in 2.3.5 emits light without replacement or interaction after upgrading");
                check(!ClientSkintData.active()&&!SkintCamera.active()&&!sitting(mc),"a full game restart begins standing with no menu authorization or camera state");
                server(mc,()->{var p=player(mc);var row=persisted.getAsJsonArray("standing");var expected=new Vec3(row.get(0).getAsDouble(),row.get(1).getAsDouble(),row.get(2).getAsDouble());check(!p.isPassenger()&&p.position().distanceTo(expected)<.02,"the saved player loads at the safe standing position");check(p.serverLevel().getEntitiesOfClass(SkintSeat.class,new AABB(a).inflate(40)).isEmpty(),"temporary seats do not reappear after saving and reopening the world");if(Boolean.getBoolean("rpgprogression.skintChargeClientRestart"))dev.rpgprogression.verification.SkintChargeChecks.persistedFixtures(p.serverLevel(),checks);});advance();}
            case 1->{if(age<10)return;use(mc);advance();}
            case 2->{if(!hub(mc)||!sitting(mc))return;check(!mc.player.getVehicle().getUUID().toString().equals(persisted.get("seat").getAsString()),"right-click after restart creates a new temporary seat");shots.add("08-after-restart-seated.png");advance();}
            case 3->{if(age<5)return;exit(mc);advance();}
            case 4->{if(age==6){check(sitting(mc)&&((SkintSeat)mc.player.getVehicle()).standing(),"Exit plays stand before removing the passenger");check(poseMatches,"the actual rendered get-up pose matches stand keyframes");server(mc,()->check(SkintManager.session(player(mc))==null&&!SkintManager.canProgress(player(mc)),"progression access closes at the beginning of standing, not after the cutscene"));}if(!returned(mc))return;check(!sitting(mc),"the player can stand up normally after a full game restart");shots.add("09-after-restart-standing.png");advance();}
            case 5->{if(age>5&&shots.isEmpty())finish(mc);}
            default->{}
        }
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
            if(relogin()){restart(mc);return;}
            switch(stage){
                case 0->{mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.languageCode="ru_ru";mc.getLanguageManager().setSelected("ru_ru");reload=mc.reloadResourcePacks();mc.options.setCameraType(CameraType.FIRST_PERSON);server(mc,()->prepare(mc));advance();}
                case 1->{if(age<40)return;
                    if(!RequirementsVisualChecks.tick(mc,checks))return;
                    if(!ReadOnlyProgressionChecks.tick(mc,checks))return;
                    var recipe=mc.level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.withDefaultNamespace("stick")).orElseThrow();
                    check(mc.player.getRecipeBook().contains(recipe),"the client recipe book still receives the unlocked recipe");
                    net.minecraft.client.gui.components.toasts.RecipeToast.addOrUpdate(mc.getToasts(),recipe);
                    check(mc.getToasts().getToast(net.minecraft.client.gui.components.toasts.RecipeToast.class,net.minecraft.client.gui.components.toasts.Toast.NO_TOKEN)==null,"New Recipes Unlocked cannot create a toast");
                    var toastId=net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId.PERIODIC_NOTIFICATION;
                    net.minecraft.client.gui.components.toasts.SystemToast.addOrUpdate(mc.getToasts(),toastId,Component.literal("Verification"),Component.literal("Other notifications still work"));
                    check(mc.getToasts().getToast(net.minecraft.client.gui.components.toasts.SystemToast.class,toastId)!=null,"other notifications remain available");mc.getToasts().clear();
                    normal=pose(mc);startTrace(mc,"entry");use(mc);advance();}
                case 2->{if(!hub(mc))return;check(mc.level.getDayTime()%24000==18000&&mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,a)==15&&mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,a.above())>=14,"the skint emits level 15 and lights adjacent air at night");check(ClientSkintData.active(),"right-click still opens the authorized hub with the entry animation");
                    check(sitting(mc),"the real client is mounted on its invisible skint seat");seatId=mc.player.getVehicle().getUUID();
                    check(renderedFrames>5&&poseMatches,"the actual vanilla player renderer applies the authored sitting keyframes");check(textureMatches,"the rendered player uses their actual skin texture rather than anima.png");check(SkintCamera.TRANSITION_NANOS==1_400_000_000L,"entry and return cameras both use the requested 1.4-second duration");
                    server(mc,()->{var player=player(mc);check(player.getVehicle() instanceof SkintSeat&&SkintManager.canProgress(player),"the authoritative server also has the seated player and valid menu session");var seat=(SkintSeat)player.getVehicle();
                        var tag=player.saveWithoutId(new net.minecraft.nbt.CompoundTag());check(!tag.contains("RootVehicle")&&Math.abs(tag.getList("Pos",6).getDouble(1)-seat.getY())<.001,"saving a seated player records safe standing coordinates without a temporary vehicle");
                        check(!seat.shouldBeSaved()&&!seat.getType().canSerialize(),"the invisible seat cannot be persisted as a world entity");});
                    shots.add("01-hub-seated.png");advance();}
                case 3->{if(age<5)return;
                    if(nested==0){progression(mc);nested=1;age=0;return;}
                    if(nested==1){if(!(mc.screen instanceof dev.rpgprogression.client.gui.CharacterScreen)||age<15)return;check(sitting(mc)&&mc.player.getVehicle().getUUID().equals(seatId),"the real world player stays seated while progression is open");check(previewFrames>0&&previewStanding,"the actual progression portrait renders straight standing legs while the world remains seated");shots.add("02-progression-standing.png");nested=6;age=0;return;}
                    if(nested==6){if(age<2)return;tree(mc);nested=2;age=0;return;}
                    if(nested==2){if(!(mc.screen instanceof dev.rpgprogression.client.gui.SkillTreeScreen)||age<20)return;check(sitting(mc)&&mc.player.getVehicle().getUUID().equals(seatId),"the same seat is retained throughout the skill tree");shots.add("03-tree-seated.png");nested=7;age=0;return;}
                    if(nested==7){if(age<2)return;mc.screen.onClose();nested=3;age=0;return;}
                    if(nested==3){if(age<10)return;mc.screen.onClose();nested=4;age=0;return;}
                    if(nested==4){if(!(mc.screen instanceof SkintScreen)||age<10)return;check(sitting(mc),"returning to the hub does not stand up or create a new seat");mc.setScreen(new dev.rpgprogression.client.gui.SkintTravelScreen(mc.screen));nested=8;age=0;return;}
                    if(nested==8){if(age<12)return;shots.add("12-travel-cost.png");nested=9;age=0;return;}
                    if(nested==9){if(age<2)return;mc.screen.onClose();nested=5;age=0;return;}
                    startTrace(mc,"exit-button");exit(mc);check(mc.screen==null&&!ClientSkintData.active()&&SkintCamera.returning(),"Exit closes the UI and revokes access immediately while the camera starts returning");advance();}
                case 4->{if(age==6){check(sitting(mc)&&((SkintSeat)mc.player.getVehicle()).standing(),"Exit plays stand before removing the passenger");check(poseMatches,"the actual rendered get-up pose matches stand keyframes");server(mc,()->check(SkintManager.session(player(mc))==null&&!SkintManager.canProgress(player(mc)),"progression access closes at the beginning of standing, not after the cutscene"));}if(!returned(mc)||!motions.has("exit-button"))return;check(!sitting(mc)&&same(pose(mc),normal)&&mc.options.getCameraType()==CameraType.FIRST_PERSON,"Exit smoothly restores the original first-person position and angles");shots.add("02-after-exit.png");server(mc,()->check(SkintManager.session(player(mc))==null&&!player(mc).isPassenger(),"Exit closes the server session and stands the player up"));advance();}
                case 5->{if(age<10)return;use(mc);advance();}
                case 6->{if(!hub(mc))return;startTrace(mc,"escape");escape(mc);check(mc.screen==null&&!ClientSkintData.active()&&SkintCamera.returning(),"Escape starts the same smooth return and closes authorization");advance();}
                case 7->{if(!returned(mc)||!motions.has("escape"))return;check(nearFrames>3&&nearLeaks==0,"the local body is never rendered when the entry or return camera is inside its safety margin");check(!sitting(mc)&&same(pose(mc),normal),"Escape stands up and restores the same original first-person view");shots.add("03-after-escape.png");advance();}
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
                case 19->{if(!hub(mc))return;exit(mc);advance();}
                case 20->{if(!movementSent&&age>=4){movementSent=true;server(mc,()->{var p=player(mc);p.teleportTo(p.serverLevel(),a.getX()+1.5,a.getY(),a.getZ()+2.5,90,0);});return;}if(!returned(mc)||age<20)return;check(pose(mc).position().distanceTo(mc.player.getEyePosition())<.02&&Math.abs(Mth.wrapDegrees(pose(mc).yaw()-mc.player.getYRot()))<.1,"the return follows the live player destination after movement and rotation");server(mc,()->player(mc).teleportTo(player(mc).serverLevel(),a.getX()+.5,a.getY(),a.getZ()+2.5,180,10));advance();}
                case 21->{if(age<20)return;use(mc);advance();}
                case 22->{if(!hub(mc))return;exit(mc);server(mc,()->{var p=player(mc);p.teleportTo(p.serverLevel(),a.getX()+16.5,a.getY(),a.getZ()+2.5,180,10);});advance();}
                case 23->{if(!returned(mc)||age<20)return;check(pose(mc).position().distanceTo(mc.player.getEyePosition())<.02&&mc.options.getCameraType()==CameraType.FIRST_PERSON,"a distant teleport cancels the return and immediately uses the new normal camera");shots.add("06-after-teleport-cancel.png");advance();}
                case 24->{if(age<10)return;server(mc,()->player(mc).teleportTo(player(mc).serverLevel(),a.getX()+.5,a.getY(),a.getZ()+2.5,180,10));advance();}
                case 25->{if(age<15)return;use(mc);advance();}
                case 26->{if(!hub(mc)||!sitting(mc))return;startTrace(mc,"travel");server(mc,()->{var p=player(mc);var target=p.getServer().getLevel(net.minecraft.world.level.Level.NETHER);var destination=new BlockPos(0,100,0);SkintTravelChecks.point(target,destination);var point=SkintNetwork.get(target).ensure(target,destination);dev.rpgprogression.progression.ProgressionManager.get(p).discoveredSkints.add(point.id());sourceToken=SkintManager.session(p).token();fund(mc);check(SkintManager.travel(p,point.id()),"an authorized cross-dimension transfer is queued");check(p.serverLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)&&p.getVehicle() instanceof SkintSeat seat&&seat.standing()&&SkintManager.pending(p),"travel first plays stand at the source instead of teleporting immediately");check(!SkintManager.canProgress(p),"attribute spending is disabled during the departure animation");
                    SkintManager.action(p,4,sourceToken,new UUID(0,0));check(p.serverLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)&&SkintManager.pending(p),"an early black-frame acknowledgement cannot skip the stand animation");});advance();}
                case 27->{
                    if(!fadeInterruptionChecked&&SkintFade.active()&&!SkintFade.committed()){
                        fadeInterruptionChecked=true;escape(mc);check(mc.screen instanceof SkintScreen&&ClientSkintData.active(),"Escape during fade cannot expose the intermediate transfer or close the source session");
                    }
                    if(!mc.level.dimension().equals(net.minecraft.world.level.Level.NETHER)||!hub(mc)||ClientSkintData.busy()||!sitting(mc)||!motions.has("travel"))return;check(ClientSkintData.origin().pos().equals(new BlockPos(0,100,0)),"the destination hub opens automatically in the Nether without another right-click");shots.add("10-arrival-menu.png");server(mc,()->{var p=player(mc);check(SkintManager.canProgress(p)&&!SkintManager.pending(p)&&!sourceToken.equals(SkintManager.session(p).token()),"arrival replaces the old authorization with a valid destination session");spent(mc,15,"cross-world skint travel costs five without a second twenty-five charge");});advance();}
                case 28->{if(!hub(mc)||!sitting(mc))return;server(mc,()->{var p=player(mc);var origin=p.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);var point=SkintNetwork.get(origin).ensure(origin,a);fund(mc);check(SkintManager.travel(p,point.id()),"the return route also queues its stand animation");});advance();}
                case 29->{if(!mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||!hub(mc)||ClientSkintData.busy()||!ClientSkintData.origin().pos().equals(a))return;check(sitting(mc),"the source menu also reopens automatically after returning from the Nether");advance();}
                case 30->{if(!hub(mc)||!sitting(mc))return;check(!mc.player.getVehicle().getUUID().equals(seatId),"reopening creates a fresh seat after previous seats were removed");server(mc,()->{var player=player(mc);var seat=(SkintSeat)player.getVehicle();var list=new JsonArray();list.add(seat.getX());list.add(seat.getY());list.add(seat.getZ());persisted.add("standing",list);persisted.addProperty("skintX",a.getX());persisted.addProperty("skintY",a.getY());persisted.addProperty("skintZ",a.getZ());persisted.addProperty("seat",seat.getUUID().toString());
                    check(player.serverLevel().getEntitiesOfClass(SkintSeat.class,new AABB(a).inflate(40)).size()==1,"only the currently occupied seat remains in the world");});advance();}
                case 31->{if(age<5)return;shots.add("07-before-quit-seated.png");advance();}
                case 32->{if(age<5)return;server(mc,()->{var p=player(mc);localTarget=a.offset(12,0,0);SkintTravelChecks.point(p.serverLevel(),localTarget);var point=SkintNetwork.get(p.serverLevel()).ensure(p.serverLevel(),localTarget);dev.rpgprogression.progression.ProgressionManager.get(p).discoveredSkints.add(point.id());fund(mc);check(SkintManager.travel(p,point.id()),"a same-dimension departure can be queued");});advance();}
                case 33->{if(age<5||!ClientSkintData.busy())return;escape(mc);advance();}
                case 34->{if(age<35||!returned(mc))return;check(mc.player.position().distanceTo(Vec3.atBottomCenterOf(a))<4,"Escape cancels a queued transfer before it can move the player");server(mc,()->{spent(mc,20,"Escape cancellation does not spend spark");check(!SkintManager.pending(player(mc))&&SkintManager.session(player(mc))==null,"cancelling clears both pending travel and authorization");});use(mc);advance();}
                case 35->{if(!hub(mc))return;server(mc,()->{var p=player(mc);var point=SkintNetwork.get(p.serverLevel()).ensure(p.serverLevel(),localTarget);fund(mc);check(SkintManager.travel(p,point.id()),"the destination is valid before an interrupted departure");p.serverLevel().setBlock(localTarget,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);});advance();}
                case 36->{if(age<35||ClientSkintData.busy()||!hub(mc))return;check(ClientSkintData.origin().pos().equals(a)&&sitting(mc),"a destination removed during stand keeps the player at the source and restores its hub");server(mc,()->{var p=player(mc);spent(mc,20,"a removed or timed-out target does not spend spark");SkintTravelChecks.point(p.serverLevel(),localTarget);var point=SkintNetwork.get(p.serverLevel()).ensure(p.serverLevel(),localTarget);dev.rpgprogression.progression.ProgressionManager.get(p).discoveredSkints.add(point.id());fund(mc);check(SkintManager.travel(p,point.id()),"the recreated and rediscovered destination accepts a new departure");});advance();}
                case 37->{if(!hub(mc)||ClientSkintData.busy()||!ClientSkintData.origin().pos().equals(localTarget))return;check(sitting(mc),"same-dimension travel automatically opens and seats the player at the new skint");shots.add("11-local-arrival.png");server(mc,()->{var p=player(mc);spent(mc,15,"same-world skint travel costs five once");fund(mc);check(SkintManager.travel(p,SkintNetwork.get(p.serverLevel()).ensure(p.serverLevel(),a).id()),"the same-dimension route back is accepted");});advance();}
                case 38->{if(!hub(mc)||ClientSkintData.busy()||!ClientSkintData.origin().pos().equals(a))return;server(mc,()->{var p=player(mc);var seat=(SkintSeat)p.getVehicle();var list=new JsonArray();list.add(seat.getX());list.add(seat.getY());list.add(seat.getZ());persisted.add("standing",list);persisted.addProperty("seat",seat.getUUID().toString());});advance();}
                case 39->{if(age<5||!shots.isEmpty())return;fadeInterruptionChecked=false;server(mc,()->{var p=player(mc);var point=SkintNetwork.get(p.serverLevel()).ensure(p.serverLevel(),localTarget);fund(mc);check(SkintManager.travel(p,point.id()),"a second departure begins for the fade-time invalidation test");});advance();}
                case 40->{if(!fadeInterruptionChecked&&SkintFade.renderedOpacity()>.1){fadeInterruptionChecked=true;server(mc,()->player(mc).serverLevel().setBlock(localTarget,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3));}
                    if(!fadeInterruptionChecked||ClientSkintData.busy()||!hub(mc))return;
                    check(ClientSkintData.origin().pos().equals(a)&&sitting(mc)&&!SkintFade.active(),"a destination removed during fade restores the source hub without a stuck black screen");
                    originalReceiver=ModNetworking.clientSkint;ModNetworking.clientSkint=packet->{if(packet.data().getInt("mode")!=4)originalReceiver.accept(packet);};
                    server(mc,()->{var p=player(mc);spent(mc,20,"a removed or timed-out target does not spend spark");SkintTravelChecks.point(p.serverLevel(),localTarget);var point=SkintNetwork.get(p.serverLevel()).ensure(p.serverLevel(),localTarget);dev.rpgprogression.progression.ProgressionManager.get(p).discoveredSkints.add(point.id());fund(mc);check(SkintManager.travel(p,point.id()),"a departure can wait for a deliberately missing fade acknowledgement");});advance();}
                case 41->{if(age<250||ClientSkintData.busy()||!hub(mc))return;ModNetworking.clientSkint=originalReceiver;
                    check(ClientSkintData.origin().pos().equals(a)&&!SkintFade.active()&&sitting(mc),"a missing black-frame acknowledgement times out at the source and restores a usable menu");
                    server(mc,()->{spent(mc,20,"missing acknowledgement timeout does not spend spark");check(!SkintManager.pending(player(mc))&&SkintManager.canProgress(player(mc)),"server timeout removes pending travel and restores progression access");});advance();}
                case 42->{if(age<5)return;drainedDuringStand=false;server(mc,()->{var p=player(mc);var point=SkintNetwork.get(p.serverLevel()).ensure(p.serverLevel(),localTarget);var d=dev.rpgprogression.progression.ProgressionManager.get(p);d.spark=4;
                    check(!SkintManager.travel(p,point.id())&&!SkintManager.pending(p)&&SkintManager.canProgress(p),"a real client route is rejected without five spark and leaves the source menu usable");
                    fund(mc);check(SkintManager.travel(p,point.id()),"the resource-change departure starts with sufficient spark");});advance();}
                case 43->{if(!drainedDuringStand&&age>=6){drainedDuringStand=true;server(mc,()->dev.rpgprogression.progression.ProgressionManager.get(player(mc)).spark=4);}
                    if(!drainedDuringStand||ClientSkintData.busy()||!hub(mc))return;server(mc,()->{var p=player(mc);spent(mc,4,"insufficient spark at final confirmation cancels without spending");check(SkintManager.canProgress(p)&&!SkintManager.pending(p),"resource loss during stand restores the source menu");SkintManager.close(p,true);});advance();}
                case 44->{if(age<10||mc.screen!=null)return;server(mc,()->{var p=player(mc);var d=dev.rpgprogression.progression.ProgressionManager.get(p);var nether=p.getServer().getLevel(net.minecraft.world.level.Level.NETHER);d.spark=24;
                    p.teleportTo(nether,.5,100,2.5,180,10);check(p.serverLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)&&d.spark==24,"ordinary dimension travel with twenty-four spark is still blocked");
                    d.spark=25;p.teleportTo(nether,.5,100,2.5,180,10);check(p.serverLevel()==nether&&d.spark==0,"a successful ordinary dimension transfer still charges exactly twenty-five");});advance();}
                case 45->{if(!mc.level.dimension().equals(net.minecraft.world.level.Level.NETHER)||mc.screen!=null||age<20)return;server(mc,()->{var p=player(mc);var d=dev.rpgprogression.progression.ProgressionManager.get(p);d.spark=25;var world=p.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);p.teleportTo(world,a.getX()+.5,a.getY(),a.getZ()+2.5,180,10);check(p.serverLevel()==world&&d.spark==0,"ordinary return travel also charges twenty-five once");d.spark=5;});advance();}
                case 46->{if(!mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||mc.screen!=null||age<20)return;use(mc);advance();}
                case 47->{if(!hub(mc))return;server(mc,()->{var p=player(mc);var seat=(SkintSeat)p.getVehicle();var row=new JsonArray();row.add(seat.getX());row.add(seat.getY());row.add(seat.getZ());persisted.add("standing",row);persisted.addProperty("seat",seat.getUUID().toString());});advance();}
                case 48->{if(age>5&&shots.isEmpty())finish(mc);}
                default->{}
            }
        }catch(Throwable error){failed=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_SEAT_CLIENT failed at stage "+stage,error);finish(mc);}
    }
    @SubscribeEvent public static void render(RenderFrameEvent.Post event){
        if(!enabled()||finished)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        try{
            if(reopenPending&&mc.screen instanceof SkintScreen){
                var source=(SkintCamera.View)field(SkintCamera.class,"start");reopenVerified=lastPose!=null&&source.position().distanceTo(lastPose.position())<.02;
                check(field(SkintCamera.class,"previous")==CameraType.FIRST_PERSON,"reopening remembers the actual original perspective instead of the temporary cinematic mode");reopenPending=false;
            }
            lastPose=pose(mc);if(SkintCamera.hidePlayer(mc.player,mc.getTimer().getGameTimeDeltaPartialTick(true))){nearFrames++;if(worldBodyRendered)nearLeaks++;}
            if(traceName!=null){
                long elapsed=System.nanoTime()-traceBegan;var pose=pose(mc);var sample=new JsonObject();sample.addProperty("nanos",elapsed);sample.addProperty("fade",SkintFade.renderedOpacity());sample.addProperty("busy",ClientSkintData.busy());sample.addProperty("dimension",mc.level.dimension().location().toString());sample.addProperty("screen",mc.screen==null?"World":mc.screen.getClass().getSimpleName());sample.addProperty("x",pose.position().x);sample.addProperty("y",pose.position().y);sample.addProperty("z",pose.position().z);sample.addProperty("yaw",pose.yaw());sample.addProperty("pitch",pose.pitch());sample.addProperty("returning",SkintCamera.returning());sample.addProperty("authoredSeconds",authoredSeconds);sample.addProperty("authoredPoseRendered",poseMatches);sample.addProperty("skinMatches",textureMatches);sample.addProperty("bodyHidden",SkintCamera.hidePlayer(mc.player,mc.getTimer().getGameTimeDeltaPartialTick(true)));sample.addProperty("worldBodyRendered",worldBodyRendered);trace.add(sample);
                try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){var small=new NativeImage(640,360,false);pixels.resizeSubRectTo(0,0,pixels.getWidth(),pixels.getHeight(),small);frames.add(new Frame(small));}
                if((traceName.equals("entry")&&SkintCamera.settled())||(traceName.equals("travel")&&mc.level.dimension().equals(net.minecraft.world.level.Level.NETHER)&&SkintCamera.settled()&&!SkintFade.active())||(!traceName.equals("entry")&&!traceName.equals("travel")&&!SkintCamera.active())){
                    if(traceName.equals("travel")){
                        check(trace.size()>=10,"native frames include stand before transfer and the destination menu entry");
                        var rows=trace.asList().stream().map(JsonElement::getAsJsonObject).toList();
                        int firstFade=-1,firstDestination=-1,firstReveal=-1;boolean darkIntermediate=false,revealIntermediate=false;double prev=0;
                        for(int i=0;i<rows.size();i++){
                            var row=rows.get(i);double a=row.get("fade").getAsDouble();
                            if(firstFade<0&&a>0)firstFade=i;
                            if(firstDestination<0&&row.get("dimension").getAsString().equals("minecraft:the_nether"))firstDestination=i;
                            if(a>0&&a<1){if(firstDestination<0)darkIntermediate=true;else{revealIntermediate=true;if(firstReveal<0)firstReveal=i;}}
                            if(a>=.999){check(black(frames.get(i).pixels()),"opaque transfer frame "+i+" contains only black pixels");}
                            prev=a;
                        }
                        check(firstFade>=0&&rows.get(firstFade).get("nanos").getAsLong()>=1_300_000_000L&&rows.get(firstFade).get("authoredSeconds").getAsDouble()>=1.4,"fade starts after the full 1.4-second standing animation");
                        check(firstDestination>0&&rows.get(firstDestination-1).get("fade").getAsDouble()==1&&rows.get(firstDestination).get("fade").getAsDouble()==1,"both sides of the dimension switch are completely black");
                        check(darkIntermediate&&revealIntermediate,"both fade out and fade in render intermediate opacity frames");
                        check(firstReveal>firstDestination&&rows.get(firstReveal).get("screen").getAsString().equals("SkintScreen")&&!rows.get(firstReveal).get("busy").getAsBoolean(),"fade in begins only in the destination hub");
                        check(rows.get(rows.size()-1).get("fade").getAsDouble()==0,"arrival finishes with a fully visible destination menu");
                    }else if(traceName.equals("entry")){
                        // settled() samples nanoTime after the frame sample above; its boundary may
                        // be crossed while building that sample. Compare using the same later clock.
                        check(trace.size()>=6&&System.nanoTime()-traceBegan>=SkintCamera.TRANSITION_NANOS,"the entry cutscene renders intermediate frames over 1.4 seconds");
                    }else{
                    var first=trace.get(0).getAsJsonObject();var firstPos=new Vec3(first.get("x").getAsDouble(),first.get("y").getAsDouble(),first.get("z").getAsDouble());double distance=traceSource.position().distanceTo(normal.position());
                    check(trace.size()>=4&&firstPos.distanceTo(traceSource.position())<distance*.35,traceName+": first return frame stays near the visible menu camera");
                    check(trace.asList().stream().map(t->t.getAsJsonObject().get("x").getAsDouble()).distinct().count()>=4,traceName+": several intermediate camera positions are actually rendered");
                    check(same(pose,normal),traceName+": the final rendered frame matches the original normal camera");
                    check(elapsed>=SkintCamera.TRANSITION_NANOS&&elapsed<SkintCamera.TRANSITION_NANOS+600_000_000L,traceName+": the visual return completes in approximately 1.4 seconds");
                    }
                    var folder=directory(mc).resolve(traceName);Files.createDirectories(folder);for(int i=0;i<frames.size();i++)try(var pixels=frames.get(i).pixels()){pixels.writeToFile(folder.resolve("%03d.png".formatted(i)));}frames.clear();motions.add(traceName,trace);traceName=null;trace=null;
                }
            }
            if(!shots.isEmpty()){
                var batch=List.copyOf(shots);shots.clear();try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(directory(mc));for(var name:batch){pixels.writeToFile(directory(mc).resolve(name));var state=new JsonObject();state.addProperty("stage",stage);state.addProperty("screen",mc.screen==null?"World":mc.screen.getClass().getSimpleName());state.addProperty("returning",SkintCamera.returning());state.addProperty("language",mc.getLanguageManager().getSelected());captures.add(name,state);}}
            }
        }catch(Exception error){failed=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_SEAT_CLIENT frame failed",error);finish(mc);}
    }
    @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
        if(!enabled()||finished||event.getEntity()!=Minecraft.getInstance().player||!(event.getEntity().getVehicle() instanceof SkintSeat seat)||!(event.getRenderer().getModel() instanceof net.minecraft.client.model.PlayerModel<?> model))return;
        if(SkintPlayerAnimation.previewing()){previewFrames++;previewStanding=Math.abs(model.leftLeg.xRot)<.001&&Math.abs(model.rightLeg.xRot)<.001&&Math.abs(model.body.y-model.body.getInitialPose().y)<.001;return;}
        worldBodyRendered=true;
        authoredSeconds=seat.animationSeconds(event.getPartialTick());var expected=seat.animationPose(event.getPartialTick()).bone("right_arm");
        poseMatches=Math.abs(model.rightArm.xRot-Math.toRadians(-expected.rx()))<.0001&&Math.abs(model.rightArm.y-model.rightArm.getInitialPose().y+expected.y())<.0001;
        textureMatches=((net.minecraft.client.renderer.entity.player.PlayerRenderer)event.getRenderer()).getTextureLocation(Minecraft.getInstance().player).equals(Minecraft.getInstance().player.getSkin().texture());renderedFrames++;
    }
    @SubscribeEvent public static void frameStart(RenderFrameEvent.Pre event){worldBodyRendered=false;renderedFrame++;if(enabled()&&!finished)Minecraft.getInstance().mouseHandler.releaseMouse();}
    @SubscribeEvent public static void dimensionChanged(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event){
        if(enabled()&&!finished&&event.getEntity() instanceof ServerPlayer p&&p.getUUID().equals(Minecraft.getInstance().player.getUUID())){
            check(!p.isPassenger(),"the server has already dismounted the player when the dimension transfer occurs");
            // Ordinary portal transfers also occur in the price tests; only skints use the fade handshake.
            if(SkintManager.travelling(p))check(SkintFade.committed()&&SkintFade.renderedOpacity()==1,"the server transfers only after the client has rendered opaque black and acknowledged it");
        }
    }
    private SkintFlowClientSmoke(){}
}
