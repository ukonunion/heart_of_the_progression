package dev.rpgprogression.mixin;

import dev.rpgprogression.requirements.InteractionRequirements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantMenu.class)
public abstract class MerchantRequirementsMixin {
    @Inject(method="stillValid",at=@At("HEAD"),cancellable=true)
    private void rpg$valid(Player player,CallbackInfoReturnable<Boolean> ci){if(!InteractionRequirements.canTrade(player))ci.setReturnValue(false);}
    @Inject(method="quickMoveStack",at=@At("HEAD"),cancellable=true)
    private void rpg$quick(Player player,int slot,CallbackInfoReturnable<ItemStack> ci){if(!InteractionRequirements.canTrade(player))ci.setReturnValue(ItemStack.EMPTY);}
}
