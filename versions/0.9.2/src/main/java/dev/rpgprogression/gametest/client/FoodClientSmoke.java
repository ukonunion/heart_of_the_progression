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
    private static int xpBefore,levelBefore,xpStartedTick;
    private static final com.google.gson.JsonObject screenshots=new com.google.gson.JsonObject();
    private static net.minecraft.server.level.ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable error){failed=true;LogUtils.getLogger().error("RPG_FOOD_CLIENT exception",error);}});}
    private static void check(boolean ok,String message){if(!ok){failed=true;LogUtils.getLogger().error("RPG_FOOD_CLIENT FAILED {}",message);}else LogUtils.getLogger().info("RPG_FOOD_CLIENT verified {}",message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("rpgprogression.foodSmoke")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld","RPGFoodV182Combined"));return;}
        if(loading&&mc.player==null){String label=mc.screen instanceof BackupConfirmScreen?net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();break;}return;}
        if(mc.player==null||mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks>=690)mc.player.setAirSupply(mc.player.getMaxAirSupply());
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        if(ticks==30){mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setScreen(null);
            server(mc,()->{var p=player(mc);p.serverLevel().setDayTime(6000);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.removeAllEffects();p.getAbilities().invulnerable=true;
                var d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();ProgressionManager.refresh(p,10,false);
                d.stamina=d.maxStamina()*.55;d.regenCooldown=20000;p.setHealth(p.getMaxHealth()*.6f);p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
                if(IronsCompat.enabled())OptionalManaClientSmoke.foodHudFixture(p);
                mount=EntityType.HORSE.create(p.serverLevel());mount.moveTo(p.getX(),p.getY(),p.getZ(),p.getYRot(),0);mount.setTamed(true);mount.setNoAi(true);mount.setSilent(true);mount.setInvulnerable(true);
                mount.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(60);mount.setHealth(36);mount.equipSaddle(net.minecraft.sounds.SoundSource.NEUTRAL);p.serverLevel().addFreshEntity(mount);check(p.startRiding(mount,true),"mounted fixture");ModNetworking.sync(p);});}
        if(ticks>=65&&ticks<=240||ticks>=545&&ticks<=680){mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(),false);mc.player.setAirSupply(air);
            try{var f=net.minecraft.client.player.LocalPlayer.class.getDeclaredField("jumpRidingScale");f.setAccessible(true);f.setFloat(mc.player,.65f);}catch(Exception error){check(false,"jump fixture "+error);}}
        if(ticks==90){check(CenteredHud.oxygenVisible()&&CenteredHud.staminaVisible()&&CenteredHud.riddenAnimal()!=null,"all central indicators visible");
            for(var overlay:new VanillaGuiOverlay[]{VanillaGuiOverlay.FOOD_LEVEL,VanillaGuiOverlay.ARMOR_LEVEL,VanillaGuiOverlay.AIR_LEVEL,VanillaGuiOverlay.JUMP_BAR})check(overlayCanceled(mc,overlay),"native overlay disabled: "+overlay.id());
            check(!CenteredHud.experienceVisible()&&overlayCanceled(mc,VanillaGuiOverlay.EXPERIENCE_BAR),"login baseline does not flash unchanged XP");
            check(!IronsCompat.enabled()||ClientData.get().manaActive,"optional mana active");capture="hud-all-indicators.png";}
        if(ticks==125)mc.setScreen(new CharacterScreen());
        if(ticks==140)capture="character-defense.png";
        if(ticks==155){mc.setScreen(null);air=60;}
        if(ticks==175)capture="oxygen-low.png";
        if(ticks==190){mc.options.guiScale().set(3);mc.resizeDisplay();}
        if(ticks==215)capture="hud-scale3.png";
        if(ticks==241){mc.options.guiScale().set(2);mc.resizeDisplay();mc.player.setAirSupply(mc.player.getMaxAirSupply());server(mc,()->{var p=player(mc);p.stopRiding();mount.discard();p.setAirSupply(p.getMaxAirSupply());ArmorFoodReportCapture.write(p);});}
        if(ticks==270){check(!CenteredHud.oxygenVisible()&&CenteredHud.riddenAnimal()==null,"air/mount hide when unused");capture="unused-indicators-hidden.png";}
        if(ticks==280){xpBefore=mc.player.totalExperience;server(mc,()->player(mc).giveExperiencePoints(31));}
        if(ticks>280&&ticks<300&&xpStartedTick==0&&mc.player.totalExperience>xpBefore)xpStartedTick=ticks;
        if(xpStartedTick>0&&ticks==xpStartedTick+2){check(CenteredHud.experienceOpacity()>0&&CenteredHud.experienceOpacity()<1,"XP fade-in has intermediate opacity");capture="xp-appearing.png";}
        if(xpStartedTick>0&&ticks==xpStartedTick+102){check(!CenteredHud.experienceVisible()&&CenteredHud.experienceRendered()&&CenteredHud.experienceOpacity()>0&&CenteredHud.experienceOpacity()<1,"XP fade-out continues after timer expiry");capture="xp-disappearing.png";}
        if(ticks==300){check(mc.player.totalExperience>xpBefore&&CenteredHud.experienceVisible()&&!overlayCanceled(mc,VanillaGuiOverlay.EXPERIENCE_BAR),"real XP gain shows native overlay");capture="xp-gain.png";}
        if(ticks==400){check(!CenteredHud.experienceVisible()&&overlayCanceled(mc,VanillaGuiOverlay.EXPERIENCE_BAR),"unchanged XP hides after five seconds");capture="xp-expired.png";}
        if(ticks==410){levelBefore=mc.player.experienceLevel;server(mc,()->player(mc).onEnchantmentPerformed(ItemStack.EMPTY,3));}
        if(ticks==430){check(mc.player.experienceLevel==levelBefore-3&&CenteredHud.experienceVisible(),"real enchanting cost shows native XP");capture="xp-spent.png";}
        if(ticks==450){mc.options.guiScale().set(3);mc.resizeDisplay();}
        if(ticks==470){check(CenteredHud.experienceVisible(),"XP remains visible at GUI scale three");capture="xp-scale3.png";}
        if(ticks==530)check(!CenteredHud.experienceVisible(),"XP spending window expires");
        if(ticks==540)server(mc,()->{var p=player(mc);mount=EntityType.HORSE.create(p.serverLevel());mount.moveTo(p.getX(),p.getY(),p.getZ(),p.getYRot(),0);mount.setTamed(true);mount.setNoAi(true);mount.setSilent(true);mount.setInvulnerable(true);mount.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(60);mount.setHealth(36);mount.equipSaddle(net.minecraft.sounds.SoundSource.NEUTRAL);p.serverLevel().addFreshEntity(mount);check(p.startRiding(mount,true),"XP mounted fixture");p.giveExperiencePoints(1);});
        if(ticks==548){check(animation("AIR",mc)>0&&animation("AIR",mc)<1,"air appears with intermediate opacity and layout spacing");capture="meters-appearing.png";}
        if(ticks==560){check(CenteredHud.experienceVisible()&&CenteredHud.riddenAnimal()!=null&&overlayCanceled(mc,VanillaGuiOverlay.JUMP_BAR),"mounted XP visible and horse jump overlay disabled");capture="xp-mounted-scale3.png";}
        if(ticks==645){mc.options.guiScale().set(2);mc.resizeDisplay();}
        if(ticks==660){check(!CenteredHud.experienceVisible()&&CenteredHud.riddenAnimal()!=null&&mc.player.getJumpRidingScale()>0,"XP expires even with charged horse jump");capture="xp-mounted-expired.png";}
        if(ticks==690)server(mc,()->{var p=player(mc);p.stopRiding();mount.discard();p.setAirSupply(p.getMaxAirSupply());});
        if(ticks==694){check(CenteredHud.riddenAnimal()==null&&animation("ANIMAL",mc)>0&&animation("ANIMAL",mc)<1,"dismounted animal health fades instead of vanishing immediately");capture="meters-disappearing.png";}
        if(ticks==710){check(animation("AIR",mc)==0&&animation("ANIMAL",mc)==0&&!CenteredHud.experienceRendered(),"animations settle with unused indicators hidden");capture="after-transitions.png";}
        if(ticks==730){LogUtils.getLogger().info("RPG_FOOD_CLIENT_RESULT {}",failed?"FAILED":"PASSED");mc.stop();}
    }
    private static boolean overlayCanceled(Minecraft mc,VanillaGuiOverlay overlay){var event=new RenderGuiOverlayEvent.Pre(mc.getWindow(),new net.minecraft.client.gui.GuiGraphics(mc,mc.renderBuffers().bufferSource()),0,overlay.type());CenteredHud.original(event);return event.isCanceled();}
    private static float animation(String name,Minecraft mc){try{var field=CenteredHud.class.getDeclaredField(name);field.setAccessible(true);return ((dev.rpgprogression.client.hud.HudVisibilityAnimation)field.get(null)).value(mc.getFrameTime());}catch(Exception error){throw new IllegalStateException(error);}}
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){if(capture==null||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();String name=capture;capture=null;
        var file=mc.gameDirectory.toPath().resolve("screenshots/food-v182-"+(IronsCompat.enabled()?"combined":"standalone")).resolve(name);
        try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(file.getParent());pixels.writeToFile(file);
            var state=new com.google.gson.JsonObject();state.addProperty("level",mc.player.experienceLevel);state.addProperty("total",mc.player.totalExperience);state.addProperty("progress",mc.player.experienceProgress);state.addProperty("visible",CenteredHud.experienceVisible());state.addProperty("mounted",CenteredHud.riddenAnimal()!=null);state.addProperty("scale",mc.options.guiScale().get());
            for(var key:new String[]{"HEALTH","STAMINA","ANIMAL","AIR","XP","MANA"})state.addProperty(key.toLowerCase()+"Opacity",animation(key,mc));
            screenshots.add(name,state);Files.writeString(file.getParent().resolve("states.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(screenshots));
            LogUtils.getLogger().info("RPG_FOOD_CLIENT screenshot {}",file);}catch(Exception error){check(false,"screenshot "+error);}}
}
