package dev.rpgprogression.event;

import dev.rpgprogression.RPGProgression;
import dev.rpgprogression.stamina.RangedStaminaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/** Charge native bow shots and trident throws independently of optional mods. */
@EventBusSubscriber(modid=RPGProgression.MOD_ID)
public final class EpicRangedEvents {
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void bow(ArrowLooseEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&e.hasAmmo()&&BowItem.getPowerForTime(e.getCharge())>=.1F
                &&!RangedStaminaManager.can(p,"bow"))e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void projectile(EntityJoinLevelEvent e){
        if(e.loadedFromDisk()||e.getLevel().isClientSide()||!(e.getEntity() instanceof Projectile shot)
                ||!(shot.getOwner() instanceof ServerPlayer p))return;
        String category=null;
        if(shot instanceof ThrownTrident)category="spear";
        else if(shot instanceof AbstractArrow arrow&&!arrow.shotFromCrossbow()){
            for(InteractionHand hand:InteractionHand.values()){
                Item held=p.getItemInHand(hand).getItem();
                if(held instanceof BowItem)category="bow";
            }
        }
        if(category!=null&&!RangedStaminaManager.projectile(p,category))e.setCanceled(true);
    }
    private EpicRangedEvents(){}
}
