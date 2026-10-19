package dev.rpgprogression.mixin.parcool;
import dev.rpgprogression.compat.*;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo @Mixin(targets="com.alrex.parcool.common.attachment.client.LocalStamina",remap=false)
public abstract class LocalStaminaMixin {
    @Inject(method={"consume","recover"},at=@At("HEAD"),cancellable=true)
    private void rpg$noParallelDebit(LocalPlayer p,int amount,CallbackInfo ci){if(ParCoolCompat.enabled()){ParCoolBridge.mirror(p);ci.cancel();}}
    @Inject(method={"onTick","sync"},at=@At("HEAD"),cancellable=true)
    private void rpg$noParallelRegen(LocalPlayer p,CallbackInfo ci){if(ParCoolCompat.enabled()){ParCoolBridge.mirror(p);ci.cancel();}}
    @Inject(method="shouldShowHUD",at=@At("HEAD"),cancellable=true)
    private void rpg$hide(LocalPlayer p,CallbackInfoReturnable<Boolean> cir){if(ParCoolCompat.enabled())cir.setReturnValue(false);}
}
