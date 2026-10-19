package dev.rpgprogression.mixin.irons;

import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.compat.IronsCompat;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(value=ClientMagicData.class,remap=false)
public abstract class ClientMagicDataMixin {
    @Inject(method="getPlayerMana",at=@At("HEAD"),cancellable=true)
    private static void rpg$localMana(CallbackInfoReturnable<Integer> cir){var d=ClientData.get();if(IronsCompat.enabled()&&d.manaActive)cir.setReturnValue((int)d.mana);}
}
