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
    private static Path directory(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/paths-2.3.19");}
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
    private static int iconIndex;private static String currentIcon="",currentResource="";
    private static final String[][] ICONS={{"epicfight:guard","placeholder"},{"parcool:pole_climb","pole_climb"},{"parcool:hang_on","hang_on"},{"parcool:climb_up","climb_up"},{"parcool:hang_down","hang_down"},{"parcool:slide_down","slide_down"},{"parcool:trick_jump","trick_jump"},{"parcool:horizontal_wall_run","horizontal_wall_run"},{"parcool:vault","vault"},{"parcool:wall_run","wall_run"},{"parcool:wall_jump","wall_jump"},{"parcool:skydive","skydive"},{"parcool:fast_run","fast_run"},{"parcool:ride_zipline","ride_zipline"},{"parcool:fast_swim","fast_swim"},{"parcool:hide_in_block","hide_in_block"},{"parcool:charge_jump","charge_jump"},{"parcool:slide","slide"},{"parcool:crawl","crawl"}};
    private static boolean midGlow;private static net.minecraft.client.gui.screens.Screen travelScreen;
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
        var report=new JsonObject();report.addProperty("version","2.3.19");report.addProperty("result",checks.asList().stream().allMatch(c->c.getAsJsonObject().get("passed").getAsBoolean())?"PASSED":"FAILED");report.add("checks",checks);report.addProperty("restart",restart());
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
            if(mc.screen instanceof SkillTreeScreen)away(mc);
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
                    var target=POINT.offset(16,0,0);SkintTravelChecks.point(p.serverLevel(),target);
                    ((dev.rpgprogression.block.TravelSkintBlockEntity)p.serverLevel().getBlockEntity(POINT)).setCustomName("");
                    ((dev.rpgprogression.block.TravelSkintBlockEntity)p.serverLevel().getBlockEntity(target)).setCustomName("Назначение");
                    d.discoveredSkints.clear();d.discoveredSkints.add(SkintNetwork.get(p.serverLevel()).pointAt(p.serverLevel(),target).id());
                    SkillTreeManager.sync(p);
                });advance();}
                case 1->{if(!ClientSkillTree.ready||age<35)return;if(!ClientSkintData.allowed()){if(age%20==0)mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(POINT),net.minecraft.core.Direction.SOUTH,POINT,false));return;}open(mc,false);focus(mc,"parcool:crawl");advance();}
                case 2->{if(age<8)return;check(!tree(mc).readOnly(),"the actual skint authorizes this purchasing tree");check(paths().stream().noneMatch(p->p.parent().equals("parcool:fast_run")&&p.children().contains("parcool:long_jump")),"neither marked section of the removed path exists in the drawing plan");check(paths().stream().filter(SkillTreePaths.Path::fork).count()==9,"all nine paired branches use the supplied double texture");capture="01-locked-paths.png";advance();}
                case 3->{if(age<6||capture!=null)return;buy(mc,"parcool:long_jump");advance();}
                case 4->{if(!ClientData.get().unlockedSkills.contains("parcool:long_jump")||age<18)return;check(!ClientData.get().unlockedSkills.contains("parcool:fast_run")&&ClientData.get().skillPoints==11,"a real click purchases Long Jump for one point before Fast Run");longJump(mc);focus(mc,"parcool:long_jump");capture="02-independent-long-jump.png";advance();}
                case 5->{if(age<8||capture!=null)return;buy(mc,"parcool:crawl");advance();}
                case 6->{if(!ClientData.get().unlockedSkills.contains("parcool:crawl")||age<18)return;focus(mc,"parcool:crawl");check(!ClientData.get().unlockedSkills.contains("parcool:slide")&&paths().stream().filter(p->p.parent().equals("parcool:crawl")&&p.fork()).allMatch(p->p.texture(ClientData.get().unlockedSkills).endsWith("_on")),"Crawl lights its fork while Slide and Dive remain unlearned");check(paths().stream().filter(p->p.parent().equals("parcool:fast_run")).allMatch(p->p.texture(ClientData.get().unlockedSkills).endsWith("_off")),"unlearned Fast Run keeps every outgoing path off");capture="03-source-learned.png";advance();}
                case 7->{if(age<8||capture!=null)return;buy(mc,"parcool:fast_run");advance();}
                case 8->{if(!ClientData.get().unlockedSkills.contains("parcool:fast_run")||age<18)return;focus(mc,"parcool:fast_run");check(!ClientData.get().unlockedSkills.contains("parcool:fast_swim")&&paths().stream().filter(p->p.parent().equals("parcool:fast_run")).allMatch(p->p.texture(ClientData.get().unlockedSkills).endsWith("_on")),"learning Fast Run switches its straight, vertical and reversed fork paths on without buying their children");capture="04-fast-run-paths.png";advance();}
                case 9->{if(age<8||capture!=null)return;buy(mc,"parcool:charge_jump");advance();}
                case 10->{if(!ClientData.get().unlockedSkills.contains("parcool:charge_jump")||age<18)return;focus(mc,"parcool:charge_jump");capture="05-right-fork.png";advance();}
                case 11->{if(age<8||capture!=null)return;buy(mc,"parcool:hang_on");advance();}
                case 12->{if(!ClientData.get().unlockedSkills.contains("parcool:hang_on")||age<18)return;focus(mc,"parcool:hang_on");capture="06-down-fork.png";advance();}
                case 13->{if(age<8||capture!=null)return;points=ClientData.get().skillPoints;ClientSkintData.closeWithCameraReturn();mc.setScreen(null);advance();}
                case 14->{if(age<40)return;open(mc,true);focus(mc,"parcool:slide");var pos=tree(mc).skillPosition("parcool:slide");click(mc,pos[0],pos[1]);advance();}
                case 15->{if(age<10)return;check(tree(mc).readOnly()&&ClientData.get().skillPoints==points&&!ClientData.get().unlockedSkills.contains("parcool:slide"),"the hotkey-style viewing tree still cannot purchase an available child");click(mc,10,10);tree(mc).keyPressed(268,0,0);tree(mc).mouseClicked(sx(mc,320),sy(mc,194),1);tree(mc).mouseDragged(sx(mc,280),sy(mc,164),1,-40*scale(mc),-30*scale(mc));tree(mc).mouseReleased(sx(mc,280),sy(mc,164),1);tree(mc).mouseScrolled(sx(mc,320),sy(mc,194),0,1);savedView=view(mc);capture="07-pan-and-zoom.png";advance();}
                case 16->{if(age<8||capture!=null)return;tree(mc).onClose();open(mc,true);check(view(mc).equals(savedView),"closing and reopening preserves pan and zoom after textured path rendering");check(SkillTreeViewState.read(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("rpgprogression-skill-tree-view.json")).orElseThrow().equals(savedView),"the panned and zoomed view is durably saved for the next game process");mc.setScreen(null);advance();}
                case 17->{if(age<40)return;if(!ClientSkintData.allowed()){if(age%20==0)mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(POINT),net.minecraft.core.Direction.SOUTH,POINT,false));return;}check(mc.screen instanceof SkintScreen,"the ordinary right-click reopens the hub after tree checks");check(mc.screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button).noneMatch(w->((net.minecraft.client.gui.components.Button)w).getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("rpgprogression.skint.rename").getString())),"the hub has no separate rename button");advance();}
                case 18->{if(age<35)return;hubClick(mc,70,38);var box=(net.minecraft.client.gui.components.EditBox)mc.screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.EditBox).findFirst().orElseThrow();box.setValue("");for(char ch:"Островок Невера".toCharArray())mc.screen.charTyped(ch,0);check(box.getValue().equals("Островок Невера"),"clicking the title opens the focused rename field, which accepts Cyrillic typing");hubClick(mc,65,70);advance();}
                case 19->{if(ClientSkintData.origin()==null||!ClientSkintData.origin().name().equals("Островок Невера")||age<10)return;check(mc.screen instanceof SkintScreen&&ClientSkintData.allowed()&&!((Boolean)field(mc.screen,"editing")),"the server-approved name returns the same seated hub to its normal view");capture="10-renamed-hub.png";advance();}
                case 20->{if(capture!=null||age<8)return;hubClick(mc,100,149);check(mc.screen instanceof SkintTravelScreen,"the renamed hub still opens the travel menu");travelScreen=mc.screen;server(mc,()->{var p=player(mc);((dev.rpgprogression.block.TravelSkintBlockEntity)p.serverLevel().getBlockEntity(POINT.offset(16,0,0))).setCustomName("Другая стоянка");SkintManager.snapshot(p,5);});advance();}
                case 21->{if(age<10||ClientSkintData.destinations().stream().noneMatch(p->p.name().equals("Другая стоянка")))return;check(mc.screen==travelScreen,"a live name update preserves the open travel screen");check(mc.screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button).map(w->((net.minecraft.client.gui.components.Button)w).getMessage().getString()).anyMatch("Другая стоянка"::equals),"the destination button displays its updated server label");capture="11-named-travel.png";advance();}
                case 22->{if(capture!=null||age<8)return;mc.screen.onClose();mc.screen.onClose();advance();}
                case 23->{if(age<45)return;server(mc,()->{var p=player(mc);p.connection.teleport(POINT.getX()+.5,POINT.getY(),POINT.getZ()+2.5,180,30);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));p.inventoryMenu.broadcastChanges();});advance();}
                case 24->{if(age<35||!mc.player.getMainHandItem().is(net.minecraft.world.item.Items.WATER_BUCKET))return;bucket(mc);advance();}
                case 25->{if(age<10)return;check(mc.level.getBlockState(POINT).getValue(dev.rpgprogression.block.TravelSkintBlock.WATERLOGGED)&&mc.player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)&&!ClientSkintData.active(),"real client bucket input fills the named skint, returns an empty bucket, and opens no menu");capture="12-waterlogged.png";advance();}
                case 26->{if(capture!=null||age<8)return;bucket(mc);advance();}
                case 27->{if(age<10)return;check(!mc.level.getBlockState(POINT).getValue(dev.rpgprogression.block.TravelSkintBlock.WATERLOGGED)&&mc.player.getMainHandItem().is(net.minecraft.world.item.Items.WATER_BUCKET)&&!ClientSkintData.active(),"real client empty-bucket input drains the skint without opening a menu");server(mc,()->showTemplates(mc));advance();}
                case 28->{if(age<70)return;capture="13-author-structures.png";advance();}
                case 29->{if(capture==null&&age>8){check(midGlow,"a real source-skill purchase rendered an intermediate gold transition frame");open(mc,true);advance();}}
                case 30->{if(age<5||capture!=null)return;if(iconIndex>=ICONS.length){advance();return;}currentIcon=ICONS[iconIndex][0];currentResource=ICONS[iconIndex][1];focus(mc,currentIcon);capture=String.format("icon-%02d-%s.png",iconIndex+1,currentResource);iconIndex++;age=0;}
                case 31->{if(capture!=null||age<8)return;click(mc,10,10);tree(mc).keyPressed(268,0,0);capture="14-wide-tree.png";advance();}
                case 32->{if(capture!=null||age<8)return;var c=field(tree(mc),"camera");set(c,"x",savedView.x());set(c,"y",savedView.y());set(c,"zoom",savedView.zoom());tree(mc).onClose();open(mc,true);check(view(mc).equals(savedView),"the wide frame preserves the same stored camera after icon inspection");finish(mc);}
            }
        }catch(Throwable error){done=true;com.mojang.logging.LogUtils.getLogger().error("SKILL_PATHS failed",error);mc.stop();}
    }
    private static void hubClick(Minecraft mc,double x,double y){double scale=Math.min(mc.screen.width/640.0,mc.screen.height/360.0);double left=0,top=(mc.screen.height-360*scale)/2;mc.screen.mouseClicked(left+x*scale,top+y*scale,0);mc.screen.mouseReleased(left+x*scale,top+y*scale,0);}
    private static void bucket(Minecraft mc){var hand=net.minecraft.world.InteractionHand.MAIN_HAND;mc.gameMode.useItemOn(mc.player,hand,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(POINT),net.minecraft.core.Direction.SOUTH,POINT,false));mc.gameMode.useItem(mc.player,hand);}
    private static void showTemplates(Minecraft mc){
        var p=player(mc);var level=p.serverLevel();var names=List.of("skint_main_base","skint_main_sand","skint_main_red_sand","skint_main_mossy","skint_cave_1","skint_cave_2","skint_nether","skint_end");
        for(int x=76;x<128;x++)for(int z=30;z<60;z++)level.setBlock(new BlockPos(x,199,z),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),2);
        int i=0;for(String name:names){var pos=new BlockPos(80+(i%4)*12,200,34+(i/4)*12);i++;level.getStructureManager().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("rpgprogression",name)).orElseThrow().placeInWorld(level,pos,pos,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setIgnoreEntities(true),level.random,2);}
        level.setDayTime(6000);p.connection.teleport(102,233,75,180,53);
    }
    private static boolean closeColor(int a,int b){for(int shift:new int[]{0,8,16})if(Math.abs(((a>>>shift)&255)-((b>>>shift)&255))>1)return false;return true;}
    private static void iconPixels(Minecraft mc,NativeImage image)throws Exception{
        var p=tree(mc).skillPosition(currentIcon);var v=view(mc);var node=ClientSkillTree.nodes.stream().filter(n->n.id().equals(currentIcon)).findFirst().orElseThrow();boolean masked=!ClientData.get().unlockedSkills.contains(currentIcon)&&!SkillTreeManager.canUnlock(ClientData.get(),node);int tested=0,matches=0;
        try(var input=mc.getResourceManager().open(GuiTextures.texture("icons/"+currentResource));var source=NativeImage.read(input)){
            check(source.getWidth()==64&&source.getHeight()==64,"native resource size remains 64x64 for "+currentResource);
            for(int u=10;u<=54;u+=4)for(int w=10;w<=54;w+=4){if(u>=44&&w>=44)continue;int expected=source.getPixelRGBA(u,w);if((expected>>>24)!=255)continue;
                int r=expected&255,g=(expected>>>8)&255,b=(expected>>>16)&255;if(!currentResource.equals("placeholder")&&r+g+b<200)continue;
                if(masked)expected=0xFF000000|Math.round(r*.6f)|(Math.round(g*.6f)<<8)|(Math.round(b*.6f)<<16);
                double x=p[0]+((u+.5)/64-.5)*40*v.zoom(),y=p[1]+((w+.5)/64-.5)*40*v.zoom();int px=(int)(sx(mc,x)*image.getWidth()/mc.screen.width),py=(int)(sy(mc,y)*image.getHeight()/mc.screen.height);boolean found=false;
                for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(closeColor(image.getPixelRGBA(px+dx,py+dy),expected))found=true;
                tested++;if(found)matches++;
            }
        }check(tested>=3&&matches==tested,"rendered subject pixels match the supplied "+currentResource+" icon with its real lock shading: "+matches+"/"+tested);
    }
    private static void framePixels(Minecraft mc,NativeImage image)throws Exception{
        for(int x:new int[]{224,416}){int px=(int)(sx(mc,x)*image.getWidth()/mc.screen.width),py=(int)(sy(mc,316)*image.getHeight()/mc.screen.height);check(image.getPixelRGBA(px,py)==0xFF191920,"the wide footer has an opaque backdrop beside the points plaque at x="+x);}
        int tested=0,matches=0;try(var input=mc.getResourceManager().open(GuiTextures.texture("skill_tree"));var source=NativeImage.read(input)){
            check(source.getWidth()==640&&source.getHeight()==360,"the native frame keeps the full supplied 640x360 canvas");
            for(int x=28;x<612;x+=13)for(int y=20;y<348;y+=11){if(x>=240&&x<=400&&y>=308)continue;int color=source.getPixelRGBA(x,y);if((color>>>24)!=255)continue;
                int px=(int)(sx(mc,x+.5)*image.getWidth()/mc.screen.width),py=(int)(sy(mc,y+.5)*image.getHeight()/mc.screen.height);boolean found=false;for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(image.getPixelRGBA(px+dx,py+dy)==color)found=true;tested++;if(found)matches++;
            }
        }check(tested>20&&tested==matches,"all sampled wide-frame ornaments remain visible, correctly positioned and unclipped: "+matches+"/"+tested);
    }
    private static void pixels(Minecraft mc,NativeImage image)throws Exception{
        var v=view(mc);int tested=0,matches=0;
        for(var path:paths()){
            if(path.length()/60*v.zoom()*scale(mc)*image.getWidth()/mc.screen.width<1)continue;
            try(var input=mc.getResourceManager().open(GuiTextures.texture(path.texture(ClientData.get().unlockedSkills)));var source=NativeImage.read(input)){
                int u=-1,w=-1;for(int x=12;x<25&&u<0;x++)for(int y=source.getHeight()/2-3;y<=source.getHeight()/2+3;y++)if((source.getPixelRGBA(x,y)>>>24)==255){u=x;w=y;break;}if(u<0)continue;
                double r=Math.toRadians(path.rotation()),lx=(u+.5)/60*path.length(),ly=(w+.5)/source.getHeight()*path.breadth()-path.breadth()/2;
                double x=54+(path.x()+lx*Math.cos(r)-ly*Math.sin(r)-v.x())*v.zoom(),y=74+(path.y()+lx*Math.sin(r)+ly*Math.cos(r)-v.y())*v.zoom();if(x<58||x>582||y<78||y>300)continue;
                int px=(int)(sx(mc,x)*image.getWidth()/mc.screen.width),py=(int)(sy(mc,y)*image.getHeight()/mc.screen.height),expected=source.getPixelRGBA(u,w);boolean found=false;
                for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(image.getPixelRGBA(px+dx,py+dy)==expected)found=true;tested++;if(found)matches++;
            }
        }check(tested==matches&&(tested>0||40.0/60*v.zoom()*scale(mc)*image.getWidth()/mc.screen.width<1),"native framebuffer matches supplied opaque path pixels at resolvable scale for "+capture+": "+matches+"/"+tested);
    }
    @SubscribeEvent public static void render(RenderFrameEvent.Post event){
        if(!enabled())return;var mc=Minecraft.getInstance();
        if(!midGlow&&mc.screen instanceof SkillTreeScreen tree&&ClientData.get().unlockedSkills.contains("parcool:crawl"))try{
            var glow=(SkillPathGlow)field(tree,"pathGlow");float alpha=glow.alpha("parcool:crawl",ClientData.get().unlockedSkills,System.nanoTime());
            if(alpha>.15f&&alpha<.85f){Files.createDirectories(directory(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(directory(mc).resolve("09-glow-midpoint.png"));}midGlow=true;check(true,"a real learned source renders an intermediate path alpha of "+alpha);}
        }catch(Exception e){throw new IllegalStateException(e);}
        if(capture==null)return;
        try{Files.createDirectories(directory(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){if(mc.screen instanceof SkillTreeScreen){if(capture.startsWith("icon-"))iconPixels(mc,image);else pixels(mc,image);if(capture.equals("14-wide-tree.png"))framePixels(mc,image);}image.writeToFile(directory(mc).resolve(capture));}capture=null;}
        catch(Exception error){done=true;com.mojang.logging.LogUtils.getLogger().error("SKILL_PATHS capture failed",error);mc.stop();}
    }
    @SubscribeEvent public static void frameStart(RenderFrameEvent.Pre event){if(enabled()&&Minecraft.getInstance().screen instanceof SkillTreeScreen)try{away(Minecraft.getInstance());}catch(Exception error){throw new IllegalStateException(error);}}
    private SkillPathsClientSmoke(){}
}
