package dev.rpgprogression.requirements;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

@EventBusSubscriber(modid="rpgprogression")
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
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void block(PlayerInteractEvent.RightClickBlock e){
        var p=e.getEntity();boolean denied=blocked(p,e.getItemStack());
        // Empty-hand interaction must check the placed workstation, not only the held item.
        // Sneak-placement against its side does not activate the block's menu.
        if(!denied&&(!p.isSecondaryUseActive()||p.getMainHandItem().isEmpty()&&p.getOffhandItem().isEmpty()))
            denied=blocked(p,new ItemStack(e.getLevel().getBlockState(e.getPos()).getBlock()));
        if(denied){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void entity(PlayerInteractEvent.EntityInteract e){if(blocked(e.getEntity(),e.getItemStack())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void entitySpecific(PlayerInteractEvent.EntityInteractSpecific e){if(blocked(e.getEntity(),e.getItemStack())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void mine(PlayerInteractEvent.LeftClickBlock e){if(blocked(e.getEntity(),e.getItemStack()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void broken(BlockEvent.BreakEvent e){if(blocked(e.getPlayer(),e.getPlayer().getMainHandItem()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void start(LivingEntityUseItemEvent.Start e){if(e.getEntity() instanceof Player p&&blocked(p,e.getItem()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void use(LivingEntityUseItemEvent.Tick e){if(e.getEntity() instanceof Player p&&!ItemRequirements.allowed(p,e.getItem()))p.stopUsingItem();}
    /** Covers armor installed through inventories, dispensers, commands and requirements changed later. */
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void tick(PlayerTickEvent.Pre e){
        if(!(e.getEntity() instanceof ServerPlayer p)||p.isRemoved()||!p.isAlive()
                ||dev.rpgprogression.progression.PlayerProgressionProvider.get(p).isEmpty())return;
        ItemStack station=p.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu?new ItemStack(net.minecraft.world.item.Items.ENCHANTING_TABLE)
                :p.containerMenu instanceof net.minecraft.world.inventory.BrewingStandMenu?new ItemStack(net.minecraft.world.item.Items.BREWING_STAND):ItemStack.EMPTY;
        if(station.isEmpty()&&dev.rpgprogression.compat.IronsCompat.present())station=dev.rpgprogression.compat.IronsRequirements.station(p.containerMenu);
        if(!station.isEmpty()&&blocked(p,station))p.closeContainer();
        if(dev.rpgprogression.compat.IronsCompat.present()){
            dev.rpgprogression.compat.CurioRequirementGuards.validate(p);
            dev.rpgprogression.compat.IronsRequirements.validateCast(p);
        }
        for(EquipmentSlot slot:EquipmentSlot.values())if(slot.getType()==EquipmentSlot.Type.HUMANOID_ARMOR){
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
