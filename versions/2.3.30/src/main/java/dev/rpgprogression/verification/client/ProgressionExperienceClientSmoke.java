package dev.rpgprogression.verification.client;

import com.google.gson.JsonArray;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.hud.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.verification.*;
import dev.rpgprogression.xp.ProgressionOrb;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class ProgressionExperienceClientSmoke {
    private static final JsonArray checks=new JsonArray();
    private static int stage,age,boot;private static boolean loading,done,monotonic=true,pauseCaptured;private static CompletableFuture<?> pending;
    private static long lastShown,pausedCount;
    private static boolean mods(){return Boolean.getBoolean("rpgprogression.portRenderMods");}
    private static Path folder(Minecraft mc){return mc.gameDirectory.toPath().resolve("screenshots/experience-2.3.30-"+(mods()?"all":"base"));}
    private static void check(boolean ok,String text){ProgressionExperienceChecks.check(checks,ok,text);}
    private static void next(){stage++;age=0;}
    private static void capture(Minecraft mc,String name)throws Exception{Files.createDirectories(folder(mc));try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder(mc).resolve(name));}}
    private static void server(Minecraft mc,java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action){pending=CompletableFuture.runAsync(()->action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())),mc.getSingleplayerServer());}
    private static void amount(Minecraft mc,long xp){server(mc,p->{var d=ProgressionManager.get(p);d.xp=xp;d.levelingPending=false;d.dirty=true;ModNetworking.sync(p);});}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(done||!Boolean.getBoolean("rpgprogression.experienceClientSmoke"))return;var mc=Minecraft.getInstance();
        try{
            if(mc.player==null||mc.getSingleplayerServer()==null){if(++boot>6000)throw new IllegalStateException("startup timeout");if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.options.pauseOnLostFocus=false;var previous=mc.screen;mc.createWorldOpenFlows().openWorld(System.getProperty("rpgprogression.smokeWorld"),()->mc.setScreen(previous));}return;}
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}if(mc.getOverlay()!=null)return;if(++age>600)throw new IllegalStateException("stage timeout "+stage);
            switch(stage){
                case 0->{if(age<30)return;mc.options.guiScale().set(2);mc.options.hideGui=false;mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);mc.resizeDisplay();RPGConfig.SHOW_EXPERIENCE_HUD.set(true);
                    server(mc,p->{ProgressionOrbChecks.scene(p);p.serverLevel().getEntitiesOfClass(ProgressionOrb.class,new AABB(130,210,130,160,230,160)).forEach(Entity::discard);p.setGameMode(GameType.CREATIVE);RPGConfig.CREATIVE_XP.set(false);p.connection.teleport(144.5,221,150.5,180,13);var d=ProgressionManager.get(p);d.level=1000;d.xp=0;d.levelingPending=false;ModNetworking.sync(p);ProgressionOrb.award(p.serverLevel(),new Vec3(144.5,221.2,144.5),2477);ProgressionOrb.award(p.serverLevel(),new Vec3(148.5,221.2,144.5),2477);});next();}
                case 1->{if(age==1)ProgressionExperienceHud.reset();if(age<30||ClientData.get().xp!=0)return;check(!ProgressionExperienceHud.visible()&&ProgressionExperienceHud.displayedAmount(.5f)==0,"the initial synchronized amount is an immediate silent baseline");capture(mc,"01-before-pickup.png");server(mc,p->p.connection.teleport(144.5,221,144.5,180,13));next();}
                case 2->{if(ClientData.get().xp!=2477)return;long shown=ProgressionExperienceHud.displayedAmount(.5f);if(shown<=0||shown>=2477)return;check(ClientData.get().xp==2477,"a real star pickup immediately credits and synchronizes its full value in creative mode");check(ProgressionExperienceHud.visible()&&shown>0&&shown<2477,"the visible count animates through intermediate values after the real pickup");check(ProgressionExperienceHud.displayedAmount(0)<ProgressionExperienceHud.displayedAmount(1),"frame interpolation advances the count between client ticks");capture(mc,"02-first-pickup-counting.png");lastShown=ProgressionExperienceHud.displayedAmount(1);server(mc,p->p.connection.teleport(148.5,221,144.5,180,13));next();}
                case 3->{long shown=ProgressionExperienceHud.displayedAmount(1);monotonic&=shown>=lastShown;lastShown=shown;if(ClientData.get().xp!=4954)return;check(ClientData.get().xp==4954,"the next real star pickup adds its exact value while the first count is changing");check(monotonic&&shown>0&&shown<4954,"another pickup continues toward the new total without resetting or jumping to the total");capture(mc,"03-second-pickup-counting.png");next();}
                case 4->{if(age<20)return;check(ProgressionExperienceHud.displayedAmount(1)==4954&&ClientData.get().xp==4954,"the animation settles on the exact synchronized total after both pickups");capture(mc,"04-final-total.png");amount(mc,0);next();}
                case 5->{if(ClientData.get().xp!=0)return;long shown=ProgressionExperienceHud.displayedAmount(.5f);if(shown<=0||shown>=4954)return;check(shown>0&&shown<4954,"a decrease or level rollover also smoothly approaches the new remaining amount");capture(mc,"05-decreasing.png");next();}
                case 6->{if(age<16)return;check(ProgressionExperienceHud.displayedAmount(1)==0,"the displayed count finishes at zero without negative values");RPGConfig.SHOW_EXPERIENCE_HUD.set(false);amount(mc,10000);next();}
                case 7->{if(age<18||ClientData.get().xp!=10000)return;check(!ProgressionExperienceHud.visible(),"disabled indicators do not display or queue the animated change");RPGConfig.SHOW_EXPERIENCE_HUD.set(true);next();}
                case 8->{if(age<5)return;check(!ProgressionExperienceHud.visible()&&ProgressionExperienceHud.displayedAmount(0)==10000,"re-enabling snaps the silent baseline to the current amount instead of replaying old changes");amount(mc,123456789);next();}
                case 9->{if(age<18||ClientData.get().xp!=123456789)return;String expected=ExperienceAmountFormat.format(123456789,mc.options.languageCode.startsWith("ru"),50,mc.font::width);check(ProgressionExperienceHud.displayedAmount(1)==123456789&&ProgressionExperienceHud.amount().equals(expected)&&mc.font.width(expected)<=50,"the settled animated value retains the existing width-aware localized abbreviation");capture(mc,"06-large-final-total.png");amount(mc,200000000);next();}
                case 10->{if(ClientData.get().xp!=200000000)return;long shown=ProgressionExperienceHud.displayedAmount(1);if(shown<=123456789||shown>=200000000)return;check(shown>123456789&&shown<200000000,"a new synchronized value starts a fresh count from the existing displayed amount");mc.pauseGame(false);pausedCount=shown;next();}
                case 11->{if(!pauseCaptured&&mc.isPaused()){pausedCount=ProgressionExperienceHud.displayedAmount(1);pauseCaptured=true;}if(age<10)return;check(pauseCaptured&&mc.isPaused()&&ProgressionExperienceHud.displayedAmount(1)==pausedCount,"pausing the game freezes the in-progress count");mc.setScreen(null);next();}
                case 12->{if(age<20)return;check(ProgressionExperienceHud.displayedAmount(1)==200000000,"unpausing completes the count at its exact target");capture(mc,"07-resumed-total.png");ProgressionExperienceHud.reset();check(!ProgressionExperienceHud.visible()&&ProgressionExperienceHud.displayedAmount(1)==0,"disconnect cleanup clears both the count animation and the visibility state");done=true;ProgressionExperienceChecks.report(folder(mc).resolve("result.json"),checks,null);mc.stop();}
            }
        }catch(Throwable error){done=true;com.mojang.logging.LogUtils.getLogger().error("EXPERIENCE_SMOOTH stage {}",stage,error);try{ProgressionExperienceChecks.report(folder(mc).resolve("result.json"),checks,error);}catch(Exception e){throw new IllegalStateException(e);}mc.stop();}
    }
    private ProgressionExperienceClientSmoke(){}
}
