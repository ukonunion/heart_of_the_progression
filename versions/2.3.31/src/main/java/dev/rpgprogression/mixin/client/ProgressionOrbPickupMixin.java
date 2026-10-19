package dev.rpgprogression.mixin.client;

import dev.rpgprogression.xp.ProgressionOrb;
import net.minecraft.client.multiplayer.*;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.sounds.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Keep the native absorption animation while the RPG orb supplies its one XP pickup sound. */
@Mixin(ClientPacketListener.class)
public abstract class ProgressionOrbPickupMixin {
    @Redirect(method="handleTakeItemEntity",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void rpgprogression$pickupSound(ClientLevel level,double x,double y,double z,SoundEvent sound,SoundSource source,float volume,float pitch,boolean delayed,ClientboundTakeItemEntityPacket packet){
        if(!(level.getEntity(packet.getItemId()) instanceof ProgressionOrb))level.playLocalSound(x,y,z,sound,source,volume,pitch,delayed);
    }
}
