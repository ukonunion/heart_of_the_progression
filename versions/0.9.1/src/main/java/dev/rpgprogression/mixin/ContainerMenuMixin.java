package dev.rpgprogression.mixin;

import dev.rpgprogression.inventory.InventorySlotRules;
import dev.rpgprogression.recipe.CraftingResultAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMenuMixin {
    /** Validates all click types before vanilla branches into shift, number-key, clone, drag, etc. */
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void rpg$click(int index,int button,ClickType type,Player player,CallbackInfo ci) {
        AbstractContainerMenu menu=(AbstractContainerMenu)(Object)this;
        if(index>=0&&index<menu.slots.size()) {
            Slot slot=menu.slots.get(index);
            if(InventorySlotRules.locked(slot)||(slot instanceof CraftingResultAccess access&&!access.rpg$allowed(player))) {
                ci.cancel(); menu.broadcastChanges();
            }
        }
    }
    /** Vanilla's merge-into-existing-stack branch skips mayPlace; hide locked targets from that branch. */
    @Redirect(method="moveItemStackTo",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack rpg$mergeTarget(Slot slot) { return InventorySlotRules.locked(slot)?ItemStack.EMPTY:slot.getItem(); }
    @Redirect(method="moveItemStackTo",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean rpg$emptyTarget(Slot slot,ItemStack stack) {return !InventorySlotRules.locked(slot)&&slot.mayPlace(stack);}
}
