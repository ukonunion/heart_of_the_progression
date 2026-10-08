package dev.rpgprogression.mixin;

import dev.rpgprogression.inventory.InventorySlotRules;
import dev.rpgprogression.recipe.CraftingResultAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge has no cancellable, universal slot acceptance hook. This covers drag and ordinary slots. */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @Inject(method="mayPlace",at=@At("HEAD"),cancellable=true)
    private void rpg$place(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(InventorySlotRules.locked((Slot)(Object)this))cir.setReturnValue(false);
    }
    @Inject(method="mayPickup",at=@At("HEAD"),cancellable=true)
    private void rpg$pickup(Player player,CallbackInfoReturnable<Boolean> cir) {
        if(InventorySlotRules.locked((Slot)(Object)this)||((Object)this instanceof CraftingResultAccess access&&!access.rpg$allowed(player)))cir.setReturnValue(false);
    }
}
