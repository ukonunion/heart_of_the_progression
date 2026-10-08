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
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=RPGProgression.MOD_ID)
public final class CommonEvents {
    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<Entity> e) {
        if(e.getObject() instanceof Player){
            PlayerProgressionProvider provider=new PlayerProgressionProvider();
            e.addCapability(new ResourceLocation(RPGProgression.MOD_ID,"progression"),provider); e.addListener(provider::invalidate);
        }
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        e.getOriginal().reviveCaps();
        try { e.getOriginal().getCapability(PlayerProgressionProvider.CAPABILITY).ifPresent(old->
            e.getEntity().getCapability(PlayerProgressionProvider.CAPABILITY).ifPresent(d->{d.load(old.save()); if(e.isWasDeath()){d.stamina=d.maxStamina();d.exhausted=false;} }));
        } finally { e.getOriginal().invalidateCaps(); }
    }
    private static void ready(Player player) {
        if(player instanceof ServerPlayer p){ PlayerProgression d=ProgressionManager.get(p);d.sampledPosition=false;ModNetworking.rules(p);ProgressionManager.refresh(p,d.get(RPGAttribute.INTELLIGENCE),false); }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){ready(e.getEntity());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){ready(e.getEntity());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){ready(e.getEntity());}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        PlayerProgression d=ProgressionManager.get(p); d.initialize();
        StaminaManager.tick(p,d); XPManager.running(p,d);
        dev.rpgprogression.resource.SparkManager.tick(p,d);dev.rpgprogression.compat.IronsCompat.tick(p,d);
        if(p.tickCount%20==0)InventoryLockManager.sanitize(p);
        if(++d.syncTicks>=RPGConfig.STAMINA_SYNC_TICKS.get()) {
            d.syncTicks=0;
            if(d.dirty)ModNetworking.sync(p);
            else if(Math.abs(d.stamina-d.lastSentStamina)>=RPGConfig.STAMINA_SYNC_DELTA.get()||p.tickCount%100==0)ModNetworking.stamina(p);
            if(Math.abs(d.spark-d.lastSentSpark)>=0.05||Math.abs(d.mana-d.lastSentMana)>=0.5||d.manaCapacity!=d.lastSentManaCapacity||d.manaActive!=d.lastSentManaActive||
                    (d.spark>=d.maxSpark()&&d.lastSentSpark<d.maxSpark())||(d.mana>=d.manaCapacity&&d.lastSentMana<d.manaCapacity)||p.tickCount%100==0)ModNetworking.resources(p);
        }
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e) {
        if(e.phase==TickEvent.Phase.END&&IntelligenceRecipeManager.reloadPending) {
            IntelligenceRecipeManager.reloadPending=false;
            for(ServerPlayer p:e.getServer().getPlayerList().getPlayers())ready(p);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void attack(AttackEntityEvent e) {
        if(!(e.getEntity() instanceof ServerPlayer p)||StaminaManager.exempt(p))return;
        if(dev.rpgprogression.compat.EpicFightCompat.battle(p))return;
        PlayerProgression d=ProgressionManager.get(p);
        if(d.attackCooldown>0){if(d.exhausted)e.setCanceled(true);return;}
        d.attackCooldown=d.exhausted?Math.max(2,(int)Math.ceil(20/p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED))):2;
        StaminaManager.spend(p,d,RPGConfig.ATTACK_COST.get());
    }
    @SubscribeEvent public static void breakSpeed(PlayerEvent.BreakSpeed e) {
        e.getEntity().getCapability(PlayerProgressionProvider.CAPABILITY).ifPresent(d->{if(d.exhausted&&!StaminaManager.exempt(e.getEntity()))e.setNewSpeed(e.getNewSpeed()*RPGConfig.EXHAUSTED_MINING.get().floatValue());});
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
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hurt(LivingHurtEvent e) {
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        DefenseType type=DefenseManager.classify(e.getSource());
        if(type!=null)e.setAmount(e.getAmount()*(float)(1-DefenseManager.percent(ProgressionManager.get(p),type)/100));
    }
    @SubscribeEvent public static void useTick(LivingEntityUseItemEvent.Tick e) {
        if(!(e.getEntity() instanceof Player p)||!(e.getItem().getItem() instanceof BowItem||e.getItem().getItem() instanceof CrossbowItem))return;
        p.getCapability(PlayerProgressionProvider.CAPABILITY).ifPresent(d->{
            double ratio=AttributeManager.drawRatio(p);
            d.drawRemainder+=1/ratio-1;
            int extra=(int)d.drawRemainder; d.drawRemainder-=extra;
            if(extra>0)e.setDuration(Math.max(1,e.getDuration()-extra));
        });
    }
    @SubscribeEvent public static void useStart(LivingEntityUseItemEvent.Start e) {
        if(e.getEntity() instanceof Player p)p.getCapability(PlayerProgressionProvider.CAPABILITY).ifPresent(d->d.drawRemainder=0);
    }
    @SubscribeEvent public static void reload(AddReloadListenerEvent e) {e.addListener(IntelligenceRecipeManager.INSTANCE);}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){RPGCommand.register(e.getDispatcher());}
    private CommonEvents() {}
}
