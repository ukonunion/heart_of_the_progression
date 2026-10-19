package dev.rpgprogression.mixin.parcool;
import com.alrex.parcool.common.network.payload.StaminaPayload;
import dev.rpgprogression.compat.*;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(value=StaminaPayload.class,remap=false)
public abstract class StaminaPayloadMixin {
    @Inject(method="handleServer",at=@At("HEAD"),cancellable=true)
    private static void rpg$serverOwnsReservoir(StaminaPayload msg,IPayloadContext ctx,CallbackInfo ci){if(ParCoolCompat.enabled()){ci.cancel();ctx.enqueueWork(()->ParCoolBridge.mirror(ctx.player()));}}
}
