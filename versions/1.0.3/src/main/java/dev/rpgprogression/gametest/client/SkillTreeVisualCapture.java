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
    private static void tree(Minecraft mc,int branch){mc.setScreen(new SkillTreeScreen(new CharacterScreen(),branch));cursorAway(mc);}
    private static String details(Minecraft mc,int branch){
        var node=ClientSkillTree.nodes.stream().filter(n->n.branch()==branch&&SkillTreeManager.canUnlock(ClientData.get(),n)).findFirst().orElseThrow();
        var s=(SkillTreeScreen)mc.screen;s.focusSkill(node.id());var p=s.skillPosition(node.id());click(mc,p[0],p[1]);return node.id();
    }
    private static void learn(Minecraft mc){
        var button=mc.screen.children().stream().filter(c->c instanceof SkillTreeButton b&&b.active).map(c->(SkillTreeButton)c).findFirst().orElseThrow();
        click(mc,button.getX()+20,button.getY()+20);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("rpgprogression.treeVisualCapture")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld"));return;}
        if(loading&&mc.player==null){
            String label=mc.screen instanceof BackupConfirmScreen?Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;
        }
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        ticks++;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        boolean parcool=SkillTreeManager.parcool(),epic=SkillTreeManager.epic(),irons=dev.rpgprogression.compat.IronsCompat.present();
        if(ticks==20){
            profile=parcool&&epic&&irons?"all":parcool?"parcool":epic?"epicfight":irons?"irons":"standalone";
            check(System.getProperty("rpgprogression.smokeWorld").endsWith("-"+profile),"actual optional-mod profile "+profile);
            mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.bobView().set(false);
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
        if(ticks==155&&parcool){check(ClientData.get().unlockedSkills.contains(parkourId),"ParCool skill learned through custom button");shot("04-parcool-learned");}
        if(ticks==160&&parcool)mc.screen.keyPressed(256,0,0);
        if(ticks==170&&epic)tree(mc,2);
        if(ticks==190&&epic)mc.screen.keyPressed(268,0,0);
        if(ticks==195&&epic)shot("05-epicfight");
        if(ticks==200&&epic){epicId=details(mc,2);server(mc,()->EpicTreeClientCheck.blockEquipForFixture(player(mc),epicId));}
        if(ticks==215&&epic)shot("06-epicfight-description");
        if(ticks==220&&epic)learn(mc);
        if(ticks==240&&epic){check(ClientData.get().unlockedSkills.contains(epicId),"Epic skill learned through custom button");shot("07-epicfight-actions");}
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
        if(ticks==375){LogUtils.getLogger().info("HEART_TREE_VISUAL_RESULT {} {}",profile,failed?"FAILED":"PASSED");mc.stop();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
        if(captures.isEmpty()||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var pending=List.copyOf(captures);captures.clear();
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            var dir=mc.gameDirectory.toPath().resolve("screenshots/tree-1.0.2").resolve(profile);Files.createDirectories(dir);
            for(var name:pending){pixels.writeToFile(dir.resolve(name));
                var state=new com.google.gson.JsonObject();state.addProperty("tick",ticks);state.addProperty("skillPoints",ClientData.get().skillPoints);state.addProperty("guiScale",mc.options.guiScale().get());
                state.addProperty("parcool",ClientSkillTree.parcool);state.addProperty("epicfight",ClientSkillTree.epic);state.addProperty("irons",ClientSkillTree.irons);
                state.addProperty("nodes",ClientSkillTree.nodes.size());states.add(name,state);LogUtils.getLogger().info("HEART_TREE_VISUAL capture {}/{}",profile,name);
            }
            var mods=new com.google.gson.JsonArray();for(var mod:ModList.get().getMods()){var entry=new com.google.gson.JsonObject();entry.addProperty("id",mod.getModId());entry.addProperty("version",mod.getVersion().toString());mods.add(entry);}
            var report=new com.google.gson.JsonObject();report.add("loadedMods",mods);report.add("screenshots",states);
            Files.writeString(dir.resolve("capture-states.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception error){check(false,"framebuffer capture: "+error);}
    }
    private SkillTreeVisualCapture(){}
}
