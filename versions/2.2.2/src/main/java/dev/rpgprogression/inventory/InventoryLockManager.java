package dev.rpgprogression.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class InventoryLockManager {
    /** Relocate legacy items without deleting on full inventories or cancelled item tosses. */
    public static void sanitize(ServerPlayer p) {
        Inventory inv=p.getInventory(); int unlocked=InventorySlotRules.unlocked(p);
        boolean changed=false;
        for(int index=9+unlocked;index<36;index++) {
            ItemStack stack=inv.getItem(index);
            if(stack.isEmpty())continue;
            for(int dest=0;dest<9+unlocked&&!stack.isEmpty();dest++) {
                ItemStack other=inv.getItem(dest);
                if(!other.isEmpty()&&ItemStack.isSameItemSameComponents(other,stack)) {
                    int count=Math.min(stack.getCount(),Math.min(other.getMaxStackSize(),inv.getMaxStackSize())-other.getCount());
                    if(count>0){ other.grow(count); stack.shrink(count); changed=true; }
                }
            }
            for(int dest=0;dest<9+unlocked&&!stack.isEmpty();dest++) {
                if(inv.getItem(dest).isEmpty()) { inv.setItem(dest,stack.split(Math.min(stack.getCount(),stack.getMaxStackSize()))); changed=true; }
            }
            if(!stack.isEmpty()) {
                ItemEntity drop=p.drop(stack.copy(),false,false);
                if(drop!=null) { drop.setTarget(p.getUUID()); stack.setCount(0); changed=true; }
                // A cancelled drop leaves the remainder in its original, still locked slot.
            }
            if(stack.isEmpty())inv.setItem(index,ItemStack.EMPTY);
        }
        if(changed){ inv.setChanged(); p.containerMenu.broadcastChanges(); }
    }
    private InventoryLockManager() {}
}
