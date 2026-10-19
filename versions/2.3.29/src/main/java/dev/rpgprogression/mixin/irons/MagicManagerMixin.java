package dev.rpgprogression.mixin.irons;

import dev.rpgprogression.compat.IronsCompat;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(value=MagicManager.class,remap=false)
public abstract class MagicManagerMixin {
    @Inject(method="regenPlayerMana",at=@At("HEAD"),cancellable=true)
    private void rpg$oneRecovery(ServerPlayer p,MagicData data,CallbackInfoReturnable<Boolean> cir){if(IronsCompat.shares(p))cir.setReturnValue(false);}
}
