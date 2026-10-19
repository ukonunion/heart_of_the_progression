package dev.rpgprogression.event;

import dev.rpgprogression.RPGProgression;
import dev.rpgprogression.attribute.*;
import dev.rpgprogression.command.RPGCommand;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.defense.*;
import dev.rpgprogression.inventory.InventoryLockManager;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.recipe.IntelligenceRecipeManager;
import dev.rpgprogression.stamina.StaminaManager;
import dev.rpgprogression.xp.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=RPGProgression.MOD_ID)
public final class CommonEvents {
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        PlayerProgressionProvider.get(e.getOriginal()).ifPresent(old -> PlayerProgressionProvider.get(e.getEntity()).ifPresent(data -> {
            data.load(old.save());
            if(e.isWasDeath()){data.stamina=data.maxStamina();data.exhausted=false;dev.rpgprogression.food.FoodManager.clear(data);}
        }));
    }
    private static void ready(Player player) {
        if(player instanceof ServerPlayer p&&!p.isRemoved())PlayerProgressionProvider.get(p).ifPresent(d->{
            d.sampledPosition=false;ModNetworking.rules(p);ProgressionManager.refresh(p,d.get(RPGAttribute.INTELLIGENCE),false);
        });
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){ready(e.getEntity());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){ready(e.getEntity());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){ready(e.getEntity());}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e) {
        // Player.tick posts END even when death/removal invalidated capabilities earlier in that tick.
        if(!(e.getEntity() instanceof ServerPlayer p)||p.isRemoved()||!p.isAlive())return;
        PlayerProgression d=PlayerProgressionProvider.get(p).orElse(null);
        if(d==null)return;
        d.initialize();
        dev.rpgprogression.food.FoodManager.tick(p,d);
        StaminaManager.tick(p,d); XPManager.running(p,d);
        dev.rpgprogression.resource.SparkManager.tick(p,d);dev.rpgprogression.compat.IronsCompat.tick(p,d);
        if(p.tickCount%20==0){InventoryLockManager.sanitize(p);if(EquipmentDefense.updateCache(p,d))d.dirty=true;}
        if(++d.syncTicks>=RPGConfig.STAMINA_SYNC_TICKS.get()) {
            d.syncTicks=0;
            if(d.dirty)ModNetworking.sync(p);
            else if(Math.abs(d.stamina-d.lastSentStamina)>=RPGConfig.STAMINA_SYNC_DELTA.get()||p.tickCount%100==0)ModNetworking.stamina(p);
            if(Math.abs(d.spark-d.lastSentSpark)>=0.05||Math.abs(d.mana-d.lastSentMana)>=0.5||d.manaCapacity!=d.lastSentManaCapacity||d.manaActive!=d.lastSentManaActive||
                    (d.spark>=d.maxSpark()&&d.lastSentSpark<d.maxSpark())||(d.mana>=d.manaCapacity&&d.lastSentMana<d.manaCapacity)||p.tickCount%100==0)ModNetworking.resources(p);
        }
    }
    @SubscribeEvent public static void serverPreTick(ServerTickEvent.Pre e) { ConfigEvents.upgradeServerConfig(); }
    @SubscribeEvent public static void serverTick(ServerTickEvent.Post e) {
        if(IntelligenceRecipeManager.reloadPending) {
            IntelligenceRecipeManager.reloadPending=false;
            for(ServerPlayer p:e.getServer().getPlayerList().getPlayers())ready(p);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void attack(AttackEntityEvent e) {
        if(!(e.getEntity() instanceof ServerPlayer p)||p.isRemoved()||!p.isAlive()||StaminaManager.exempt(p))return;
        if(dev.rpgprogression.compat.EpicFightCompat.battle(p))return;
        PlayerProgression d=ProgressionManager.get(p);
        if(d.attackCooldown>0){if(d.exhausted)e.setCanceled(true);return;}
        d.attackCooldown=d.exhausted?Math.max(2,(int)Math.ceil(20/p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED))):2;
        StaminaManager.spend(p,d,StaminaManager.regularAttackCost());
    }
    @SubscribeEvent public static void breakSpeed(PlayerEvent.BreakSpeed e) {
        PlayerProgressionProvider.get(e.getEntity()).ifPresent(d->{if(d.exhausted&&!StaminaManager.exempt(e.getEntity()))e.setNewSpeed(e.getNewSpeed()*RPGConfig.EXHAUSTED_MINING.get().floatValue());});
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void killed(LivingDeathEvent e) {
        if(e.getSource().getEntity() instanceof ServerPlayer p)XPManager.killed(p,e.getEntity());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void fished(ItemFishedEvent e) {
        if(e.getEntity() instanceof ServerPlayer p)XPManager.fished(p,e.getDrops());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void placed(BlockEvent.EntityPlaceEvent e) {
        if(!(e.getLevel() instanceof ServerLevel l)||!(e.getEntity() instanceof Player))return;
        PlacedBlockTracker tracker=PlacedBlockTracker.get(l);
        if(e instanceof BlockEvent.EntityMultiPlaceEvent multi) multi.getReplacedBlockSnapshots().forEach(s->tracker.mark(s.getPos()));
        else tracker.mark(e.getPos());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void piston(PistonEvent.Pre e) {
        if(!(e.getLevel() instanceof ServerLevel l))return;
        var helper=e.getStructureHelper();
        if(helper!=null&&helper.resolve()) {
            var tracker=PlacedBlockTracker.get(l);
            // Conservatively retain source markers too: other mods can still cancel the piston.
            for(var pos:helper.getToPush())if(tracker.contains(pos))tracker.mark(pos.relative(e.getDirection(),e.getPistonMoveType()==PistonEvent.PistonMoveType.EXTEND?1:-1));
        }
    }
    // Armor and native spell resistance already acted. Apply only the RPG contribution.
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hurt(LivingDamageEvent.Pre e) {
        if(!(e.getEntity() instanceof ServerPlayer p)||p.isRemoved()||!p.isAlive())return;
        if(e.getSource().is(net.minecraft.world.damagesource.DamageTypes.STARVE)){e.setNewDamage(0);return;}
        DefenseType type=DefenseManager.classify(e.getSource(),p);
        if(type!=null)e.setNewDamage(e.getNewDamage()*(float)(1-DefenseManager.percent(ProgressionManager.get(p),type)/100));
    }
    @SubscribeEvent public static void useTick(LivingEntityUseItemEvent.Tick e) {
        if(!(e.getEntity() instanceof Player p)||!(e.getItem().getItem() instanceof BowItem||e.getItem().getItem() instanceof CrossbowItem))return;
        PlayerProgressionProvider.get(p).ifPresent(d->{
            double ratio=AttributeManager.drawRatio(p);
            d.drawRemainder+=1/ratio-1;
            int extra=(int)d.drawRemainder; d.drawRemainder-=extra;
            if(extra>0)e.setDuration(Math.max(1,e.getDuration()-extra));
        });
    }
    @SubscribeEvent public static void useStart(LivingEntityUseItemEvent.Start e) {
        if(e.getEntity() instanceof Player p)PlayerProgressionProvider.get(p).ifPresent(d->d.drawRemainder=0);
    }
    @SubscribeEvent public static void reload(AddReloadListenerEvent e) {e.addListener(IntelligenceRecipeManager.INSTANCE);}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){RPGCommand.register(e.getDispatcher());}
    private CommonEvents() {}
}
