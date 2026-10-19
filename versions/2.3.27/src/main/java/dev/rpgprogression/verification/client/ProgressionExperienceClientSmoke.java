package dev.rpgprogression.verification.client;

import com.google.gson.JsonArray;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.hud.ProgressionExperienceHud;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.verification.*;
import dev.rpgprogression.xp.ProgressionOrb;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class ProgressionExperienceClientSmoke {
    private static final JsonArray checks=new JsonArray();private static int stage,age,boot;private static boolean loading,done;private static CompletableFuture<?> pending;private static long beforeXP;private static int beforeLevel;
    private static boolean mods(){return Boolean.getBoolean("rpgprogression.portRenderMods");}
    private static Path folder(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/experience-2.3.27-"+(mods()?"all":"base"));}
    private static void check(boolean ok,String text){ProgressionExperienceChecks.check(checks,ok,text);}
    private static void next(){stage++;age=0;}
    private static void capture(Minecraft mc,String name)throws Exception{Files.createDirectories(folder(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder(mc).resolve(name));}}
    private static void server(Minecraft mc,java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action){pending=CompletableFuture.runAsync(()->action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())),mc.getSingleplayerServer());}
    private static void amount(Minecraft mc,long xp){server(mc,p->{var d=ProgressionManager.get(p);d.initialize();d.level=1000;d.xp=xp;d.levelingPending=false;d.dirty=true;ModNetworking.sync(p);});}
    private static void language(Minecraft mc,String code){mc.getLanguageManager().setSelected(code);mc.options.languageCode=code;pending=mc.reloadResourcePacks();}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(done||!Boolean.getBoolean("rpgprogression.experienceClientSmoke"))return;var mc=Minecraft.getInstance();
        try{
            if(mc.player==null||mc.getSingleplayerServer()==null){if(++boot>6000)throw new IllegalStateException("startup timeout");if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var previous=mc.screen;mc.createWorldOpenFlows().openWorld(System.getProperty("rpgprogression.smokeWorld"),()->mc.setScreen(previous));}return;}
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}if(mc.getOverlay()!=null)return;if(++age>600)throw new IllegalStateException("stage timeout "+stage);
            switch(stage){
                case 0->{if(age<30)return;mc.options.guiScale().set(2);mc.options.hideGui=false;mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);mc.resizeDisplay();server(mc,p->{try{ProgressionExperienceChecks.run(p,checks);ProgressionOrbChecks.scene(p);var d=ProgressionManager.get(p);d.level=1000;d.xp=0;d.levelingPending=false;ModNetworking.sync(p);}catch(Exception t){throw new IllegalStateException(t);}});next();}
                case 1->{if(age<15||mc.player.getX()<140)return;var stars=java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(),false).filter(x->x instanceof ProgressionOrb).toList();if(stars.isEmpty())return;check(!stars.isEmpty(),"the native client renders the synchronized gold-star entities");beforeXP=ClientData.get().xp;beforeLevel=ClientData.get().level;server(mc,p->p.connection.teleport(144.5,221,144.5,180,13));next();}
                case 2->{if(age<45)return;check(ClientData.get().xp>beforeXP||ClientData.get().level>beforeLevel,"natural pickup credits the server and synchronizes the current RPG amount");amount(mc,7);next();}
                case 3->{if(ClientData.get().xp!=7)return;language(mc,"ru_ru");next();}
                case 4->{if(age<15)return;check(ProgressionExperienceHud.visible()&&ProgressionExperienceHud.amount().equals("7"),"the HUD shows only the synchronized amount 7");capture(mc,"01-amount-7-ru.png");amount(mc,9999);next();}
                case 5->{if(age<12||ClientData.get().xp!=9999)return;check(ProgressionExperienceHud.amount().equals("9999"),"amounts below ten thousand stay exact");capture(mc,"02-amount-9999-ru.png");amount(mc,10000);next();}
                case 6->{if(age<12||ClientData.get().xp!=10000)return;check(ProgressionExperienceHud.amount().equals("10 Т"),"Russian switches to 10 Т exactly at ten thousand");capture(mc,"03-amount-10T-ru.png");amount(mc,1_250_000);next();}
                case 7->{if(age<12||ClientData.get().xp!=1_250_000)return;check(ProgressionExperienceHud.amount().equals("1,2 М"),"Russian millions fit the same panel with one truncated decimal");capture(mc,"04-amount-million-ru.png");amount(mc,10000);next();}
                case 8->{if(ClientData.get().xp!=10000)return;language(mc,"en_us");next();}
                case 9->{if(age<12)return;check(ProgressionExperienceHud.amount().equals("10 K"),"English switches to 10 K at the same boundary");capture(mc,"05-amount-10K-en.png");amount(mc,1_000_000);next();}
                case 10->{if(age<12||ClientData.get().xp!=1_000_000)return;check(ProgressionExperienceHud.amount().equals("1 M"),"English uses 1 M at one million");capture(mc,"06-amount-million-en.png");mc.options.guiScale().set(3);mc.resizeDisplay();next();}
                case 11->{if(age<12)return;check(ProgressionExperienceHud.x(mc.getWindow().getGuiScaledWidth())+70==mc.getWindow().getGuiScaledWidth()&&ProgressionExperienceHud.y(mc.getWindow().getGuiScaledHeight())>=0,"the supplied frame stays attached to the right edge at a different GUI scale");capture(mc,"07-gui-scale-3.png");mc.options.hideGui=true;check(!ProgressionExperienceHud.visible(),"F1 hides the experience panel");next();}
                case 12->{if(age<8)return;capture(mc,"08-hidden-f1.png");mc.options.hideGui=false;boolean saved=RPGConfig.SHOW_HUD.get();RPGConfig.SHOW_HUD.set(false);check(!ProgressionExperienceHud.visible(),"the existing HUD visibility setting hides the panel");RPGConfig.SHOW_HUD.set(saved);server(mc,p->p.setGameMode(GameType.SPECTATOR));next();}
                case 13->{if(age<12)return;check(!ProgressionExperienceHud.visible(),"spectator mode hides the panel");server(mc,p->p.setGameMode(GameType.CREATIVE));next();}
                case 14->{if(age<12)return;check(ProgressionExperienceHud.visible(),"the amount panel is visible in creative mode");capture(mc,"09-creative.png");server(mc,p->p.setGameMode(GameType.SURVIVAL));next();}
                case 15->{if(age<12)return;done=true;ProgressionExperienceChecks.report(folder(mc).resolve("result.json"),checks,null);mc.stop();}
            }
        }catch(Throwable t){done=true;com.mojang.logging.LogUtils.getLogger().error("EXPERIENCE_CLIENT",t);try{ProgressionExperienceChecks.report(folder(mc).resolve("result.json"),checks,t);}catch(Exception ex){throw new IllegalStateException(ex);}mc.stop();}
    }
    private ProgressionExperienceClientSmoke(){}
}
