package dev.rpgprogression.gametest.client;

import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.client.hud.CenteredHud;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.*;
import java.nio.file.Files;
import java.util.function.BiConsumer;

/** Own smoke-world fixture only: activate every currently supported HUD element together. */
public final class AllIndicatorsClientFixture {
    private static Horse mount;
    public static void tick(int tick,Minecraft mc,BiConsumer<Boolean,String> check){
        if(tick==525){mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.setScreen(null);
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var d=ProgressionManager.get(p);
                p.removeAllEffects();p.getAbilities().invulnerable=true;p.setHealth(p.getMaxHealth()*.6f);
                p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));p.setAirSupply(120);p.setExperienceLevels(7);p.setExperiencePoints((int)(p.getXpNeededForNextLevel()*.55));
                d.stamina=d.maxStamina()*.55;d.exhausted=false;d.regenCooldown=20000;d.mana=d.manaCapacity*.4;d.manaRegenCooldown=20000;
                mount=EntityType.HORSE.create(p.serverLevel());mount.moveTo(p.getX(),p.getY(),p.getZ(),p.getYRot(),0);mount.setTamed(true);mount.setNoAi(true);mount.setSilent(true);mount.setInvulnerable(true);
                mount.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(60);mount.setHealth(36);mount.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.SADDLE));
                p.serverLevel().addFreshEntity(mount);check.accept(p.startRiding(mount,true),"all-indicators mount attached");ModNetworking.sync(p);
            });
        }
        if(tick>=540&&tick<590){
            mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(),false);
            // Air normally refills above water; keep the reduced-air fixture visible
            // without submerging the camera or covering the screenshot with water.
            mc.player.setAirSupply(120);
            try{var jump=net.minecraft.client.player.LocalPlayer.class.getDeclaredField("jumpRidingScale");jump.setAccessible(true);jump.setFloat(mc.player,.65f);}
            catch(ReflectiveOperationException error){check.accept(false,"HUD jump fixture: "+error);}
        }
        if(tick==570){
            var d=ClientData.get();check.accept(CenteredHud.staminaVisible(),"all-indicators stamina active");
            check.accept(mc.player.getArmorValue()>0&&mc.player.getAirSupply()<mc.player.getMaxAirSupply(),"all-indicators armor and air active");
            check.accept(mc.player.getVehicle() instanceof Horse&&mc.player.getJumpRidingScale()>0,"all-indicators mount health and jump active");
            check.accept(!dev.rpgprogression.compat.IronsCompat.enabled()||d.manaActive&&d.mana<d.manaCapacity,"all-indicators mana active with Iron's");
            check.accept(mc.player.experienceLevel==7&&mc.player.experienceProgress>0,"all-indicators vanilla experience active");
        }
        if(tick==575){
            var file=mc.gameDirectory.toPath().resolve("screenshots/"+(Boolean.getBoolean("rpgprogression.manaSmoke")?"mana-v170-":"tree-v161-")+(dev.rpgprogression.skill.SkillTreeManager.parcool()?"combined":"standalone")).resolve("hud-all-indicators.png");
            try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(file.getParent());pixels.writeToFile(file);com.mojang.logging.LogUtils.getLogger().info("RPG_ALL_INDICATORS screenshot {}",file);}
            catch(Exception error){check.accept(false,"HUD screenshot: "+error);}
        }
        if(tick==590)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.stopRiding();if(mount!=null){mount.discard();mount=null;}p.setAirSupply(p.getMaxAirSupply());p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);});
    }
    private AllIndicatorsClientFixture(){}
}
