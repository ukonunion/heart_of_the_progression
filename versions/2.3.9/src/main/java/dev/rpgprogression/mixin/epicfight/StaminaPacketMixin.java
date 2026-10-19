package dev.rpgprogression.mixin.epicfight;

import dev.rpgprogression.compat.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.network.client.CPSetStamina;
import yesman.epicfight.network.EpicFightServerBoundPayloadHandler;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** NeoForge handlers execute on the server thread; the payload itself is an immutable record. */
@Pseudo @Mixin(value=EpicFightServerBoundPayloadHandler.class,remap=false)
public interface StaminaPacketMixin {
    @Inject(method="handleSetStamina",at=@At("HEAD"),cancellable=true)
    private static void rpg$validate(CPSetStamina message,IPayloadContext context,CallbackInfo ci){
        if(!EpicFightCompat.enabled())return;
        if(context.player() instanceof ServerPlayer p&&EpicFightCompat.shares(p)){
            var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);
            if(patch!=null)EpicFightBridge.clientRequest(patch,message.consumption(),message.resetActionTick());
        }
        ci.cancel();
    }
}
