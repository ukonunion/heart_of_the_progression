package dev.rpgprogression.gametest.client;
import com.mojang.logging.LogUtils;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.compat.IronsCompat;
import dev.rpgprogression.progression.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class ManaClientSmoke {
    private static boolean loading;private static volatile boolean failed;private static int ticks;private static String capture;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("rpgprogression.manaSmoke")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld","RPGManaV171Combined"));return;}
        if(loading&&mc.player==null){String label=mc.screen instanceof BackupConfirmScreen?net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;}
        if(mc.player==null||mc.getSingleplayerServer()==null)return;ticks++;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        if(Boolean.getBoolean("rpgprogression.manaRelogin")){
            if(ticks==55){check(ClientData.get().level==1201,"uncapped character level survives client restart");check(ClientData.get().manaCapacity==8&&ClientData.get().mana>=6&&ClientData.get().mana<6.6,"new mana units survive client restart");}
            if(ticks==70)finish(mc);return;
        }
        if(ticks==30){mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setScreen(null);
            server(mc,()->{var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.removeAllEffects();p.getAbilities().invulnerable=true;
                var d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();d.level=1201;ProgressionManager.refresh(p,10,false);p.setHealth(p.getMaxHealth());XPReportCapture.write(p);});}
        if(IronsCompat.enabled()){OptionalManaClientSmoke.tick(ticks,mc,ManaClientSmoke::check,n->capture=n);if(ticks>=690&&ticks<=755)AllIndicatorsClientFixture.tick(525+ticks-690,mc,ManaClientSmoke::check);}
        else{if(ticks==65){check(!ClientData.get().manaActive,"no Iron's means no mana HUD");check(ClientData.get().level==1201,"standalone level has no old cap");capture="standalone-no-mana.png";}if(ticks==90)finish(mc);}
        if(ticks==805)finish(mc);
    }
    public static net.minecraft.server.level.ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    public static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable ex){failed=true;LogUtils.getLogger().error("RPG_MANA_CLIENT exception",ex);}});}
    private static void check(boolean ok,String message){if(!ok){failed=true;LogUtils.getLogger().error("RPG_MANA_CLIENT FAILED {}",message);}else LogUtils.getLogger().info("RPG_MANA_CLIENT verified {}",message);}
    private static void finish(Minecraft mc){LogUtils.getLogger().info("RPG_MANA_CLIENT_RESULT {}",failed?"FAILED":"PASSED");mc.stop();}
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){if(capture==null||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();String name=capture;capture=null;
        var file=mc.gameDirectory.toPath().resolve("screenshots/mana-v171-"+(IronsCompat.enabled()?"combined":"standalone")).resolve(name);
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(file.getParent());pixels.writeToFile(file);LogUtils.getLogger().info("RPG_MANA_CLIENT screenshot {}",file);}catch(Exception ex){check(false,"screenshot: "+ex);}}
}
