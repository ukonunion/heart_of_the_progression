package dev.rpgprogression.gametest.client;

import com.mojang.logging.LogUtils;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.gui.CharacterScreen;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;

/** Opt-in developer smoke test. Excluded from the distributed JAR with the entire gametest package. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class ClientSmokeTest {
    private static boolean loading=false,failed=false;
    private static int tick=0;
    private static String capture;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(!Boolean.getBoolean("rpgprogression.smokeTest")||e.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,"RPGSmoke");return;}
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        mc.getToasts().clear();
        tick++;
        if(Boolean.getBoolean("rpgprogression.reloginOnly")) {
            if(tick==20) {
                var saved=ClientData.get();
                check(saved.level==12&&saved.xp==542&&saved.points==1&&saved.get(RPGAttribute.STRENGTH)==15,"saved progression after full client/server restart");
                LogUtils.getLogger().info("RPG_RELOGIN_SMOKE {}",failed?"FAILED":"PASSED");mc.stop();
            }
            return;
        }
        if(tick==30)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            if(p==null)return;
            p.setGameMode(GameType.SURVIVAL);var d=ProgressionManager.get(p);
            d.level=12;d.xp=542;d.points=2;d.set(RPGAttribute.STRENGTH,14);d.set(RPGAttribute.VITALITY,14);d.set(RPGAttribute.AGILITY,12);d.set(RPGAttribute.ENDURANCE,10);d.set(RPGAttribute.INTELLIGENCE,11);
            p.getAbilities().invulnerable=true;p.setHealth(28);ProgressionManager.refresh(p,10,false);
        });
        if(tick==70){
            check(ClientData.get().level==12&&ClientData.get().get(RPGAttribute.STRENGTH)==14,"server-to-client progression sync");
            mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new CharacterScreen());
        }
        if(tick==90)capture="character-ru-scale2.png";
        if(tick==100)ModNetworking.CHANNEL.sendToServer(new ModNetworking.SpendPoint(RPGAttribute.STRENGTH.ordinal()));
        if(tick==120){check(ClientData.get().get(RPGAttribute.STRENGTH)==15&&ClientData.get().points==1,"C2S spend / S2C sync");mc.options.guiScale().set(3);mc.resizeDisplay();}
        if(tick==140)capture="character-ru-scale3.png";
        if(tick==150){mc.setScreen(new InventoryScreen(mc.player));}
        if(tick==170)capture="inventory-locked.png";
        if(tick==180){mc.setScreen(null);}
        if(tick==200)capture="stamina-hud.png";
        if(tick==220){LogUtils.getLogger().info("RPG_CLIENT_SMOKE {}",failed?"FAILED":"PASSED");mc.stop();}
    }
    private static void check(boolean result,String assertion){if(!result){failed=true;LogUtils.getLogger().error("RPG_CLIENT_SMOKE assertion failed: {}",assertion);}else LogUtils.getLogger().info("RPG_CLIENT_SMOKE verified: {}",assertion);}
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e) {
        if(capture==null||e.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();String filename=capture;capture=null;
        Path destination=mc.gameDirectory.toPath().resolve("screenshots").resolve(filename);
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            Files.createDirectories(destination.getParent());pixels.writeToFile(destination);
            LogUtils.getLogger().info("RPG_CLIENT_SMOKE screenshot: {}",destination);
        }catch(Exception error){failed=true;LogUtils.getLogger().error("RPG_CLIENT_SMOKE screenshot failed",error);}
    }
}
