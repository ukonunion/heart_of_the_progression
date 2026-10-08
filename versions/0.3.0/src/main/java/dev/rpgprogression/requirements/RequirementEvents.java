package dev.rpgprogression.requirements;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="rpgprogression")
public final class RequirementEvents {
    private static final Map<Player,Long> lastMessage=new WeakHashMap<>();
    private static boolean blocked(Player p,ItemStack stack){
        if(ItemRequirements.allowed(p,stack))return false;
        if(p instanceof ServerPlayer){
            long tick=p.level().getGameTime();
            if(tick-lastMessage.getOrDefault(p,-100L)>=20){p.displayClientMessage(Component.translatable("rpgprogression.requirements.blocked"),true);lastMessage.put(p,tick);}
        }
        return true;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent e){if(blocked(e.getEntity(),e.getEntity().getMainHandItem()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void item(PlayerInteractEvent.RightClickItem e){if(blocked(e.getEntity(),e.getItemStack())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void block(PlayerInteractEvent.RightClickBlock e){if(blocked(e.getEntity(),e.getItemStack())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void entity(PlayerInteractEvent.EntityInteract e){if(blocked(e.getEntity(),e.getItemStack())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void entitySpecific(PlayerInteractEvent.EntityInteractSpecific e){if(blocked(e.getEntity(),e.getItemStack())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void mine(PlayerInteractEvent.LeftClickBlock e){if(blocked(e.getEntity(),e.getItemStack()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void broken(BlockEvent.BreakEvent e){if(blocked(e.getPlayer(),e.getPlayer().getMainHandItem()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void start(LivingEntityUseItemEvent.Start e){if(e.getEntity() instanceof Player p&&blocked(p,e.getItem()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void use(LivingEntityUseItemEvent.Tick e){if(e.getEntity() instanceof Player p&&!ItemRequirements.allowed(p,e.getItem()))p.stopUsingItem();}
    /** Covers armor installed through inventories, dispensers, commands and requirements changed later. */
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.START||!(e.player instanceof ServerPlayer p))return;
        for(EquipmentSlot slot:EquipmentSlot.values())if(slot.getType()==EquipmentSlot.Type.ARMOR){
            ItemStack stack=p.getItemBySlot(slot);
            if(!stack.isEmpty()&&blocked(p,stack)){
                p.setItemSlot(slot,ItemStack.EMPTY);
                p.getInventory().add(stack);
                if(!stack.isEmpty())p.drop(stack,false);
                p.inventoryMenu.broadcastChanges();
            }
        }
    }
    private RequirementEvents(){}
}
