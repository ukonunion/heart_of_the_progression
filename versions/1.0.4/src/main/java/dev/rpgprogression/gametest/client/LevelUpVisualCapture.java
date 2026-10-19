package dev.rpgprogression.gametest.client;

import com.mojang.logging.LogUtils;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.hud.LevelUpHud;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Real XP awards and native framebuffer captures in isolated development-world copies. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class LevelUpVisualCapture {
    private static boolean loading;
    private static volatile boolean failed;
    private static int ticks;
    private static String profile,originalLanguage;
    private static final List<String> captures=new ArrayList<>();
    private static final com.google.gson.JsonObject states=new com.google.gson.JsonObject();
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable error){failed=true;LogUtils.getLogger().error("HEART_LEVEL_UP server failure",error);}});}
    private static void check(boolean ok,String message){if(!ok)failed=true;LogUtils.getLogger().info("HEART_LEVEL_UP {} {}",ok?"verified":"FAILED",message);}
    private static void shot(String name){captures.add("screenshots/"+name+".png");}
    private static void frame(String language){captures.add("frames/"+language+"/"+String.format(Locale.ROOT,"%05d",ticks)+".png");}
    private static void language(Minecraft mc,String language){mc.getLanguageManager().setSelected(language);}
    private static void gain(Minecraft mc){server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);ProgressionManager.addXPAdministrative(p,d.requiredXP()-d.xp);ModNetworking.sync(p);});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("rpgprogression.levelUpCapture")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld"));return;}
        if(loading&&mc.player==null){
            String label=mc.screen instanceof BackupConfirmScreen?Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;
        }
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        ticks++;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        if(ticks==20){
            profile=ModList.get().isLoaded("epicfight")?"all":"standalone";originalLanguage=mc.getLanguageManager().getSelected();
            check(System.getProperty("rpgprogression.smokeWorld").endsWith("-"+profile),"actual mod profile "+profile);
            mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.bobView().set(false);mc.options.hideGui=false;mc.setScreen(null);
            server(mc,()->{var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.onUpdateAbilities();p.removeAllEffects();p.stopRiding();p.getInventory().clearContent();
                var d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();ProgressionManager.refresh(p,10,false);
                p.setHealth(p.getMaxHealth());p.serverLevel().setDayTime(6000);p.serverLevel().setWeatherParameters(100000,0,false,false);
                p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);ModNetworking.sync(p);});
        }
        if(ticks==60){check(!LevelUpHud.active()&&ClientData.get().level==1,"login and initial progress sync are silent");shot("01-login-no-banner");}
        if(ticks==65)server(mc,()->{var p=player(mc);ProgressionManager.addXPAdministrative(p,125);p.giveExperiencePoints(20);ModNetworking.sync(p);});
        if(ticks==100){check(!LevelUpHud.active()&&ClientData.get().level==1&&ClientData.get().xp==125,"partial RPG XP and vanilla XP are silent");shot("02-xp-no-banner");language(mc,"ru_ru");}
        if(ticks>=130&&ticks<=230&&ticks%2==0)frame("ru");
        if(ticks==135)gain(mc);
        if(ticks==165){check(ClientData.get().level==2&&LevelUpHud.opacity(1)==1,"real server RPG level gain shows Russian banner");shot("03-level-up-ru");}
        if(ticks==235){check(!LevelUpHud.active(),"Russian banner fades out completely");shot("04-after-fade");}
        if(ticks==240)server(mc,()->{var p=player(mc);var d=ProgressionManager.get(p);d.level=30;d.xp=0;ModNetworking.sync(p);});
        if(ticks==275){check(ClientData.get().level==30&&!LevelUpHud.active(),"even a higher-level snapshot alone is silent");language(mc,"en_us");}
        if(ticks>=300&&ticks<=400&&ticks%2==0)frame("en");
        if(ticks==305)gain(mc);
        if(ticks==335){check(ClientData.get().level==31&&LevelUpHud.opacity(1)==1,"real server RPG level gain shows English banner");shot("05-level-up-en");}
        if(ticks==410){check(!LevelUpHud.active(),"English banner fades out completely");mc.options.guiScale().set(3);mc.resizeDisplay();gain(mc);}
        if(ticks==430){check(LevelUpHud.opacity(1)==1,"banner fits GUI Scale 3");shot("06-level-up-scale3");gain(mc);}
        if(ticks==445){check(ClientData.get().level==33&&LevelUpHud.opacity(1)==1,"another real level gain extends the visible banner without blinking");}
        if(ticks==450){mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.hideGui=true;}
        if(ticks==455){check(LevelUpHud.active(),"F1 hides the rendered HUD without creating or resetting a level gain");shot("07-f1-hidden");}
        if(ticks==456)mc.options.hideGui=false;
        if(ticks==515){check(!LevelUpHud.active(),"extended banner eventually disappears");server(mc,()->{var p=player(mc);p.teleportTo(p.server.getLevel(Level.NETHER),0,90,0,Set.of(),0,0);});}
        if(ticks>=550&&mc.level.dimension()==Level.NETHER){
            check(!LevelUpHud.active(),"dimension change does not trigger the banner");ClientData.reset();check(!LevelUpHud.active(),"logout cleanup leaves no banner state");
            language(mc,originalLanguage);LogUtils.getLogger().info("HEART_LEVEL_UP_RESULT {} {}",profile,failed?"FAILED":"PASSED");mc.stop();
        }else if(ticks>=900){check(false,"dimension change timed out");language(mc,originalLanguage);LogUtils.getLogger().info("HEART_LEVEL_UP_RESULT {} FAILED",profile);mc.stop();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
        if(captures.isEmpty()||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var pending=List.copyOf(captures);captures.clear();
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            var root=mc.gameDirectory.toPath().resolve("screenshots/level-up-1.0.3").resolve(profile);Files.createDirectories(root);
            for(var name:pending){var target=root.resolve(name);Files.createDirectories(target.getParent());pixels.writeToFile(target);
                var state=new com.google.gson.JsonObject();state.addProperty("tick",ticks);state.addProperty("level",ClientData.get().level);state.addProperty("xp",ClientData.get().xp);
                state.addProperty("language",mc.getLanguageManager().getSelected());state.addProperty("opacity",LevelUpHud.opacity(mc.getFrameTime()));state.addProperty("guiScale",mc.options.guiScale().get());state.addProperty("hideGui",mc.options.hideGui);states.add(name,state);
            }
            var mods=new com.google.gson.JsonArray();for(var mod:ModList.get().getMods()){var entry=new com.google.gson.JsonObject();entry.addProperty("id",mod.getModId());entry.addProperty("version",mod.getVersion().toString());mods.add(entry);}
            var report=new com.google.gson.JsonObject();report.add("loadedMods",mods);report.add("captures",states);
            Files.writeString(root.resolve("capture-states.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Exception error){check(false,"framebuffer capture: "+error);}
    }
    private LevelUpVisualCapture(){}
}
