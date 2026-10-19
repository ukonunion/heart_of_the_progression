package dev.rpgprogression.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FoodData remains a harmless compatibility shell; no hunger, starvation or food healing. */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Shadow private float exhaustionLevel;
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void rpg$noHunger(Player player,CallbackInfo ci){foodLevel=20;saturationLevel=0;exhaustionLevel=0;ci.cancel();}
    @Inject(method="eat(IF)V",at=@At("HEAD"),cancellable=true)
    private void rpg$noNutrition(int nutrition,float saturation,CallbackInfo ci){foodLevel=20;saturationLevel=0;exhaustionLevel=0;ci.cancel();}
    @Inject(method="addExhaustion",at=@At("HEAD"),cancellable=true)
    private void rpg$noExhaustion(float amount,CallbackInfo ci){exhaustionLevel=0;ci.cancel();}
}
