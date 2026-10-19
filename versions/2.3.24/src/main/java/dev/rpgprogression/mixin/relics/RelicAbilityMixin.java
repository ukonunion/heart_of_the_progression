package dev.rpgprogression.mixin.relics;

import dev.rpgprogression.requirements.ItemRequirements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo @Mixin(targets="it.hurts.sskirillss.relics.items.relics.base.IRelicItem",remap=false)
public interface RelicAbilityMixin {
    @Inject(method="canPlayerUseAbility",at=@At("HEAD"),cancellable=true)
    private void rpg$ability(Player player,ItemStack stack,String ability,CallbackInfoReturnable<Boolean> ci){if(!ItemRequirements.allowed(player,stack))ci.setReturnValue(false);}
    @Inject(method="castActiveAbility",at=@At("HEAD"),cancellable=true)
    private void rpg$cast(ItemStack stack,Player player,String ability,@Coerce Object type,@Coerce Object stage,CallbackInfo ci){if(!ItemRequirements.allowed(player,stack))ci.cancel();}
}
