package dev.rpgprogression.mixin;

import dev.rpgprogression.food.FoodManager;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class FoodEffectsMixin {
    @Inject(method="addEatEffect",at=@At("HEAD"),cancellable=true)
    private void rpg$appleReplacement(ItemStack stack,Level level,LivingEntity eater,CallbackInfo ci){if(eater instanceof Player&&FoodManager.golden(stack))ci.cancel();}
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
    private void rpg$noHungerEffect(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> cir){if((Object)this instanceof Player&&effect.getEffect()==MobEffects.HUNGER)cir.setReturnValue(false);}
}
