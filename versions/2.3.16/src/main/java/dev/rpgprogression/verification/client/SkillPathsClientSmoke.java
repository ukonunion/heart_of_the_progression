package dev.rpgprogression.verification.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import dev.rpgprogression.client.*;
import dev.rpgprogression.client.gui.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import dev.rpgprogression.skint.*;
import dev.rpgprogression.verification.SkintTravelChecks;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class SkillPathsClientSmoke {
    private static final JsonArray checks=new JsonArray();
    private static final BlockPos POINT=new BlockPos(48,200,4);
    private static boolean loading,done;private static int stage,age,boot;private static String capture;
    private static CompletableFuture<?> pending;private static SkillTreeViewState savedView;private static int points;
    private static Path directory(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/paths-2.3.16");}
    private static boolean restart(){return Boolean.getBoolean("rpgprogression.skillPathsRestart");}
    private static boolean enabled(){return Boolean.getBoolean("rpgprogression.skillPathsSmoke")&&!done;}
    private static void check(boolean ok,String text){SkintTravelChecks.check(checks,ok,text);}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static java.lang.reflect.Field member(Object o,String name)throws Exception{for(Class<?> type=o.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static Object field(Object o,String name)throws Exception{return member(o,name).get(o);}
    private static void set(Object o,String name,Object value)throws Exception{member(o,name).set(o,value);}
    private static double number(Object o,String name)throws Exception{return ((Number)field(o,name)).doubleValue();}
    private static void server(Minecraft mc,Runnable action){pending=CompletableFuture.runAsync(action,mc.getSingleplayerServer());}
    private static void advance(){stage++;age=0;com.mojang.logging.LogUtils.getLogger().info("SKILL_PATHS_STAGE {}",stage);}
    private static SkillTreeScreen tree(Minecraft mc){return (SkillTreeScreen)mc.screen;}
    private static double scale(Minecraft mc){return Math.min(1.5,Math.min(mc.screen.width/640.0,mc.screen.height/360.0));}
    private static double sx(Minecraft mc,double x){return (mc.screen.width-640*scale(mc))/2+x*scale(mc);}
    private static double sy(Minecraft mc,double y){return (mc.screen.height-360*scale(mc))/2+y*scale(mc);}
    private static void click(Minecraft mc,double x,double y){mc.screen.mouseClicked(sx(mc,x),sy(mc,y),0);mc.screen.mouseReleased(sx(mc,x),sy(mc,y),0);}
    private static void away(Minecraft mc)throws Exception{
        mc.mouseHandler.releaseMouse();set(mc.mouseHandler,"xpos",sx(mc,638)*mc.getWindow().getGuiScale());set(mc.mouseHandler,"ypos",sy(mc,358)*mc.getWindow().getGuiScale());
    }
    private static void focus(Minecraft mc,String id)throws Exception{tree(mc).focusSkill(id);click(mc,10,10);away(mc);}
    private static void buy(Minecraft mc,String id)throws Exception{focus(mc,id);var p=tree(mc).skillPosition(id);click(mc,p[0],p[1]);}
    private static SkillTreeViewState view(Minecraft mc)throws Exception{var c=field(mc.screen,"camera");return new SkillTreeViewState(number(c,"x"),number(c,"y"),(float)number(c,"zoom"));}
    private static void open(Minecraft mc,boolean readOnly){mc.setScreen(new SkillTreeScreen(new CharacterScreen(null,readOnly)));}
    private static List<SkillTreePaths.Path> paths(){var entries=ClientSkillTree.nodes.stream().map(n->new SkillTreeLayout.Entry(n.id(),n.parents())).toList();return SkillTreePaths.arrange(SkillTreeLayout.arrangeDesigned(entries),entries);}
    private static void longJump(Minecraft mc)throws Exception{
        var p=mc.player;var ability=com.alrex.parcool.common.attachment.common.Parkourability.get(p);var leap=ability.get(com.alrex.parcool.common.action.impl.CatLeap.class);var run=ability.get(com.alrex.parcool.common.action.impl.FastRun.class);var props=ability.getAdditionalProperties();var key=com.alrex.parcool.client.input.KeyRecorder.keySneak;
        Object notDoing=field(run,"notDoingTick"),notSprinting=field(props,"notSprintingTick"),pressed=field(key,"pressed"),released=field(key,"released"),ready=field(leap,"ready"),readyTick=field(leap,"readyTick");boolean ground=p.onGround();var movement=p.getDeltaMovement();
        try{
            set(run,"notDoingTick",100);set(props,"notSprintingTick",0);set(key,"pressed",true);set(key,"released",false);set(leap,"ready",false);set(leap,"readyTick",0);
            leap.onClientTick(p,ability);check(((Number)field(leap,"readyTick")).intValue()==1,"native CatLeap prepares from ordinary sprint while Fast Run remains locked");
            set(key,"pressed",false);set(key,"released",true);p.setOnGround(true);p.setDeltaMovement(.1,0,0);
            check(leap.canStart(p,ability,java.nio.ByteBuffer.allocate(32))&&!dev.rpgprogression.compat.ParCoolBridge.blocked(p,leap),"the unlocked independent long jump passes native start conditions and RPG action authorization");
        }finally{set(run,"notDoingTick",notDoing);set(props,"notSprintingTick",notSprinting);set(key,"pressed",pressed);set(key,"released",released);set(leap,"ready",ready);set(leap,"readyTick",readyTick);p.setOnGround(ground);p.setDeltaMovement(movement);}
    }
    private static void finish(Minecraft mc)throws Exception{
        var report=new JsonObject();report.addProperty("version","2.3.16");report.addProperty("result",checks.asList().stream().allMatch(c->c.getAsJsonObject().get("passed").getAsBoolean())?"PASSED":"FAILED");report.add("checks",checks);report.addProperty("restart",restart());
        if(savedView!=null){var v=new JsonObject();v.addProperty("x",savedView.x());v.addProperty("y",savedView.y());v.addProperty("zoom",savedView.zoom());report.add("view",v);}
        var mods=new JsonArray();for(var m:net.neoforged.fml.ModList.get().getMods()){var item=new JsonObject();item.addProperty("id",m.getModId());item.addProperty("version",m.getVersion().toString());mods.add(item);}report.add("loadedMods",mods);
        Files.createDirectories(directory(mc));Files.writeString(directory(mc).resolve(restart()?"restart-result.json":"first-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        done=true;mc.stop();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!enabled())return;var mc=Minecraft.getInstance();
        try{
            if(mc.player==null||mc.getSingleplayerServer()==null){if(++boot>6000)throw new IllegalStateException("Startup timeout");if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var prev=mc.screen;mc.createWorldOpenFlows().openWorld(System.getProperty("rpgprogression.smokeWorld"),()->mc.setScreen(prev));}return;}
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(mc.getOverlay()!=null)return;if(++age>300)throw new IllegalStateException("Stage timeout "+stage);
            if(mc.screen!=null)away(mc);
            if(restart()){
                switch(stage){
                    case 0->{if(age<30)return;mc.options.guiScale().set(2);mc.resizeDisplay();open(mc,true);advance();}
                    case 1->{if(!ClientSkillTree.ready||age<20)return;var report=JsonParser.parseString(Files.readString(directory(mc).resolve("first-result.json"))).getAsJsonObject();var v=report.getAsJsonObject("view");savedView=new SkillTreeViewState(v.get("x").getAsDouble(),v.get("y").getAsDouble(),v.get("zoom").getAsFloat());check(view(mc).equals(savedView),"a full client restart restores the same pan and zoom with the textured renderer");check(ClientSkillTree.nodes.stream().filter(n->n.id().equals("parcool:long_jump")).findFirst().orElseThrow().parents().isEmpty(),"the server still sends Long Jump without a prerequisite after restart");check(ClientData.get().unlockedSkills.containsAll(Set.of("parcool:long_jump","parcool:fast_run","parcool:crawl")),"previous purchases survive the full game restart");capture="08-after-restart.png";advance();}
                    case 2->{if(age>5&&capture==null)finish(mc);}
                }return;
            }
            switch(stage){
                case 0->{if(age<30)return;mc.options.guiScale().set(2);mc.options.hideGui=true;mc.resizeDisplay();mc.setScreen(null);server(mc,()->{
                    var p=player(mc);SkintManager.close(p,true);SkintTravelChecks.point(p.serverLevel(),POINT);p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.removeAllEffects();p.getInventory().clearContent();p.connection.teleport(POINT.getX()+.5,POINT.getY(),POINT.getZ()+2.5,180,10);
                    var d=ProgressionManager.get(p);d.initialize();d.unlockedSkills.clear();d.parcoolSkillsImported=d.epicSkillsImported=true;d.skillPoints=12;d.stamina=d.maxStamina();d.exhausted=false;d.spark=d.maxSpark();d.dirty=true;
                    var catalog=SkillTreeManager.catalog(p);check(catalog.size()==43&&catalog.stream().filter(n->n.parents().isEmpty()).count()==10,"the real server catalog retains 43 nodes and has ten independent starting skills");check(catalog.stream().filter(n->n.id().equals("parcool:long_jump")).findFirst().orElseThrow().parents().isEmpty(),"the real server removed Fast Run as a Long Jump prerequisite");
                    SkillTreeManager.sync(p);
                });advance();}
                case 1->{if(!ClientSkillTree.ready||age<35)return;if(!ClientSkintData.allowed()){if(age%20==0)mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(POINT),net.minecraft.core.Direction.SOUTH,POINT,false));return;}open(mc,false);focus(mc,"parcool:crawl");advance();}
                case 2->{if(age<8)return;check(!tree(mc).readOnly(),"the actual skint authorizes this purchasing tree");check(paths().stream().noneMatch(p->p.parent().equals("parcool:fast_run")&&p.children().contains("parcool:long_jump")),"neither marked section of the removed path exists in the drawing plan");check(paths().stream().filter(SkillTreePaths.Path::fork).count()==9,"all nine paired branches use the supplied double texture");capture="01-locked-paths.png";advance();}
                case 3->{if(age<6||capture!=null)return;buy(mc,"parcool:long_jump");advance();}
                case 4->{if(!ClientData.get().unlockedSkills.contains("parcool:long_jump")||age<8)return;check(!ClientData.get().unlockedSkills.contains("parcool:fast_run")&&ClientData.get().skillPoints==11,"a real click purchases Long Jump for one point before Fast Run");longJump(mc);focus(mc,"parcool:long_jump");capture="02-independent-long-jump.png";advance();}
                case 5->{if(age<8||capture!=null)return;buy(mc,"parcool:crawl");advance();}
                case 6->{if(!ClientData.get().unlockedSkills.contains("parcool:crawl")||age<8)return;focus(mc,"parcool:crawl");check(!ClientData.get().unlockedSkills.contains("parcool:slide")&&paths().stream().filter(p->p.parent().equals("parcool:crawl")&&p.fork()).allMatch(p->p.texture(ClientData.get().unlockedSkills).endsWith("_on")),"Crawl lights its fork while Slide and Dive remain unlearned");check(paths().stream().filter(p->p.parent().equals("parcool:fast_run")).allMatch(p->p.texture(ClientData.get().unlockedSkills).endsWith("_off")),"unlearned Fast Run keeps every outgoing path off");capture="03-source-learned.png";advance();}
                case 7->{if(age<8||capture!=null)return;buy(mc,"parcool:fast_run");advance();}
                case 8->{if(!ClientData.get().unlockedSkills.contains("parcool:fast_run")||age<8)return;focus(mc,"parcool:fast_run");check(!ClientData.get().unlockedSkills.contains("parcool:fast_swim")&&paths().stream().filter(p->p.parent().equals("parcool:fast_run")).allMatch(p->p.texture(ClientData.get().unlockedSkills).endsWith("_on")),"learning Fast Run switches its straight, vertical and reversed fork paths on without buying their children");capture="04-fast-run-paths.png";advance();}
                case 9->{if(age<8||capture!=null)return;buy(mc,"parcool:charge_jump");advance();}
                case 10->{if(!ClientData.get().unlockedSkills.contains("parcool:charge_jump")||age<8)return;focus(mc,"parcool:charge_jump");capture="05-right-fork.png";advance();}
                case 11->{if(age<8||capture!=null)return;buy(mc,"parcool:hang_on");advance();}
                case 12->{if(!ClientData.get().unlockedSkills.contains("parcool:hang_on")||age<8)return;focus(mc,"parcool:hang_on");capture="06-down-fork.png";advance();}
                case 13->{if(age<8||capture!=null)return;points=ClientData.get().skillPoints;ClientSkintData.closeWithCameraReturn();mc.setScreen(null);advance();}
                case 14->{if(age<40)return;open(mc,true);focus(mc,"parcool:slide");var pos=tree(mc).skillPosition("parcool:slide");click(mc,pos[0],pos[1]);advance();}
                case 15->{if(age<10)return;check(tree(mc).readOnly()&&ClientData.get().skillPoints==points&&!ClientData.get().unlockedSkills.contains("parcool:slide"),"the hotkey-style viewing tree still cannot purchase an available child");click(mc,10,10);tree(mc).keyPressed(268,0,0);tree(mc).mouseClicked(sx(mc,320),sy(mc,194),1);tree(mc).mouseDragged(sx(mc,280),sy(mc,164),1,-40*scale(mc),-30*scale(mc));tree(mc).mouseReleased(sx(mc,280),sy(mc,164),1);tree(mc).mouseScrolled(sx(mc,320),sy(mc,194),0,1);savedView=view(mc);capture="07-pan-and-zoom.png";advance();}
                case 16->{if(age<8||capture!=null)return;tree(mc).onClose();open(mc,true);check(view(mc).equals(savedView),"closing and reopening preserves pan and zoom after textured path rendering");check(SkillTreeViewState.read(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("rpgprogression-skill-tree-view.json")).orElseThrow().equals(savedView),"the panned and zoomed view is durably saved for the next game process");finish(mc);}
            }
        }catch(Throwable error){done=true;com.mojang.logging.LogUtils.getLogger().error("SKILL_PATHS failed",error);mc.stop();}
    }
    private static void pixels(Minecraft mc,NativeImage image)throws Exception{
        var v=view(mc);int tested=0,matches=0;
        for(var path:paths()){
            if(path.length()/60*v.zoom()*scale(mc)*image.getWidth()/mc.screen.width<1)continue;
            try(var input=mc.getResourceManager().open(GuiTextures.texture(path.texture(ClientData.get().unlockedSkills)));var source=NativeImage.read(input)){
                int u=-1,w=-1;for(int x=12;x<25&&u<0;x++)for(int y=source.getHeight()/2-3;y<=source.getHeight()/2+3;y++)if((source.getPixelRGBA(x,y)>>>24)==255){u=x;w=y;break;}if(u<0)continue;
                double r=Math.toRadians(path.rotation()),lx=(u+.5)/60*path.length(),ly=(w+.5)/source.getHeight()*path.breadth()-path.breadth()/2;
                double x=138+(path.x()+lx*Math.cos(r)-ly*Math.sin(r)-v.x())*v.zoom(),y=74+(path.y()+lx*Math.sin(r)+ly*Math.cos(r)-v.y())*v.zoom();if(x<142||x>498||y<78||y>310)continue;
                int px=(int)(sx(mc,x)*image.getWidth()/mc.screen.width),py=(int)(sy(mc,y)*image.getHeight()/mc.screen.height),expected=source.getPixelRGBA(u,w);boolean found=false;
                for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(image.getPixelRGBA(px+dx,py+dy)==expected)found=true;tested++;if(found)matches++;
            }
        }check(tested==matches&&(tested>0||40.0/60*v.zoom()*scale(mc)*image.getWidth()/mc.screen.width<1),"native framebuffer matches supplied opaque path pixels at resolvable scale for "+capture+": "+matches+"/"+tested);
    }
    @SubscribeEvent public static void render(RenderFrameEvent.Post event){
        if(!enabled()||capture==null||!(Minecraft.getInstance().screen instanceof SkillTreeScreen))return;var mc=Minecraft.getInstance();
        try{Files.createDirectories(directory(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels(mc,image);image.writeToFile(directory(mc).resolve(capture));}capture=null;}
        catch(Exception error){done=true;com.mojang.logging.LogUtils.getLogger().error("SKILL_PATHS capture failed",error);mc.stop();}
    }
    @SubscribeEvent public static void frameStart(RenderFrameEvent.Pre event){if(enabled()&&Minecraft.getInstance().screen!=null)try{away(Minecraft.getInstance());}catch(Exception error){throw new IllegalStateException(error);}}
    private SkillPathsClientSmoke(){}
}
