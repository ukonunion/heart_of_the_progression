package dev.rpgprogression.mixin.clavis;

import com.llamalad7.mixinextras.sugar.Local;
import dev.rpgprogression.requirements.ItemRequirements;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clavis consumes lockpicks in its own packet, outside the normal item-use events. */
@Pseudo
@Mixin(targets="it.hurts.shatterbyte.clavis.common.network.packet.CheckIfLockedPacket",remap=false)
public abstract class LockPickRequirementMixin {
    @Inject(method="handleServer",at=@At(value="INVOKE",target="Lit/hurts/shatterbyte/clavis/common/data/LootUtils;unlockWithQuality(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lit/hurts/shatterbyte/clavis/common/data/Lock;F)V"),cancellable=true,remap=false)
    private void rpgprogression$requireLockpick(CallbackInfo ci,@Local ServerPlayer player,@Local ItemStack stack){
        if(!ItemRequirements.allowed(player,stack)){
            player.displayClientMessage(Component.translatable("rpgprogression.requirements.blocked"),true);
            ci.cancel();
        }
    }
}
