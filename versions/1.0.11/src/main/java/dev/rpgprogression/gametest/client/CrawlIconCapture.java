package dev.rpgprogression.gametest.client;

import com.mojang.logging.LogUtils;
import dev.rpgprogression.client.*;
import dev.rpgprogression.client.gui.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.SkillTreeManager;
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

/** Opt-in native screenshots in a copied development world; excluded from the release JAR. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class CrawlIconCapture {
    private static final String ID="parcool:crawl";
    private static boolean loading;
    private static volatile boolean failed;
    private static int ticks;
    private static String originalLanguage;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static final List<String> captures=new ArrayList<>();
    private static final com.google.gson.JsonObject states=new com.google.gson.JsonObject();
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void check(boolean ok,String message){if(!ok)failed=true;LogUtils.getLogger().info("HEART_CRAWL_ICON {} {}",ok?"verified":"FAILED",message);}
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable error){failed=true;LogUtils.getLogger().error("HEART_CRAWL_ICON server failure",error);}});}
    private static double scale(Screen s){return Math.min(1.5,Math.min(s.width/640.0,s.height/360.0));}
    private static double sx(Screen s,double x){return (s.width-640*scale(s))/2+x*scale(s);}
    private static double sy(Screen s,double y){return (s.height-360*scale(s))/2+y*scale(s);}
    private static void cursor(Minecraft mc,boolean hover){
        var s=(SkillTreeScreen)mc.screen;var p=s.skillPosition(ID);
        double x=hover?sx(s,p[0]):s.width-5,y=hover?sy(s,p[1]):s.height-5;
        try{for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,(name.equals("xpos")?x:y)*mc.getWindow().getGuiScale());}}
        catch(Exception error){throw new IllegalStateException(error);}
    }
    private static void tree(Minecraft mc){mc.setScreen(new SkillTreeScreen(new CharacterScreen(),1));((SkillTreeScreen)mc.screen).focusSkill(ID);cursor(mc,false);}
    private static void language(Minecraft mc,String value){mc.getLanguageManager().setSelected(value);mc.options.languageCode=value;reload=mc.reloadResourcePacks();}
    private static void shot(String name){captures.add(name+".png");}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("rpgprogression.crawlIconCapture")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld"));return;}
        if(loading&&mc.player==null){
            String label=mc.screen instanceof BackupConfirmScreen?Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;
        }
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        if(reload!=null){if(!reload.isDone())return;reload.join();reload=null;}
        if(mc.getOverlay()!=null)return;
        ticks++;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        if(ticks==20){
            check(SkillTreeManager.parcool(),"ParCool is loaded");mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.pauseOnLostFocus=false;
            originalLanguage=mc.getLanguageManager().getSelected();language(mc,"ru_ru");
            server(mc,()->{var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.stopRiding();p.removeAllEffects();p.getInventory().clearContent();
                var d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();d.level=20;d.skillMilestones=4;d.skillPoints=4;
                ProgressionManager.refresh(p,10,false);SkillTreeManager.refresh(p,d);ModNetworking.sync(p);});
        }
        if(ticks==40)ModNetworking.CHANNEL.sendToServer(new ModNetworking.RequestTree());
        if(ticks==60){
            check(ClientSkillTree.ready&&ClientSkillTree.nodes.stream().anyMatch(n->n.id().equals(ID)),"native crawl node received from server");
            check(mc.getResourceManager().getResource(GuiTextures.texture("icons/crawl")).isPresent(),"supplied icon resource is loaded");tree(mc);
        }
        if(ticks==80)shot("01-crawl-available");
        if(ticks==85)cursor(mc,true);
        if(ticks==100)shot("02-crawl-description");
        if(ticks==105){var s=(SkillTreeScreen)mc.screen;var p=s.skillPosition(ID);s.mouseClicked(sx(s,p[0]),sy(s,p[1]),0);s.mouseReleased(sx(s,p[0]),sy(s,p[1]),0);}
        if(ticks==125){check(ClientData.get().unlockedSkills.contains(ID)&&ClientData.get().skillPoints==3,"one icon click learns crawl for one point");cursor(mc,false);shot("03-crawl-learned");}
        if(ticks==135)server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);d.unlockedSkills.remove(ID);d.skillPoints=0;SkillTreeManager.refresh(p,d);ModNetworking.sync(p);});
        if(ticks==150){check(!ClientData.get().unlockedSkills.contains(ID)&&ClientData.get().skillPoints==0,"locked state received from server");tree(mc);cursor(mc,true);}
        if(ticks==165)shot("04-crawl-locked");
        if(ticks==170){mc.options.guiScale().set(3);mc.resizeDisplay();tree(mc);}
        if(ticks==185)shot("05-crawl-scale3");
        if(ticks==190){mc.setScreen(null);mc.options.guiScale().set(2);mc.resizeDisplay();language(mc,originalLanguage);}
        if(ticks==210){LogUtils.getLogger().info("HEART_CRAWL_ICON_RESULT {}",failed?"FAILED":"PASSED");mc.stop();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
        if(captures.isEmpty()||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();var pending=List.copyOf(captures);captures.clear();
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            var dir=mc.gameDirectory.toPath().resolve("screenshots/crawl-1.0.10");Files.createDirectories(dir);
            for(var name:pending){pixels.writeToFile(dir.resolve(name));var state=new com.google.gson.JsonObject();
                state.addProperty("tick",ticks);state.addProperty("skillPoints",ClientData.get().skillPoints);state.addProperty("learned",ClientData.get().unlockedSkills.contains(ID));
                state.addProperty("guiScale",mc.options.guiScale().get());state.addProperty("language",mc.getLanguageManager().getSelected());states.add(name,state);
                LogUtils.getLogger().info("HEART_CRAWL_ICON capture {}",name);
            }
            var mods=new com.google.gson.JsonArray();for(var mod:ModList.get().getMods()){var entry=new com.google.gson.JsonObject();entry.addProperty("id",mod.getModId());entry.addProperty("version",mod.getVersion().toString());mods.add(entry);}
            var report=new com.google.gson.JsonObject();report.add("loadedMods",mods);report.add("screenshots",states);
            Files.writeString(dir.resolve("capture-states.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception error){check(false,"framebuffer capture: "+error);}
    }
    private CrawlIconCapture(){}
}
