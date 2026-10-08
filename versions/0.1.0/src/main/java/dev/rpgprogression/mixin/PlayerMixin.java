package dev.rpgprogression.mixin;

import dev.rpgprogression.defense.DefenseManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Player.class)
public abstract class PlayerMixin {
    /** No Forge event exposes the food-exhaustion amount before it reaches FoodData. */
    @ModifyVariable(method="causeFoodExhaustion",at=@At("HEAD"),argsOnly=true)
    private float rpg$exhaustion(float value) {return DefenseManager.reduceFoodExhaustion((Player)(Object)this,value);}
}
