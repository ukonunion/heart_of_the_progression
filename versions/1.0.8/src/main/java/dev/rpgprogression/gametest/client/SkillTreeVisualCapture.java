package dev.rpgprogression.gametest.client;

import com.mojang.logging.LogUtils;
import dev.rpgprogression.client.*;
import dev.rpgprogression.client.gui.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Native framebuffer captures and menu actions in isolated development-world copies. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class SkillTreeVisualCapture {
    private static boolean loading;
    private static volatile boolean failed;
    private static int ticks;
    private static String profile,parkourId,epicId;
    private static String hoveredId,originalLanguage;
    private static int blockedPoints,dragPoints;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static final List<String> captures=new ArrayList<>();
    private static final com.google.gson.JsonObject states=new com.google.gson.JsonObject();
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable error){failed=true;LogUtils.getLogger().error("HEART_TREE_VISUAL server failure",error);}});}
    private static void check(boolean ok,String message){if(!ok)failed=true;LogUtils.getLogger().info("HEART_TREE_VISUAL {} {}",ok?"verified":"FAILED",message);}
    private static void shot(String name){captures.add(name+".png");}
    private static double scale(Screen s){return Math.min(1.5,Math.min(s.width/640.0,s.height/360.0));}
    private static double sx(Screen s,double x){return (s.width-640*scale(s))/2+x*scale(s);}
    private static double sy(Screen s,double y){return (s.height-360*scale(s))/2+y*scale(s);}
    private static void cursorAway(Minecraft mc){
        try{for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,(name.equals("xpos")?mc.screen.width-5:mc.screen.height-5)*mc.getWindow().getGuiScale());}}
        catch(Exception error){throw new IllegalStateException(error);}
    }
    private static void click(Minecraft mc,double x,double y){var s=mc.screen;check(s.mouseClicked(sx(s,x),sy(s,y),0),"menu action "+x+","+y);s.mouseReleased(sx(s,x),sy(s,y),0);}
    private static void cursor(Minecraft mc,double x,double y){
        try{var s=mc.screen;for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,(name.equals("xpos")?sx(s,x):sy(s,y))*mc.getWindow().getGuiScale());}}
        catch(Exception error){throw new IllegalStateException(error);}
    }
    private static void hover(Minecraft mc,String id){var s=(SkillTreeScreen)mc.screen;s.focusSkill(id);var p=s.skillPosition(id);cursor(mc,p[0],p[1]);hoveredId=id;}
    private static SkillInfoTooltip info(Minecraft mc){try{var f=SkillTreeScreen.class.getDeclaredField("info");f.setAccessible(true);return (SkillInfoTooltip)f.get(mc.screen);}catch(Exception error){throw new IllegalStateException(error);}}
    private static void language(Minecraft mc,String value){mc.getLanguageManager().setSelected(value);mc.options.languageCode=value;reload=mc.reloadResourcePacks();}
    private static void longest(Minecraft mc){
        var node=ClientSkillTree.nodes.stream().max(Comparator.comparingInt(n->SkillDescriptions.describe(n).stream().mapToInt(c->c.getString().length()).sum()+n.parents().size()*60)).orElseThrow();
        tree(mc,node.branch());hover(mc,node.id());
    }
    private static void tree(Minecraft mc,int branch){mc.setScreen(new SkillTreeScreen(new CharacterScreen(),branch));cursorAway(mc);}
    private static String details(Minecraft mc,int branch){
        var node=ClientSkillTree.nodes.stream().filter(n->n.branch()==branch&&SkillTreeManager.canUnlock(ClientData.get(),n)).findFirst().orElseThrow();
        hover(mc,node.id());return node.id();
    }
    private static void learn(Minecraft mc){
        var s=(SkillTreeScreen)mc.screen;var p=s.skillPosition(hoveredId);click(mc,p[0],p[1]);
        check(mc.screen==s&&s.children().stream().noneMatch(c->c instanceof SkillTreeButton),"one icon click keeps the tree and opens no duplicate detail window");
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        boolean patch=Boolean.getBoolean("rpgprogression.skillInfoCapture");
        if(!(patch||Boolean.getBoolean("rpgprogression.treeVisualCapture"))||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld"));return;}
        if(loading&&mc.player==null){
            String label=mc.screen instanceof BackupConfirmScreen?Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;
        }
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        if(reload!=null){if(!reload.isDone())return;reload.join();reload=null;}
        ticks++;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        boolean parcool=SkillTreeManager.parcool(),epic=SkillTreeManager.epic(),irons=dev.rpgprogression.compat.IronsCompat.present();
        if(ticks==20){
            profile=parcool&&epic&&irons?"all":parcool?"parcool":epic?"epicfight":irons?"irons":"standalone";
            check(System.getProperty("rpgprogression.smokeWorld").endsWith("-"+profile),"actual optional-mod profile "+profile);
            mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.bobView().set(false);
            originalLanguage=mc.getLanguageManager().getSelected();if(patch)language(mc,"ru_ru");
            server(mc,()->{var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.removeAllEffects();p.stopRiding();p.getInventory().clearContent();
                if(epic)EpicTreeClientCheck.prepareFixture(p);
                var d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();d.level=20;d.skillMilestones=4;d.skillPoints=4;
                ProgressionManager.refresh(p,10,false);SkillTreeManager.refresh(p,d);ModNetworking.sync(p);});
        }
        if(ticks==60)tree(mc,0);
        if(ticks==80){check(ClientSkillTree.ready&&ClientData.get().skillPoints==4,"server catalog and skill point balance");shot("01-own");}
        if(ticks==85&&parcool)tree(mc,1);
        if(ticks==105&&parcool)mc.screen.keyPressed(268,0,0);
        if(ticks==110&&parcool)shot("02-parcool");
        if(ticks==115&&parcool)parkourId=details(mc,1);
        if(ticks==130&&parcool)shot("03-parcool-description");
        if(ticks==135&&parcool)learn(mc);
        if(ticks==155&&parcool){check(ClientData.get().unlockedSkills.contains(parkourId)&&ClientData.get().skillPoints==3,"ParCool skill learned by one icon click for one point");shot("04-parcool-learned");}
        if(ticks==160&&parcool)mc.screen.keyPressed(256,0,0);
        if(ticks==170&&epic)tree(mc,2);
        if(ticks==190&&epic)mc.screen.keyPressed(268,0,0);
        if(ticks==195&&epic)shot("05-epicfight");
        if(ticks==200&&epic){epicId=details(mc,2);server(mc,()->EpicTreeClientCheck.blockEquipForFixture(player(mc),epicId));}
        if(ticks==215&&epic)shot("06-epicfight-description");
        if(ticks==220&&epic)learn(mc);
        if(ticks==240&&epic){check(ClientData.get().unlockedSkills.contains(epicId)&&ClientData.get().skillPoints==(parcool?2:3),"Epic skill learned by one icon click for one point");shot("07-epicfight-actions");}
        if(ticks==265&&epic){
            var action=mc.screen.children().stream().filter(c->c instanceof SkillTreeActionButton b&&b.active).map(c->(SkillTreeActionButton)c).findFirst().orElseThrow();
            click(mc,action.getX()+50,action.getY()+10);
        }
        if(ticks==290&&epic)server(mc,()->EpicTreeClientCheck.equipped(player(mc),epicId,SkillTreeVisualCapture::check));
        if(ticks==295&&epic)shot("08-epicfight-equipped");
        if(ticks==300&&epic)mc.screen.keyPressed(256,0,0);
        if(ticks==310&&irons)tree(mc,3);
        if(ticks==330&&irons)shot("09-irons");
        if(ticks==340){mc.options.guiScale().set(3);mc.resizeDisplay();tree(mc,epic?2:parcool?1:irons?3:0);mc.screen.keyPressed(268,0,0);}
        if(ticks==360)shot("10-scale3");
        if(ticks==365){mc.screen.keyPressed(256,0,0);check(mc.screen instanceof CharacterScreen,"Escape returns to character menu without a vanilla button");mc.setScreen(null);mc.options.guiScale().set(2);mc.resizeDisplay();}
        if(ticks==375&&(!patch||!parcool&&!epic)){LogUtils.getLogger().info("HEART_TREE_VISUAL_RESULT {} {}",profile,failed?"FAILED":"PASSED");mc.stop();}
        if(!patch)return;
        if(ticks==380){longest(mc);}
        if(ticks==395){check(info(mc)!=null,"longest real skill description has the custom frame");shot("11-long-description-ru");}
        if(ticks==400){var s=mc.screen;var t=info(mc);s.mouseScrolled(sx(s,t.x()+50),sy(s,t.y()+85),-10);}
        if(ticks==410){var t=info(mc);check(t.scroll()==t.maximumScroll(),"complete description can be read by scrolling without a modal");shot("12-long-description-ru-bottom");}
        if(ticks==415&&epic){
            var blocked=ClientSkillTree.nodes.stream().filter(n->n.branch()==2&&!ClientData.get().unlockedSkills.containsAll(n.parents())).findFirst().orElseThrow();
            hover(mc,blocked.id());blockedPoints=ClientData.get().skillPoints;var p=((SkillTreeScreen)mc.screen).skillPosition(blocked.id());click(mc,p[0],p[1]);
        }
        if(ticks==435&&epic){check(ClientData.get().skillPoints==blockedPoints&&!ClientData.get().unlockedSkills.contains(hoveredId),"locked prerequisites prevent direct learning and spending");shot("13-locked-requirements");}
        if(ticks==440&&epic){
            var node=ClientSkillTree.nodes.stream().filter(n->n.branch()==2&&SkillTreeManager.canUnlock(ClientData.get(),n)).findFirst().orElseThrow();hover(mc,node.id());
            var s=(SkillTreeScreen)mc.screen;var p=s.skillPosition(node.id());dragPoints=ClientData.get().skillPoints;
            s.mouseClicked(sx(s,p[0]),sy(s,p[1]),0);s.mouseDragged(sx(s,p[0]+24),sy(s,p[1]),0,24*scale(s),0);s.mouseReleased(sx(s,p[0]+24),sy(s,p[1]),0);cursorAway(mc);
        }
        if(ticks==460&&epic)check(ClientData.get().skillPoints==dragPoints&&!ClientData.get().unlockedSkills.contains(hoveredId),"panning from a node does not accidentally learn it");
        if(ticks==465)language(mc,"en_us");
        if(ticks==480){tree(mc,parcool?1:2);mc.screen.keyPressed(268,0,0);}
        if(ticks==495)shot("14-tree-en-no-labels");
        if(ticks==500){tree(mc,parcool?1:2);details(mc,parcool?1:2);}
        if(ticks==515)shot("15-info-en");
        if(ticks==520){longest(mc);}
        if(ticks==535)shot("16-long-description-en");
        if(ticks==540){mc.options.guiScale().set(3);mc.resizeDisplay();longest(mc);}
        if(ticks==555)shot("17-info-en-scale3");
        if(ticks==560){mc.screen.keyPressed(256,0,0);check(mc.screen instanceof CharacterScreen,"Escape returns immediately to character menu");language(mc,originalLanguage);}
        if(ticks==575){mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(null);LogUtils.getLogger().info("HEART_TREE_VISUAL_RESULT {} {}",profile,failed?"FAILED":"PASSED");mc.stop();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
        if(captures.isEmpty()||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var pending=List.copyOf(captures);captures.clear();
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            var dir=mc.gameDirectory.toPath().resolve("screenshots/tree-1.0.4").resolve(profile);Files.createDirectories(dir);
            for(var name:pending){pixels.writeToFile(dir.resolve(name));
                var state=new com.google.gson.JsonObject();state.addProperty("tick",ticks);state.addProperty("skillPoints",ClientData.get().skillPoints);state.addProperty("guiScale",mc.options.guiScale().get());
                state.addProperty("parcool",ClientSkillTree.parcool);state.addProperty("epicfight",ClientSkillTree.epic);state.addProperty("irons",ClientSkillTree.irons);
                state.addProperty("nodes",ClientSkillTree.nodes.size());states.add(name,state);LogUtils.getLogger().info("HEART_TREE_VISUAL capture {}/{}",profile,name);
                state.addProperty("language",mc.getLanguageManager().getSelected());
                if(mc.screen instanceof SkillTreeScreen&&info(mc)!=null){var t=info(mc);state.addProperty("infoX",t.x());state.addProperty("infoY",t.y());state.addProperty("infoHeight",t.height());state.addProperty("scroll",t.scroll());state.addProperty("maximumScroll",t.maximumScroll());}
            }
            var mods=new com.google.gson.JsonArray();for(var mod:ModList.get().getMods()){var entry=new com.google.gson.JsonObject();entry.addProperty("id",mod.getModId());entry.addProperty("version",mod.getVersion().toString());mods.add(entry);}
            var report=new com.google.gson.JsonObject();report.add("loadedMods",mods);report.add("screenshots",states);
            Files.writeString(dir.resolve("capture-states.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception error){check(false,"framebuffer capture: "+error);}
    }
    private SkillTreeVisualCapture(){}
}
