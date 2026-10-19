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
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class ProgressionExperienceClientSmoke {
    private static final JsonArray checks=new JsonArray();private static int stage,age,boot;private static boolean loading,done,fadeCaptured;private static CompletableFuture<?> pending;private static long beforeXP;private static int beforeLevel;
    private static boolean mods(){return Boolean.getBoolean("rpgprogression.portRenderMods");}
    private static Path folder(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/experience-2.3.29-"+(mods()?"all":"base"));}
    private static void check(boolean ok,String text){ProgressionExperienceChecks.check(checks,ok,text);}
    private static void next(){stage++;age=0;}
    private static void capture(Minecraft mc,String name)throws Exception{Files.createDirectories(folder(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder(mc).resolve(name));}}
    private static void server(Minecraft mc,java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action){pending=CompletableFuture.runAsync(()->action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())),mc.getSingleplayerServer());}
    private static void amount(Minecraft mc,long xp){server(mc,p->{var d=ProgressionManager.get(p);d.initialize();d.level=1000;d.xp=xp;d.levelingPending=false;d.dirty=true;ModNetworking.sync(p);});}
    private static void language(Minecraft mc,String code){mc.getLanguageManager().setSelected(code);mc.options.languageCode=code;pending=mc.reloadResourcePacks();}
    private static List<AbstractWidget> widgets(GuiEventListener listener){
        var result=new ArrayList<AbstractWidget>();if(listener instanceof AbstractWidget w)result.add(w);if(listener instanceof ContainerEventHandler c)for(var child:c.children())result.addAll(widgets(child));return result;
    }
    private static CycleButton<?> setting(Minecraft mc){
        var controls=widgets(mc.screen);var label=controls.stream().filter(w->w instanceof StringWidget&&w.getMessage().getString().equals("Индикатор RPG-опыта")).findFirst().orElseThrow();
        return controls.stream().filter(w->w instanceof CycleButton<?> && w.getY()==label.getY()&&w.getX()>label.getX()).map(w->(CycleButton<?>)w).findFirst().orElseThrow();
    }
    private static void openSetting(Minecraft mc){
        var container=net.neoforged.fml.ModList.get().getModContainerById("rpgprogression").orElseThrow();var factory=container.getCustomExtension(IConfigScreenFactory.class).orElseThrow();
        mc.setScreen(factory.createScreen(container,new TitleScreen()));
        widgets(mc.screen).stream().filter(w->w instanceof Button&&w.getMessage().getString().contains("Клиентские настройки")).map(w->(Button)w).findFirst().orElseThrow().onPress();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(done||!Boolean.getBoolean("rpgprogression.experienceClientSmoke"))return;var mc=Minecraft.getInstance();
        try{
            if(mc.player==null||mc.getSingleplayerServer()==null){if(++boot>6000)throw new IllegalStateException("startup timeout");if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var previous=mc.screen;mc.createWorldOpenFlows().openWorld(System.getProperty("rpgprogression.smokeWorld"),()->mc.setScreen(previous));}return;}
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}if(mc.getOverlay()!=null)return;if(++age>600)throw new IllegalStateException("stage timeout "+stage);
            switch(stage){
                case 0->{if(age<30)return;mc.options.guiScale().set(2);mc.options.hideGui=false;mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);mc.resizeDisplay();RPGConfig.SHOW_EXPERIENCE_HUD.set(true);server(mc,p->{try{ProgressionOrbChecks.scene(p);var d=ProgressionManager.get(p);d.level=1000;d.xp=0;d.levelingPending=false;ModNetworking.sync(p);}catch(Exception t){throw new IllegalStateException(t);}});next();}
                case 1->{if(age<15||mc.player.getX()<140)return;var stars=java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(),false).filter(x->x instanceof ProgressionOrb).toList();if(stars.isEmpty())return;ProgressionExperienceHud.reset();check(!ProgressionExperienceHud.visible(),"initial synchronized amount is a silent baseline");capture(mc,"01-idle-hidden.png");beforeXP=ClientData.get().xp;beforeLevel=ClientData.get().level;server(mc,p->{RPGConfig.CREATIVE_XP.set(false);p.setGameMode(GameType.CREATIVE);p.connection.teleport(144.5,221,144.5,180,13);});next();}
                case 2->{if(age<45)return;check(ClientData.get().xp>beforeXP||ClientData.get().level>beforeLevel,"natural creative pickup credits RPG experience with creativeGainsXP=false and synchronizes it");check(ProgressionExperienceHud.visible(),"creative star pickup opens the transient amount indicator");capture(mc,"02-creative-pickup.png");server(mc,p->p.setGameMode(GameType.SURVIVAL));next();}
                case 3->{language(mc,"ru_ru");next();}
                case 4->{amount(mc,10000);next();}
                case 5->{if(age<12||ClientData.get().xp!=10000)return;check(ProgressionExperienceHud.visible()&&ProgressionExperienceHud.amount().equals("10000")&&ProgressionExperienceHud.opacity(1)>.99,"Russian displays the full 10000 after the normal smooth fade-in");capture(mc,"03-full-10000-ru.png");boolean old=RPGConfig.SHOW_HUD.get();RPGConfig.SHOW_HUD.set(false);check(ProgressionExperienceHud.visible(),"the experience toggle is independent of the stamina and mana HUD setting");RPGConfig.SHOW_HUD.set(old);next();}
                case 6->{float alpha=ProgressionExperienceHud.opacity(1);if(alpha>0&&alpha<1&&!fadeCaptured){fadeCaptured=true;capture(mc,"04-fading-out.png");}if(ProgressionExperienceHud.visible())return;check(fadeCaptured&&age>=80,"unchanged experience fades out after the five-second window");capture(mc,"05-unchanged-hidden.png");language(mc,"en_us");next();}
                case 7->{check(!ProgressionExperienceHud.visible(),"changing the language alone never opens the indicator");amount(mc,1_000_000);next();}
                case 8->{if(age<12||ClientData.get().xp!=1_000_000)return;check(ProgressionExperienceHud.amount().equals("1000000")&&ProgressionExperienceHud.visible(),"English also shows the complete million without a letter suffix");capture(mc,"06-full-million-en.png");amount(mc,9999);next();}
                case 9->{if(age<12||ClientData.get().xp!=9999)return;check(ProgressionExperienceHud.visible()&&ProgressionExperienceHud.amount().equals("9999"),"a decrease refreshes the indicator just like a gain");capture(mc,"07-loss-refresh.png");mc.options.hideGui=true;check(!ProgressionExperienceHud.visible(),"F1 hides the transient panel");mc.options.hideGui=false;language(mc,"ru_ru");next();}
                case 10->{openSetting(mc);check(mc.screen instanceof net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen&&widgets(mc.screen).stream().noneMatch(w->w.getMessage().getString().equals("Версия расположения HUD")),"the registered mod settings expose the experience toggle and hide internal migration values");next();}
                case 11->{if(age<8)return;capture(mc,"08-settings-on.png");setting(mc).onPress();next();}
                case 12->{if(age<8)return;check(!RPGConfig.SHOW_EXPERIENCE_HUD.get(),"the actual settings button turns off the independent indicator");capture(mc,"09-settings-off.png");mc.screen.onClose();mc.setScreen(null);next();}
                case 13->{if(age<12)return;var config=new com.electronwill.nightconfig.toml.TomlParser().parse(Files.readString(mc.gameDirectory.toPath().resolve("config/rpgprogression-client.toml")));check(Boolean.FALSE.equals(config.get("showProgressionExperienceHUD")),"the disabled setting is saved in the real client TOML");amount(mc,7);next();}
                case 14->{if(age<15||ClientData.get().xp!=7)return;check(!ProgressionExperienceHud.visible(),"XP changes while disabled do not display or queue a panel");capture(mc,"10-disabled.png");openSetting(mc);next();}
                case 15->{if(age<8)return;setting(mc).onPress();mc.screen.onClose();mc.setScreen(null);next();}
                case 16->{if(age<12)return;check(RPGConfig.SHOW_EXPERIENCE_HUD.get()&&!ProgressionExperienceHud.visible(),"re-enabling does not replay changes from while the indicator was disabled");amount(mc,123_456_789L);next();}
                case 17->{if(age<12||ClientData.get().xp!=123_456_789L)return;check(ProgressionExperienceHud.visible()&&ProgressionExperienceHud.amount().equals("123,4 М")&&mc.font.width(ProgressionExperienceHud.amount())<=50,"only an overflowing amount is abbreviated and fits at normal text size");capture(mc,"11-overflow-millions.png");amount(mc,1_234_567_890_123_456L);next();}
                case 18->{if(age<12||ClientData.get().xp!=1_234_567_890_123_456L)return;check(ProgressionExperienceHud.amount().equals("1,2 Квдр")&&mc.font.width(ProgressionExperienceHud.amount())<=50,"very large amounts use a readable localized suffix instead of tiny full numbers");capture(mc,"12-overflow-quadrillions.png");mc.options.guiScale().set(3);mc.resizeDisplay();next();}
                case 19->{if(age<12)return;check(ProgressionExperienceHud.x(mc.getWindow().getGuiScaledWidth())+70==mc.getWindow().getGuiScaledWidth(),"the animated panel stays at the right edge at another GUI scale");capture(mc,"12-gui-scale-3.png");server(mc,p->p.setGameMode(GameType.SPECTATOR));next();}
                case 20->{if(age<12)return;check(!ProgressionExperienceHud.visible(),"spectators do not see the experience indicator");ProgressionExperienceHud.reset();check(!ProgressionExperienceHud.visible(),"disconnect cleanup clears the experience animation and baseline");mc.gui.getChat().clearMessages(true);dev.rpgprogression.client.hud.LevelUpHud.reset();server(mc,p->{p.setGameMode(GameType.SURVIVAL);var d=ProgressionManager.get(p);d.level=1;d.points=0;d.xp=d.requiredXP()-1;d.levelingPending=false;ProgressionManager.addXPAdministrative(p,1);ModNetworking.sync(p);});next();}
                case 21->{if(age<12||ClientData.get().level!=2)return;check(dev.rpgprogression.client.hud.LevelUpHud.active()&&ClientData.get().points>0,"a real server level-up packet still shows its original banner and grants attribute points");var field=mc.gui.getChat().getClass().getDeclaredField("allMessages");field.setAccessible(true);check(((List<?>)field.get(mc.gui.getChat())).isEmpty(),"the real level-up packet creates no attribute-points chat message");capture(mc,"13-level-up-without-chat.png");done=true;ProgressionExperienceChecks.report(folder(mc).resolve("result.json"),checks,null);mc.stop();}
            }
        }catch(Throwable t){done=true;com.mojang.logging.LogUtils.getLogger().error("EXPERIENCE_CLIENT stage {}",stage,t);try{ProgressionExperienceChecks.report(folder(mc).resolve("result.json"),checks,t);}catch(Exception ex){throw new IllegalStateException(ex);}mc.stop();}
    }
    private ProgressionExperienceClientSmoke(){}
}
