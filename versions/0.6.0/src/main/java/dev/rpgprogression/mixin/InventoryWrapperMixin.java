package dev.rpgprogression.mixin;

import dev.rpgprogression.inventory.InventorySlotRules;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Also protect retained legacy items from automation while their slots are locked. Forge class: no remap. */
@Mixin(value=InvWrapper.class,remap=false)
public abstract class InventoryWrapperMixin {
    @Shadow public abstract Container getInv();
    @Inject(method="extractItem",at=@At("HEAD"),cancellable=true)
    private void rpg$extract(int slot,int amount,boolean simulate,CallbackInfoReturnable<ItemStack> cir) {
        if(getInv() instanceof Inventory inventory&&InventorySlotRules.locked(inventory.player,slot))cir.setReturnValue(ItemStack.EMPTY);
    }
}
