package dev.rpgprogression.mixin.epicfight;

import dev.rpgprogression.compat.*;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.network.client.CPSetStamina;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import java.util.function.Supplier;

@Pseudo @Mixin(value=CPSetStamina.class,remap=false)
public abstract class StaminaPacketMixin implements StaminaPacketAccess {
    @Shadow @Final private float consumption;
    @Shadow @Final private boolean resetActionTick;
    @Unique public float rpg$requestedStamina(){return consumption;}
    @Unique public boolean rpg$resetAction(){return resetActionTick;}
    @Inject(method="handle",at=@At("HEAD"),cancellable=true)
    private static void rpg$validate(CPSetStamina message,Supplier<NetworkEvent.Context> supplier,CallbackInfo ci){
        if(!EpicFightCompat.enabled())return;
        var ctx=supplier.get();var packet=(StaminaPacketAccess)(Object)message;
        float value=packet.rpg$requestedStamina();boolean reset=packet.rpg$resetAction();
        ctx.enqueueWork(()->{
            var p=ctx.getSender();if(!EpicFightCompat.shares(p))return;
            var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);
            if(patch!=null)EpicFightBridge.clientRequest(patch,value,reset);
        });
        ctx.setPacketHandled(true);ci.cancel();
    }
}
