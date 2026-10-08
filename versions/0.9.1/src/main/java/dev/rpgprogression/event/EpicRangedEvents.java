package dev.rpgprogression.event;

import dev.rpgprogression.RPGProgression;
import dev.rpgprogression.compat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Native projectiles bypass Epic Fight's melee strike callback. */
@Mod.EventBusSubscriber(modid=RPGProgression.MOD_ID)
public final class EpicRangedEvents {
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void bow(ArrowLooseEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&EpicFightCompat.battle(p)&&e.hasAmmo()&&BowItem.getPowerForTime(e.getCharge())>=.1F
                &&!EpicFightBridge.canRanged(p,"bow"))e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void crossbow(PlayerInteractEvent.RightClickItem e){
        if(e.getEntity() instanceof ServerPlayer p&&EpicFightCompat.battle(p)&&e.getItemStack().getItem() instanceof CrossbowItem
                &&CrossbowItem.isCharged(e.getItemStack())&&!EpicFightBridge.canRanged(p,"crossbow")){
            e.setCancellationResult(InteractionResult.FAIL);e.setCanceled(true);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void trident(LivingEntityUseItemEvent.Stop e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!EpicFightCompat.battle(p)||!(e.getItem().getItem() instanceof TridentItem)
                ||e.getItem().getUseDuration()-e.getDuration()<10)return;
        int riptide=EnchantmentHelper.getRiptide(e.getItem());
        if(riptide>0&&!p.isInWaterOrRain())return;
        if(!EpicFightBridge.canRanged(p,"spear")){e.setCanceled(true);return;}
        // Riptide propels the player and never spawns a projectile.
        if(riptide>0)EpicFightBridge.spendRanged(p,"spear");
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void projectile(EntityJoinLevelEvent e){
        if(e.loadedFromDisk()||e.getLevel().isClientSide()||!(e.getEntity() instanceof Projectile shot)
                ||!(shot.getOwner() instanceof ServerPlayer p)||!EpicFightCompat.battle(p))return;
        String category=null;
        if(shot instanceof ThrownTrident)category="spear";
        else if(shot instanceof AbstractArrow||shot instanceof FireworkRocketEntity&&((FireworkRocketEntity)shot).isShotAtAngle()){
            for(InteractionHand hand:InteractionHand.values()){
                Item held=p.getItemInHand(hand).getItem();
                if(held instanceof CrossbowItem){category="crossbow";break;}
                if(held instanceof BowItem)category="bow";
            }
        }
        if(category!=null&&!EpicFightBridge.spendRanged(p,category))e.setCanceled(true);
    }
    private EpicRangedEvents(){}
}
