package dev.rpgprogression.mixin.epicfight;

import dev.rpgprogression.compat.*;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@Pseudo @Mixin(value=LivingEntityPatch.class,remap=false)
public abstract class AttackStrikeMixin implements EpicAttackAccess {
    @Unique private boolean rpg$allowed=true;
    public boolean rpg$attackAllowed(){return rpg$allowed;}
    @Inject(method="onStrike",at=@At("HEAD"),cancellable=true)
    private void rpg$weaponSpend(AttackAnimation animation,InteractionHand hand,CallbackInfo ci){
        if((Object)this instanceof ServerPlayerPatch patch&&DisabledIronsWeapons.blocked(patch.getOriginal().getItemInHand(hand))){rpg$allowed=false;ci.cancel();return;}
        if((Object)this instanceof ServerPlayerPatch patch&&EpicFightCompat.shares(patch.getOriginal())){
            rpg$allowed=EpicFightBridge.strike(patch,hand);
            if(!rpg$allowed)ci.cancel();
        }else rpg$allowed=true;
    }
}
