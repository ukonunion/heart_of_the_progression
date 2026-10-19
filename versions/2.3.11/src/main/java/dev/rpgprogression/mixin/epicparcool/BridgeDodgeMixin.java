package dev.rpgprogression.mixin.epicparcool;

import dev.rpgprogression.compat.EpicFightCompat;
import dev.rpgprogression.compat.ParCoolCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/** The official bridge's Dodge animation must not send a second absolute stamina debit. */
@Pseudo
@Mixin(targets="com.yesman.epicparcool.event.ParCoolEvents",remap=false)
public abstract class BridgeDodgeMixin {
    // Exact synthetic method verified against the pinned official 21.0.0 binary.
    @Redirect(method="lambda$onSetup$8",at=@At(value="INVOKE",target="Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;setStamina(F)V"))
    private static void rpg$oneDodgePrice(PlayerPatch<?> patch,float value){
        if(!ParCoolCompat.enabled()||!EpicFightCompat.shares(patch.getOriginal()))patch.setStamina(value);
    }
}
