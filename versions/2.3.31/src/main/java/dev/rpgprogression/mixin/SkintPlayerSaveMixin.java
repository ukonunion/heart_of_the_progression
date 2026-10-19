package dev.rpgprogression.mixin;

import dev.rpgprogression.skint.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class SkintPlayerSaveMixin {
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void rpg$saveStandingAtSkint(CompoundTag tag,CallbackInfo ci){
        var player=(ServerPlayer)(Object)this;if(!(player.getVehicle() instanceof SkintSeat seat))return;
        var location=SkintSeating.standLocation(player,seat.position());var pos=new ListTag();
        pos.add(DoubleTag.valueOf(location.x));pos.add(DoubleTag.valueOf(location.y));pos.add(DoubleTag.valueOf(location.z));
        tag.put("Pos",pos);tag.remove("RootVehicle");tag.putFloat("FallDistance",0);
    }
}
