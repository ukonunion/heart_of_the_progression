package dev.rpgprogression.compat;

import dev.rpgprogression.requirements.ItemRequirements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.*;

/** Registered only when Iron's and its Curios dependency are installed. */
public final class CurioRequirementGuards {
    public static void register(){NeoForge.EVENT_BUS.register(new CurioRequirementGuards());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void equip(CurioCanEquipEvent event){
        if(event.getEntity() instanceof Player p&&!ItemRequirements.allowed(p,event.getStack()))event.setEquipResult(TriState.FALSE);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void attributes(CurioAttributeModifierEvent event){
        if(event.getSlotContext().entity() instanceof Player p&&!ItemRequirements.allowed(p,event.getItemStack()))event.clearModifiers();
    }
    public static void validate(ServerPlayer player){
        CuriosApi.getCuriosInventory(player).ifPresent(inventory->{
            for(var entry:inventory.getCurios().values()){
                var stacks=entry.getStacks();
                for(int i=0;i<stacks.getSlots();i++){
                    ItemStack stack=stacks.getStackInSlot(i);
                    if(!stack.isEmpty()&&!ItemRequirements.allowed(player,stack)){
                        stacks.setStackInSlot(i,ItemStack.EMPTY);player.getInventory().add(stack);
                        if(!stack.isEmpty())player.drop(stack,false);
                        player.inventoryMenu.broadcastChanges();
                    }
                }
            }
        });
    }
    private CurioRequirementGuards(){}
}
