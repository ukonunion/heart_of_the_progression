package dev.rpgprogression.gametest.client;

import dev.rpgprogression.client.hud.CenteredHud;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import java.util.function.*;

/** Dev-only coverage for mounted/full/damaged/hidden bars at two GUI scales. */
public final class MountedHudClientSmoke {
    private static Animal mount;
    private static final String[] cases={"horse-half","horse-full","pig-half","strider-low","unmounted","donkey-scale3"};
    public static void tick(int t,Minecraft mc,BiConsumer<Boolean,String> check,Consumer<String> capture){
        int stage=t/40,offset=t%40;if(stage>=cases.length){if(t==240)ManaClientSmoke.server(mc,()->clear(ManaClientSmoke.player(mc)));return;}
        if(offset==0){
            mc.options.guiScale().set(stage==5?3:2);mc.resizeDisplay();
            ManaClientSmoke.server(mc,()->{
                var p=ManaClientSmoke.player(mc);clear(p);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                p.getAbilities().invulnerable=true;p.setHealth(p.getMaxHealth()*.6f);p.setExperienceLevels(7);p.setExperiencePoints(7);
                var d=ProgressionManager.get(p);d.stamina=d.maxStamina()*(stage==1?1:.55);d.exhausted=false;d.regenCooldown=20000;
                if(stage!=4){
                    var type=switch(stage){case 2->EntityType.PIG;case 3->EntityType.STRIDER;case 5->EntityType.DONKEY;default->EntityType.HORSE;};
                    mount=(Animal)type.create(p.serverLevel());mount.moveTo(p.getX(),p.getY(),p.getZ(),p.getYRot(),0);mount.setNoAi(true);mount.setSilent(true);mount.setInvulnerable(true);
                    if(mount instanceof AbstractHorse horse)horse.setTamed(true);
                    if(mount instanceof Saddleable animal)animal.equipSaddle(net.minecraft.sounds.SoundSource.NEUTRAL);
                    mount.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(60);mount.setHealth(stage==1?60:stage==3?15:36);
                    p.serverLevel().addFreshEntity(mount);check.accept(p.startRiding(mount,true),"mounted HUD "+cases[stage]);
                }
                check.accept(RPGConfig.BASE_XP.get()==100&&RPGConfig.RULES_VERSION.get()==6,"new XP default and migrated server config");ModNetworking.sync(p);
            });
        }
        if(offset==20){
            check.accept((CenteredHud.riddenAnimal()!=null)==(stage!=4),"animal health visibility "+cases[stage]);
            check.accept(CenteredHud.staminaVisible()==(stage!=1),"stamina conditional stacking "+cases[stage]);
            check.accept(RPGConfig.HUD_BOTTOM.get()==31,"legacy HUD offset moved to the former XP area");
            var event=new RenderGuiOverlayEvent.Pre(mc.getWindow(),new net.minecraft.client.gui.GuiGraphics(mc,mc.renderBuffers().bufferSource()),0,VanillaGuiOverlay.EXPERIENCE_BAR.type());
            CenteredHud.original(event);check.accept(event.isCanceled()!=CenteredHud.experienceRendered(),"vanilla XP HUD follows its animated change window");
            check.accept(mc.player.experienceLevel==7&&mc.player.experienceProgress>0,"vanilla XP preserved "+cases[stage]);
            capture.accept("mount-"+cases[stage]+".png");
        }
    }
    private static void clear(net.minecraft.server.level.ServerPlayer p){p.stopRiding();if(mount!=null){mount.discard();mount=null;}}
    private MountedHudClientSmoke(){}
}
