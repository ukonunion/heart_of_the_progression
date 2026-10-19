package dev.rpgprogression.mixin;

import dev.rpgprogression.inventory.InventorySlotRules;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Creative uses its own packet path; enforce the optional creative-lock config there too. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativeInventoryMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleSetCreativeModeSlot",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true)
    private void rpg$creative(ServerboundSetCreativeModeSlotPacket packet,CallbackInfo ci) {
        int index=packet.slotNum();
        if(index>=0&&index<player.inventoryMenu.slots.size()&&InventorySlotRules.locked(player.inventoryMenu.getSlot(index))) {
            player.inventoryMenu.sendAllDataToRemote();ci.cancel();
        }
    }
}
