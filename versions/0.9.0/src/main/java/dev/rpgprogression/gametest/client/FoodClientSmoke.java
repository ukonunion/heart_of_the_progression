package dev.rpgprogression.gametest.client;

import com.mojang.logging.LogUtils;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.gui.CharacterScreen;
import dev.rpgprogression.client.hud.CenteredHud;
import dev.rpgprogression.compat.IronsCompat;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;

@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class FoodClientSmoke {
    private static boolean loading;private static volatile boolean failed;private static int ticks,air=120;private static String capture;private static Horse mount;
    private static net.minecraft.server.level.ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable error){failed=true;LogUtils.getLogger().error("RPG_FOOD_CLIENT exception",error);}});}
    private static void check(boolean ok,String message){if(!ok){failed=true;LogUtils.getLogger().error("RPG_FOOD_CLIENT FAILED {}",message);}else LogUtils.getLogger().info("RPG_FOOD_CLIENT verified {}",message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("rpgprogression.foodSmoke")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld","RPGFoodV180Combined"));return;}
        if(loading&&mc.player==null){String label=mc.screen instanceof BackupConfirmScreen?net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;}
        if(mc.player==null||mc.getSingleplayerServer()==null)return;ticks++;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        if(ticks==30){mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setScreen(null);
            server(mc,()->{var p=player(mc);p.serverLevel().setDayTime(6000);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.removeAllEffects();p.getAbilities().invulnerable=true;
                var d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();ProgressionManager.refresh(p,10,false);
                d.stamina=d.maxStamina()*.55;d.regenCooldown=20000;p.setHealth(p.getMaxHealth()*.6f);p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
                if(IronsCompat.enabled())OptionalManaClientSmoke.foodHudFixture(p);
                mount=EntityType.HORSE.create(p.serverLevel());mount.moveTo(p.getX(),p.getY(),p.getZ(),p.getYRot(),0);mount.setTamed(true);mount.setNoAi(true);mount.setSilent(true);mount.setInvulnerable(true);
                mount.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(60);mount.setHealth(36);mount.equipSaddle(net.minecraft.sounds.SoundSource.NEUTRAL);p.serverLevel().addFreshEntity(mount);check(p.startRiding(mount,true),"mounted fixture");ModNetworking.sync(p);});}
        if(ticks>=65&&ticks<=240){mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(),false);mc.player.setAirSupply(air);
            try{var f=net.minecraft.client.player.LocalPlayer.class.getDeclaredField("jumpRidingScale");f.setAccessible(true);f.setFloat(mc.player,.65f);}catch(Exception error){check(false,"jump fixture "+error);}}
        if(ticks==90){check(CenteredHud.oxygenVisible()&&CenteredHud.staminaVisible()&&CenteredHud.riddenAnimal()!=null,"all central indicators visible");
            for(var overlay:new VanillaGuiOverlay[]{VanillaGuiOverlay.FOOD_LEVEL,VanillaGuiOverlay.ARMOR_LEVEL,VanillaGuiOverlay.AIR_LEVEL}){var event=new RenderGuiOverlayEvent.Pre(mc.getWindow(),new net.minecraft.client.gui.GuiGraphics(mc,mc.renderBuffers().bufferSource()),0,overlay.type());CenteredHud.original(event);check(event.isCanceled(),"native overlay disabled: "+overlay.id());}
            check(!IronsCompat.enabled()||ClientData.get().manaActive,"optional mana active");capture="hud-all-indicators.png";}
        if(ticks==125)mc.setScreen(new CharacterScreen());
        if(ticks==140)capture="character-defense.png";
        if(ticks==155){mc.setScreen(null);air=60;}
        if(ticks==175)capture="oxygen-low.png";
        if(ticks==190){mc.options.guiScale().set(3);mc.resizeDisplay();}
        if(ticks==215)capture="hud-scale3.png";
        if(ticks==241){mc.options.guiScale().set(2);mc.resizeDisplay();mc.player.setAirSupply(mc.player.getMaxAirSupply());server(mc,()->{var p=player(mc);p.stopRiding();mount.discard();p.setAirSupply(p.getMaxAirSupply());ArmorFoodReportCapture.write(p);});}
        if(ticks==270){check(!CenteredHud.oxygenVisible()&&CenteredHud.riddenAnimal()==null,"air/mount hide when unused");capture="unused-indicators-hidden.png";}
        if(ticks==320){LogUtils.getLogger().info("RPG_FOOD_CLIENT_RESULT {}",failed?"FAILED":"PASSED");mc.stop();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){if(capture==null||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();String name=capture;capture=null;
        var file=mc.gameDirectory.toPath().resolve("screenshots/food-v180-"+(IronsCompat.enabled()?"combined":"standalone")).resolve(name);
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(file.getParent());pixels.writeToFile(file);LogUtils.getLogger().info("RPG_FOOD_CLIENT screenshot {}",file);}catch(Exception error){check(false,"screenshot "+error);}}
}
