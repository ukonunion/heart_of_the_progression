package dev.rpgprogression.verification.client;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.google.gson.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.block.ModBlocks;
import dev.rpgprogression.client.*;
import dev.rpgprogression.client.gui.*;
import dev.rpgprogression.effect.ModEffects;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skint.*;
import dev.rpgprogression.verification.SkintTravelChecks;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Native UI input, real teleport packets, then a separate-JVM persistence run. */
@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class SkintMenuClientSmoke {
    private static int boot,stage,age;
    private static boolean loading,creating,finished;
    private static volatile boolean failed;
    private static CompletableFuture<?> pending,reload;
    private static BlockPos a,b,n;
    private static SkintNetwork.Point pointA,pointB,pointN;
    private static final JsonArray checks=new JsonArray();
    private static final JsonObject captures=new JsonObject(),persisted=new JsonObject();
    private static final List<String> shots=new ArrayList<>();
    private static boolean enabled(){return Boolean.getBoolean("rpgprogression.skintMenuSmoke")||relogin();}
    private static boolean relogin(){return Boolean.getBoolean("rpgprogression.skintMenuRelogin");}
    private static Path directory(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/skint-2.3.1/all");}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static Object field(Object obj,String name)throws Exception{var f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
    private static void invoke(Object obj,String name,Class<?>[] types,Object...args)throws Exception{var f=obj.getClass().getDeclaredMethod(name,types);f.setAccessible(true);f.invoke(obj,args);}
    private static void check(boolean ok,String name){SkintTravelChecks.check(checks,ok,name);if(!ok)failed=true;}
    private static void advance(){stage++;age=0;com.mojang.logging.LogUtils.getLogger().info("SKINT_MENU_CLIENT stage {} relogin={}",stage,relogin());}
    private static void server(Minecraft mc,Runnable action){pending=CompletableFuture.runAsync(action,mc.getSingleplayerServer());}
    private static void language(Minecraft mc,String language){mc.options.languageCode=language;mc.getLanguageManager().setSelected(language);reload=mc.reloadResourcePacks();}
    private static void click(Minecraft mc,double x,double y,int w,int h){var screen=mc.screen;double scale=(w==330?Math.min(screen.width/346.0,screen.height/234.0):w==640&&screen instanceof SkintScreen?Math.min(screen.width/640.0,screen.height/360.0):Math.min(1.5,Math.min(screen.width/(double)w,screen.height/(double)h)));double sx=(screen.width-w*scale)/2+x*scale,sy=(screen.height-h*scale)/2+y*scale;screen.mouseClicked(sx,sy,0);screen.mouseReleased(sx,sy,0);}
    private static void button(Minecraft mc,String className){var button=(Button)mc.screen.children().stream().filter(c->c.getClass().getSimpleName().equals(className)).findFirst().orElseThrow();click(mc,button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,640,360);}
    private static void use(Minecraft mc,BlockPos pos){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.SOUTH,pos,false));}
    private static boolean menuChecked;
    private static void select(Minecraft mc,UUID id)throws Exception{
        if(mc.screen instanceof SkintScreen)click(mc,100,149,640,360);
        check(mc.screen instanceof SkintTravelScreen,"the hub travel action opens the separate textured teleport menu");
        if(!menuChecked){
            check(mc.screen.children().stream().filter(c->c.getClass().getSimpleName().equals("Tab")).count()==3,"all three dimension tabs are present");
            check(SkintTravelScreen.TAB_Y==2,"both tab textures are offset downward by two native pixels");
            click(mc,204,14,330,222);check(((List<?>)field(mc.screen,"points")).isEmpty(),"the End tab has its own empty destination list");
            check(((Button)field(mc.screen,"travel")).active==false,"travel is disabled without a destination");
            click(mc,164,14,330,222);check(((List<?>)field(mc.screen,"points")).stream().allMatch(p->((SkintNetwork.Point)p).dimension().equals(Level.NETHER)),"Nether tab filters routes by their actual dimension");
            mc.screen.onClose();check(mc.screen instanceof SkintScreen&&SkintCamera.active(),"Escape from travel returns to the hub without losing its camera or authorization");
            click(mc,100,149,640,360);menuChecked=true;
        }
        var point=ClientSkintData.destinations().stream().filter(p->p.id().equals(id)).findFirst().orElseThrow();
        int tab=point.dimension().equals(Level.NETHER)?1:point.dimension().equals(Level.END)?2:0;click(mc,124+tab*40,14,330,222);
        var list=(List<SkintNetwork.Point>)field(mc.screen,"points");for(int i=0;i<list.size();i++)if(list.get(i).id().equals(id)){click(mc,90,73+i*18,330,222);return;}
        throw new IllegalStateException("Missing expected route");
    }
    private static boolean arrived(Minecraft mc,BlockPos pos,net.minecraft.resources.ResourceKey<Level> dimension){return mc.level.dimension().equals(dimension)&&mc.player.position().distanceToSqr(Vec3.atCenterOf(pos))<12&&!ClientSkintData.managed(mc.screen);}
    private static void finish(Minecraft mc){
        if(finished)return;finished=true;
        try{var report=new JsonObject();report.addProperty("version","2.3.1");report.addProperty("result",failed?"FAILED":"PASSED");report.addProperty("relogin",relogin());report.addProperty("world",System.getProperty("rpgprogression.smokeWorld"));report.add("checks",checks);report.add("screenshots",captures);report.add("persisted",persisted);
            var mods=new JsonArray();for(var mod:ModList.get().getMods()){var row=new JsonObject();row.addProperty("id",mod.getModId());row.addProperty("version",mod.getVersion().toString());mods.add(row);}report.add("loadedMods",mods);
            Files.createDirectories(directory(mc));Files.writeString(directory(mc).resolve(relogin()?"restart-result.json":"first-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception error){failed=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_MENU_CLIENT report failed",error);}
        mc.options.hideGui=false;com.mojang.logging.LogUtils.getLogger().info("SKINT_MENU_CLIENT_RESULT {} relogin={}",failed?"FAILED":"PASSED",relogin());mc.stop();
    }
    private static void flow(Minecraft mc)throws Exception{
        if(++boot>6000)throw new IllegalStateException("World startup timeout");String world=System.getProperty("rpgprogression.smokeWorld","HeartSkint231-all-A");
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;
            if(relogin()){var previous=mc.screen;mc.createWorldOpenFlows().openWorld(world,()->mc.setScreen(previous));}
            else if(Files.isDirectory(mc.gameDirectory.toPath().resolve("saves").resolve(world))){var previous=mc.screen;mc.createWorldOpenFlows().openWorld(world,()->mc.setScreen(previous));}
            else{creating=true;CreateWorldScreen.openFresh(mc,mc.screen);}return;}
        if(creating&&mc.screen instanceof CreateWorldScreen){var state=field(mc.screen,"uiState");invoke(state,"setName",new Class[]{String.class},world);creating=false;invoke(mc.screen,"onCreate",new Class[]{});return;}
        if(loading&&mc.screen instanceof ConfirmScreen)for(var child:mc.screen.children())if(child instanceof Button button&&button.getMessage().getString().equals(net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString())){button.onPress();break;}
    }
    private static void prepare(Minecraft mc){
        var p=player(mc);var level=p.serverLevel();a=new BlockPos(p.getBlockX(),Math.max(100,p.getBlockY()+10),p.getBlockZ());b=a.offset(12,0,0);n=new BlockPos(0,100,0);
        var results=SkintTravelChecks.run(level,a.offset(64,0,0));for(var row:results){checks.add(row);if(!row.getAsJsonObject().get("passed").getAsBoolean())failed=true;}
        SkintTravelChecks.point(level,a);SkintTravelChecks.point(level,b);SkintTravelChecks.point(p.getServer().getLevel(Level.NETHER),n);
        var network=SkintNetwork.get(level);pointA=network.ensure(level,a);pointB=network.ensure(level,b);pointN=network.ensure(p.getServer().getLevel(Level.NETHER),n);
        var data=ProgressionManager.get(p);data.initialize();data.discoveredSkints.clear();data.discoveredSkints.add(pointB.id());data.discoveredSkints.add(pointN.id());
        data.level=25;data.xp=123;data.points=5;data.skillPoints=5;data.skillMilestones=5;data.set(RPGAttribute.STRENGTH,10);data.spark=data.maxSpark();
        p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.getAbilities().invulnerable=true;p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.removeAllEffects();
        p.connection.teleport(a.getX()+.5,a.getY(),a.getZ()+2.5,180,10);level.setDayTime(6000);level.setWeatherParameters(10000,0,false,false);ModNetworking.sync(p);
        persisted.addProperty("origin",pointA.tag().toString());persisted.addProperty("other",pointB.tag().toString());persisted.addProperty("nether",pointN.tag().toString());
    }
    private static void rememberCamera(Minecraft mc)throws Exception{var camera=field(mc.screen,"camera");persisted.addProperty("cameraX",((Number)field(camera,"x")).doubleValue());persisted.addProperty("cameraY",((Number)field(camera,"y")).doubleValue());persisted.addProperty("cameraZoom",((Number)field(camera,"zoom")).doubleValue());}
    private static void restart(Minecraft mc)throws Exception{
        switch(stage){
            case 1->{if(age<20)return;var first=JsonParser.parseString(Files.readString(directory(mc).resolve("first-result.json"))).getAsJsonObject();var saved=first.getAsJsonObject("persisted");persisted.asMap().putAll(saved.asMap());
                pointA=SkintNetwork.Point.read(TagParser.parseTag(saved.get("origin").getAsString()));pointB=SkintNetwork.Point.read(TagParser.parseTag(saved.get("other").getAsString()));pointN=SkintNetwork.Point.read(TagParser.parseTag(saved.get("nether").getAsString()));a=pointA.pos();b=pointB.pos();n=pointN.pos();
                check(!ClientSkintData.active(),"interaction authorization does not survive game restart");server(mc,()->{var p=player(mc);var data=ProgressionManager.get(p);var network=SkintNetwork.get(p.serverLevel());
                    check(data.discoveredSkints.containsAll(Set.of(pointA.id(),pointB.id(),pointN.id())),"personal discovered routes survive a separate Minecraft JVM restart");
                    check(pointA.equals(network.point(pointA.id()))&&pointB.equals(network.point(pointB.id()))&&pointN.equals(network.point(pointN.id())),"point identities and both dimensions survive actual world saving and reload");
                    check(data.level==25&&data.xp==123&&data.points==4&&data.get(RPGAttribute.STRENGTH)==11,"purchased progression survives restart together with routes");});advance();}
            case 2->{if(age<15)return;use(mc,a);advance();}
            case 3->{if(!(mc.screen instanceof SkintScreen)||age<15)return;check(ClientSkintData.destinations().size()==2,"the restored waypoint menu lists the two other personal routes");check(SkintPreviews.get(pointB.id())!=null,"the destination screenshot cache survives a separate game restart");shots.add("08-menu-after-restart.png");advance();}
            case 4->{if(age<10)return;click(mc,100,112,640,360);advance();}
            case 5->{if(!(mc.screen instanceof CharacterScreen)||age<15)return;shots.add("09-progression-after-restart.png");button(mc,"SkillTreeButton");advance();}
            case 6->{if(!(mc.screen instanceof SkillTreeScreen)||!ClientSkillTree.ready||age<20)return;var camera=field(mc.screen,"camera");check(((Number)field(camera,"x")).doubleValue()==persisted.get("cameraX").getAsDouble()&&((Number)field(camera,"y")).doubleValue()==persisted.get("cameraY").getAsDouble()&&((Number)field(camera,"zoom")).doubleValue()==persisted.get("cameraZoom").getAsDouble(),"tree camera still restores exact scale and position through the skint after restart");shots.add("10-tree-after-restart.png");advance();}
            case 7->{if(age>15&&shots.isEmpty())finish(mc);}
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
            age++;if(age>400)throw new IllegalStateException("Stage "+stage+" timeout; screen="+mc.screen);
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
            if(stage==0){mc.options.guiScale().set(2);mc.resizeDisplay();language(mc,"ru_ru");advance();return;}
            if(relogin()){restart(mc);return;}
            switch(stage){
                case 1->{if(age<10)return;mc.options.setCameraType(CameraType.FIRST_PERSON);server(mc,()->prepare(mc));advance();}
                case 2->{if(age<20)return;mc.setScreen(new CharacterScreen());check(!(mc.screen instanceof CharacterScreen),"direct character-screen opening outside a skint session is blocked");KeyMapping.click(ClientRegistration.CHARACTER.getKey());advance();}
                case 3->{if(age<5)return;check(!ClientSkintData.managed(mc.screen)&&!ClientSkintData.active(),"the progression hotkey no longer opens the menu in the world");use(mc,a);advance();}
                case 4->{if(!(mc.screen instanceof SkintScreen)||age<15)return;check(ClientSkintData.destinations().size()==2,"real block interaction opens only personally discovered destinations");
                    var state=mc.level.getBlockState(a);var quads=mc.getBlockRenderer().getBlockModel(state).getQuads(state,null,RandomSource.create(0),ModelData.EMPTY,null);double low=100,high=-100;
                    for(var quad:quads){var vertices=quad.getVertices();int stride=vertices.length/4;for(int i=0;i<4;i++){double y=Float.intBitsToFloat(vertices[i*stride+1]);low=Math.min(low,y);high=Math.max(high,y);}}
                    check(quads.size()==44&&Math.abs(low+.125)<1e-5&&Math.abs(high-1.34959836)<1e-5,"native model preserves its geometry and is lowered by exactly two pixels");
                    check(quads.stream().noneMatch(q->q.getSprite().contents().name().getPath().equals("missingno")),"the corrected model textures load without missing materials");
                    check(mc.screen.children().size()==3&&mc.screen.children().stream().map(c->((Button)c).getMessage().getString()).toList().equals(List.of("\u041f\u0440\u043e\u043a\u0430\u0447\u043a\u0430","\u041f\u0435\u0440\u0435\u043c\u0435\u0441\u0442\u0438\u0442\u044c\u0441\u044f","\u0412\u044b\u0439\u0442\u0438")),"the hub has exactly the three requested Russian actions");
                    check(SkintCamera.active()&&mc.gameRenderer.getMainCamera().isDetached(),"right-click activates a third-person camera that renders the actual player");
                    check(mc.gameRenderer.getMainCamera().getPosition().distanceTo(mc.player.getEyePosition())>2,"the side camera moves independently of the player's position");
                    check(SkintPreviews.get(pointA.id())!=null,"the native world frame is saved as the current skint preview");
                    shots.add("01-skint-menu-ru.png");advance();}
                case 5->{if(age<10)return;click(mc,100,112,640,360);advance();}
                case 6->{if(!(mc.screen instanceof CharacterScreen)||age<15)return;check(ClientSkintData.allowed(),"character progression opens through the authorized waypoint menu");var buttons=(Button[])field(mc.screen,"plus");var plus=buttons[RPGAttribute.STRENGTH.ordinal()];click(mc,plus.getX()+4,plus.getY()+4,640,360);advance();}
                case 7->{if(ClientData.get().points!=4||age<15)return;check(ClientData.get().get(RPGAttribute.STRENGTH)==11,"a real progression button buys exactly one attribute point");shots.add("02-progression-at-skint.png");advance();}
                case 8->{if(age<10)return;button(mc,"SkillTreeButton");advance();}
                case 9->{if(!(mc.screen instanceof SkillTreeScreen)||!ClientSkillTree.ready||age<20)return;var screen=(SkillTreeScreen)mc.screen;screen.focusSkill("epicfight:swordmaster");double scale=Math.min(1.5,Math.min(screen.width/640.0,screen.height/360.0));double x=(screen.width-640*scale)/2+320*scale,y=(screen.height-360*scale)/2+194*scale;screen.mouseScrolled(x,y,0,1);screen.mouseClicked(x,y,1);screen.mouseDragged(x+12*scale,y+20*scale,1,12*scale,20*scale);screen.mouseReleased(x+12*scale,y+20*scale,1);rememberCamera(mc);shots.add("03-tree-at-skint.png");advance();}
                case 10->{if(age<10)return;server(mc,()->player(mc).connection.teleport(b.getX()+.5,b.getY(),b.getZ()+2.5,180,10));advance();}
                case 11->{if(ClientSkintData.managed(mc.screen)||ClientSkintData.active()||age<15)return;check(true,"moving out of range closes progression and clears client access");server(mc,()->player(mc).connection.teleport(a.getX()+.5,a.getY(),a.getZ()+2.5,180,10));advance();}
                case 12->{if(age<15)return;use(mc,a);advance();}
                case 13->{if(!(mc.screen instanceof SkintScreen)||age<12)return;select(mc,pointB.id());shots.add("02-teleport-overworld.png");advance();}
                case 14->{if(age<4)return;click(mc,165,194,330,222);advance();}
                case 15->{if(!arrived(mc,b,Level.OVERWORLD)||age<20)return;check(true,"the real travel button teleports to the selected overworld skint");check(!SkintCamera.active()&&mc.options.getCameraType()==CameraType.FIRST_PERSON,"teleportation restores the original camera perspective");shots.add("04-arrival-overworld.png");server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);d.spark=1;d.sparkDepletionNotified=false;p.removeAllEffects();p.setHealth(7);persisted.addProperty("respawnBefore",String.valueOf(p.getRespawnPosition()));});advance();}
                case 16->{if(age<10)return;use(mc,b);advance();}
                case 17->{if(!(mc.screen instanceof SkintScreen)||age<12)return;select(mc,pointN.id());shots.add("03-teleport-nether.png");advance();}
                case 18->{if(age<4)return;click(mc,165,194,330,222);advance();}
                case 19->{if(!arrived(mc,n,Level.NETHER)||age<20)return;server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);check(d.spark>=1&&!p.hasEffect(ModEffects.SPARK_EXHAUSTION),"cross-dimension skint travel succeeds below the normal spark cost and does not apply travel exhaustion");check(d.level==25&&d.xp==123&&d.points==4,"teleportation does not spend progression or award movement XP");check(p.getHealth()<p.getMaxHealth()&&String.valueOf(p.getRespawnPosition()).equals(persisted.get("respawnBefore").getAsString()),"travel does not copy Bonfires healing or change the respawn point");});advance();}
                case 20->{if(age<40)return;shots.add("05-arrival-nether.png");advance();}
                case 21->{if(age<8)return;use(mc,n);advance();}
                case 22->{if(!(mc.screen instanceof SkintScreen)||age<12)return;language(mc,"en_us");advance();}
                case 23->{if(age<15)return;check(mc.screen.getTitle().getString().equals("Wayfinding Skint"),"the new menu has an English translation");shots.add("06-skint-menu-en.png");advance();}
                case 24->{if(age<10)return;select(mc,pointA.id());advance();}
                case 25->{if(age<4)return;click(mc,165,194,330,222);advance();}
                case 26->{if(!arrived(mc,a,Level.OVERWORLD)||age<30)return;check(true,"the discovered return route works across dimensions");shots.add("07-return-to-origin.png");server(mc,()->{var p=player(mc);check(ProgressionManager.get(p).discoveredSkints.containsAll(Set.of(pointA.id(),pointB.id(),pointN.id())),"the player retains all three discovered routes before saving");});advance();}
                case 27->{if(age<20||!shots.isEmpty())return;language(mc,"ru_ru");use(mc,a);advance();}
                case 28->{if(!(mc.screen instanceof SkintScreen)||age<15)return;select(mc,pointB.id());check(SkintPreviews.get(pointB.id())!=null,"a visited destination displays its own saved world preview");shots.add("11-selected-destination-preview.png");advance();}
                case 29->{if(age<15)return;mc.screen.onClose();click(mc,100,186,640,360);advance();}
                case 30->{if(age<15)return;check(mc.screen==null&&!ClientSkintData.active()&&!SkintCamera.active()&&mc.options.getCameraType()==CameraType.FIRST_PERSON,"Exit closes the whole hub, revokes access and restores the original camera");use(mc,a);advance();}
                case 31->{if(!(mc.screen instanceof SkintScreen)||age<15)return;mc.screen.onClose();advance();}
                case 32->{if(age<10)return;check(mc.screen==null&&!SkintCamera.active(),"Escape from the hub also restores the world camera");use(mc,a);advance();}
                case 33->{if(!(mc.screen instanceof SkintScreen)||age<15)return;select(mc,pointB.id());click(mc,204,14,330,222);shots.add("12-empty-end-tab.png");advance();}
                case 34->{if(age<15||!shots.isEmpty())return;mc.setScreen(null);finish(mc);}
                default->{}
            }
        }catch(Throwable error){failed=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_MENU_CLIENT failed stage "+stage,error);finish(mc);}
    }
    @SubscribeEvent public static void render(RenderFrameEvent.Post event){
        if(!enabled()||finished||shots.isEmpty())return;var mc=Minecraft.getInstance();var batch=List.copyOf(shots);shots.clear();
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(directory(mc));for(var name:batch){pixels.writeToFile(directory(mc).resolve(name));var state=new JsonObject();state.addProperty("stage",stage);state.addProperty("age",age);state.addProperty("screen",mc.screen==null?"World":mc.screen.getClass().getSimpleName());state.addProperty("dimension",mc.level.dimension().location().toString());state.addProperty("language",mc.getLanguageManager().getSelected());state.addProperty("authorized",ClientSkintData.active());captures.add(name,state);com.mojang.logging.LogUtils.getLogger().info("SKINT_MENU_CLIENT captured {}",name);}}
        catch(Exception error){failed=true;com.mojang.logging.LogUtils.getLogger().error("SKINT_MENU_CLIENT capture failed",error);}
    }
    private SkintMenuClientSmoke(){}
}
