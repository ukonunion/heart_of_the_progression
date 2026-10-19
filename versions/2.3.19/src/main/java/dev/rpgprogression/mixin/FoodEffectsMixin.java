package dev.rpgprogression.mixin;

import dev.rpgprogression.food.FoodManager;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.food.FoodProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class FoodEffectsMixin {
    @Unique private boolean rpg$replaceAppleEffects;
    @Inject(method="eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",at=@At("HEAD"))
    private void rpg$appleContext(Level level,ItemStack stack,FoodProperties food,CallbackInfoReturnable<ItemStack> cir){rpg$replaceAppleEffects=(Object)this instanceof Player&&FoodManager.golden(stack);}
    @Inject(method="addEatEffect",at=@At("HEAD"),cancellable=true)
    private void rpg$appleReplacement(CallbackInfo ci){if(rpg$replaceAppleEffects)ci.cancel();}
    @Inject(method="eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",at=@At("RETURN"))
    private void rpg$clearAppleContext(Level level,ItemStack stack,FoodProperties food,CallbackInfoReturnable<ItemStack> cir){rpg$replaceAppleEffects=false;}
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
    private void rpg$noHungerEffect(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> cir){if((Object)this instanceof Player&&effect.getEffect().equals(MobEffects.HUNGER))cir.setReturnValue(false);}
}
