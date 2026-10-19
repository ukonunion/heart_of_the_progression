package dev.rpgprogression.mixin;

import dev.rpgprogression.inventory.InventorySlotRules;
import dev.rpgprogression.recipe.CraftingResultAccess;
import dev.rpgprogression.xp.FurnaceXPAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge has no cancellable, universal slot acceptance hook. This covers drag and ordinary slots. */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @Inject(method="mayPlace",at=@At("HEAD"),cancellable=true)
    private void rpg$place(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(dev.rpgprogression.compat.DisabledIronsWeapons.blocked(stack)||InventorySlotRules.locked((Slot)(Object)this))cir.setReturnValue(false);
    }
    @Inject(method="mayPickup",at=@At("HEAD"),cancellable=true)
    private void rpg$pickup(Player player,CallbackInfoReturnable<Boolean> cir) {
        if((Object)this instanceof net.minecraft.world.inventory.MerchantResultSlot&&!dev.rpgprogression.requirements.InteractionRequirements.canTrade(player)){cir.setReturnValue(false);return;}
        if(dev.rpgprogression.compat.DisabledIronsWeapons.blocked(((Slot)(Object)this).getItem())||InventorySlotRules.locked((Slot)(Object)this)||((Object)this instanceof CraftingResultAccess access&&!access.rpg$allowed(player)))cir.setReturnValue(false);
    }
    @Inject(method="onQuickCraft(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",at=@At("HEAD"))
    private void rpg$quickStart(ItemStack remaining,ItemStack original,CallbackInfo ci){if((Object)this instanceof FurnaceXPAccess access)access.rpg$beginQuickResult(original,original.getCount()-remaining.getCount());}
    @Inject(method="onQuickCraft(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",at=@At("TAIL"))
    private void rpg$quickEnd(ItemStack remaining,ItemStack original,CallbackInfo ci){if((Object)this instanceof FurnaceXPAccess access)access.rpg$endQuickResult();}
}
