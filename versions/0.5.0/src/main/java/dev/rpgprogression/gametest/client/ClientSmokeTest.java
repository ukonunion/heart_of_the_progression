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
    private static Object loadingScreen;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(!Boolean.getBoolean("rpgprogression.smokeTest")||e.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld","RPGSmoke"));return;}
        if(loading&&mc.player==null&&mc.screen!=loadingScreen){
            loadingScreen=mc.screen;LogUtils.getLogger().info("RPG_CLIENT_SMOKE loading screen: {}",mc.screen==null?"none":mc.screen.getClass().getName());
            // Iron's adds an experimental dimension. Back up this isolated developer world and continue.
            if(mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen){
                String label=net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString();
                for(var child:mc.screen.children())if(child instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}
            }
            if(mc.screen instanceof net.minecraft.client.gui.screens.ConfirmScreen){
                String label=net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString();
                for(var child:mc.screen.children())if(child instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}
            }
        }
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        mc.getToasts().clear();
        mc.gui.getChat().clearMessages(true);
        tick++;
        if(mc.screen instanceof CharacterScreen){
            // The hidden test window may not receive native cursor callbacks. Keep captures tooltip-free.
            try {
                double s=Math.min(1.5,Math.min(mc.screen.width/640.0,mc.screen.height/360.0));
                double x=((mc.screen.width-640*s)/2+140*s)*mc.getWindow().getGuiScale();
                double y=((mc.screen.height-360*s)/2+148*s)*mc.getWindow().getGuiScale();
                for(String name:new String[]{"xpos","ypos"}){
                    var field=MouseHandler.class.getDeclaredField(name);field.setAccessible(true);field.setDouble(mc.mouseHandler,name.equals("xpos")?x:y);
                }
            }catch(ReflectiveOperationException error){check(false,"position smoke-test cursor: "+error);}
        }
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
            p.getInventory().clearContent();
            d.level=12;d.xp=542;d.points=3;d.set(RPGAttribute.STRENGTH,14);d.set(RPGAttribute.VITALITY,14);d.set(RPGAttribute.AGILITY,12);d.set(RPGAttribute.ENDURANCE,10);d.set(RPGAttribute.INTELLIGENCE,11);d.set(RPGAttribute.SPARK_POTENTIAL,1);d.spark=25;
            p.getAbilities().invulnerable=true;p.setHealth(28);ProgressionManager.refresh(p,10,false);
        });
        if(tick==70){
            check(ClientData.get().level==12&&ClientData.get().get(RPGAttribute.STRENGTH)==14,"server-to-client progression sync");
            mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new CharacterScreen());
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),10,10);
        }
        if(tick==90)capture="character-ru-scale2.png";
        if(tick==100&&mc.screen instanceof CharacterScreen screen){
            double s=Math.min(1.5,Math.min(screen.width/640.0,screen.height/360.0));
            double x=(screen.width-640*s)/2+574*s,y=(screen.height-360*s)/2+89*s;
            check(screen.mouseClicked(x,y,0),"scaled supplied button accepts mouse click");screen.mouseReleased(x,y,0);
        }
        if(tick==120){check(ClientData.get().get(RPGAttribute.STRENGTH)==15&&ClientData.get().points==2,"C2S spend / S2C sync");mc.options.guiScale().set(3);mc.resizeDisplay();}
        if(tick==125&&mc.screen instanceof CharacterScreen screen){
            double s=Math.min(1.5,Math.min(screen.width/640.0,screen.height/360.0));
            double x=(screen.width-640*s)/2+574*s,y=(screen.height-360*s)/2+154*s;
            check(screen.mouseClicked(x,y,0),"sixth supplied button accepts scaled click");screen.mouseReleased(x,y,0);
        }
        if(tick==135){check(ClientData.get().get(RPGAttribute.SPARK_POTENTIAL)==2&&ClientData.get().points==1&&ClientData.get().maxSpark()==30,"spark potential upgrade synchronized");check(ClientData.get().manaActive==dev.rpgprogression.compat.IronsCompat.enabled(),"mana enabled only with Iron's");}
        if(tick==140)capture="character-ru-scale3.png";
        if(tick==150){mc.setScreen(new InventoryScreen(mc.player));}
        if(tick==170)capture="inventory-locked.png";
        if(tick==180){mc.setScreen(null);}
        if(tick==200)capture="hud-idle.png";
        boolean parcool=dev.rpgprogression.compat.ParCoolCompat.enabled();
        if(parcool)ParCoolClientSmoke.tick(tick,mc,ClientSmokeTest::check);
        if(dev.rpgprogression.compat.EpicFightCompat.enabled())EpicFightClientSmoke.tick(tick,mc,ClientSmokeTest::check);
        if(dev.rpgprogression.compat.IronsCompat.enabled())IronsClientSmoke.tick(tick,mc,ClientSmokeTest::check);
        if(tick==400)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var d=ProgressionManager.get(p);
            d.stamina=55;d.exhausted=false;d.regenCooldown=200;
            d.spark=8;d.mana=30;d.manaRegenCooldown=200;ModNetworking.resources(p);
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
            p.setAirSupply(0);p.giveExperiencePoints(3);ModNetworking.stamina(p);
        });
        if(tick==420){
            check(dev.rpgprogression.client.hud.CenteredHud.staminaVisible(),"stamina visible while spent");
            check(dev.rpgprogression.client.hud.CenteredHud.experienceVisible(),"XP shown after use");
            check(mc.player.getArmorValue()>0&&mc.player.getAirSupply()<mc.player.getMaxAirSupply(),"conditional armor and air present");
            capture="hud-active.png";
        }
        if(tick==440)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var d=ProgressionManager.get(p);
            d.stamina=d.maxStamina();d.exhausted=false;d.regenCooldown=0;
            d.spark=d.maxSpark();d.mana=d.manaCapacity;ModNetworking.resources(p);
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.item.ItemStack.EMPTY);p.setAirSupply(p.getMaxAirSupply());ModNetworking.stamina(p);
        });
        if(tick==535){
            check(!dev.rpgprogression.client.hud.CenteredHud.staminaVisible()&&!dev.rpgprogression.client.hud.CenteredHud.experienceVisible(),"inactive stamina and XP hide again");
            capture="hud-recovered.png";
        }
        if(tick==545)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var d=ProgressionManager.get(p);
            d.level=100;d.xp=0;d.points=0;ModNetworking.sync(p);
        });
        if(tick==560){mc.setScreen(new CharacterScreen());}
        if(tick==580)capture="character-max-level.png";
        if(tick==600)mc.setScreen(null);
        if(tick==610)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var d=ProgressionManager.get(p);var origin=p.serverLevel();
            var target=p.server.getLevel(net.minecraft.world.level.Level.NETHER);double x=p.getX(),y=p.getY(),z=p.getZ();d.spark=25;
            p.teleportTo(target,0,110,0,0,0);check(p.serverLevel()==target&&d.spark==0,"real dimension teleport charges exactly25");
            p.teleportTo(origin,x,y,z,0,0);check(p.serverLevel()==target&&d.spark==0,"real return teleport blocked without reservoir");
            d.spark=25;p.teleportTo(origin,x,y,z,0,0);check(p.serverLevel()==origin&&d.spark==0,"refilled real return charges exactly once");
        });
        if(tick==640)check(ClientData.get().spark<1,"dimension cost synchronized to client");
        if(Boolean.getBoolean("rpgprogression.hudSmokeTest"))HudClientSmoke.tick(tick,mc,ClientSmokeTest::check);
        if(tick==650&&!Boolean.getBoolean("rpgprogression.hudSmokeTest"))finish(mc);
    }
    static void captureHud(String name){capture=name;}
    static void finish(Minecraft mc){LogUtils.getLogger().info("RPG_CLIENT_SMOKE {}",failed?"FAILED":"PASSED");mc.stop();}
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
