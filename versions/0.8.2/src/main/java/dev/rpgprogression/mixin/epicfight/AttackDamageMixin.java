package dev.rpgprogression.mixin.epicfight;

import dev.rpgprogression.compat.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@Pseudo @Mixin(value=AttackAnimation.class,remap=false)
public abstract class AttackDamageMixin {
    @Inject(method="hurtCollidingEntities",at=@At("HEAD"),cancellable=true)
    private void rpg$requireStamina(LivingEntityPatch<?> patch,float previous,float elapsed,EntityState before,EntityState after,AttackAnimation.Phase phase,CallbackInfo ci){
        if(patch instanceof ServerPlayerPatch&&EpicFightCompat.shares(((ServerPlayerPatch)patch).getOriginal())&&!((EpicAttackAccess)patch).rpg$attackAllowed())ci.cancel();
    }
}
