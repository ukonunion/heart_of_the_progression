package dev.rpgprogression.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="rpgprogression")
public final class DisabledWeaponEvents {
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent e){if(DisabledIronsWeapons.blocked(e.getEntity().getMainHandItem()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void item(PlayerInteractEvent.RightClickItem e){if(DisabledIronsWeapons.blocked(e.getItemStack())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void block(PlayerInteractEvent.RightClickBlock e){if(DisabledIronsWeapons.blocked(e.getItemStack())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void entity(PlayerInteractEvent.EntityInteract e){if(DisabledIronsWeapons.blocked(e.getItemStack())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void precise(PlayerInteractEvent.EntityInteractSpecific e){if(DisabledIronsWeapons.blocked(e.getItemStack())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void mine(PlayerInteractEvent.LeftClickBlock e){if(DisabledIronsWeapons.blocked(e.getItemStack()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void broken(BlockEvent.BreakEvent e){if(DisabledIronsWeapons.blocked(e.getPlayer().getMainHandItem()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void start(LivingEntityUseItemEvent.Start e){if(DisabledIronsWeapons.blocked(e.getItem()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void use(LivingEntityUseItemEvent.Tick e){if(DisabledIronsWeapons.blocked(e.getItem()))e.getEntity().stopUsingItem();}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void dropped(EntityJoinLevelEvent e){
        if(e.getEntity() instanceof ItemEntity item&&DisabledIronsWeapons.blocked(item.getItem())){item.discard();e.setCanceled(true);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void equipment(EntityTickEvent.Pre e){
        if(!(e.getEntity() instanceof net.minecraft.world.entity.LivingEntity living))return;
        if(e.getEntity().level().isClientSide)return;
        for(var slot:EquipmentSlot.values())if(DisabledIronsWeapons.blocked(living.getItemBySlot(slot)))living.setItemSlot(slot,ItemStack.EMPTY);
    }
    /** Catch commands, creative packets, injected inventories and existing open container contents. */
    public static void sanitize(ServerPlayer player){
        boolean changed=false;var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++)if(DisabledIronsWeapons.blocked(inventory.getItem(i))){inventory.setItem(i,ItemStack.EMPTY);changed=true;}
        for(var slot:player.containerMenu.slots)if(DisabledIronsWeapons.blocked(slot.getItem())){slot.set(ItemStack.EMPTY);changed=true;}
        if(DisabledIronsWeapons.blocked(player.containerMenu.getCarried())){player.containerMenu.setCarried(ItemStack.EMPTY);changed=true;}
        if(changed){inventory.setChanged();player.containerMenu.broadcastChanges();}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void tick(PlayerTickEvent.Pre e){if(e.getEntity() instanceof ServerPlayer p&&!p.isRemoved())sanitize(p);}
    private DisabledWeaponEvents(){}
}
