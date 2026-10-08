package dev.rpgprogression.mixin;

import dev.rpgprogression.food.FoodManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method="causeFoodExhaustion",at=@At("HEAD"),cancellable=true)
    private void rpg$exhaustion(float value,CallbackInfo ci){ci.cancel();}
    @Inject(method="canEat",at=@At("HEAD"),cancellable=true)
    private void rpg$eatAnytime(boolean alwaysEat,CallbackInfoReturnable<Boolean> cir){cir.setReturnValue(true);}
    @Inject(method="eat",at=@At("HEAD"))
    private void rpg$foodBenefit(Level level,ItemStack stack,CallbackInfoReturnable<ItemStack> cir){if((Object)this instanceof ServerPlayer p)FoodManager.consume(p,stack);}
    @Redirect(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void rpg$noPeacefulHealing(Player player,float amount){}
}
