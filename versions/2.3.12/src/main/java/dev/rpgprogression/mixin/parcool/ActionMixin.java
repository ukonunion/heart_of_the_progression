package dev.rpgprogression.mixin.parcool;
import com.alrex.parcool.common.action.*;
import com.alrex.parcool.common.attachment.common.Parkourability;
import dev.rpgprogression.compat.ParCoolBridge;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.nio.ByteBuffer;
@Pseudo @Mixin(value=Action.class,remap=false)
public abstract class ActionMixin {
    @Shadow public abstract boolean isDoing();
    @Shadow public abstract StaminaConsumeTiming getStaminaConsumeTiming();
    @Inject(method="start",at=@At("HEAD"),cancellable=true)
    private void rpg$gate(Player p,Parkourability parkour,ByteBuffer info,CallbackInfo ci){if(isDoing()||ParCoolBridge.blocked(p,(Action)(Object)this))ci.cancel();}
    @Inject(method="start",at=@At("TAIL"))
    private void rpg$debit(Player p,Parkourability parkour,ByteBuffer info,CallbackInfo ci){if(getStaminaConsumeTiming()==StaminaConsumeTiming.OnStart&&!((Object)this instanceof com.alrex.parcool.common.action.impl.ChargeJump))ParCoolBridge.spend(p,(Action)(Object)this);}
    @Inject(method="finish",at=@At("HEAD"))
    private void rpg$chargedJump(Player p,CallbackInfo ci){if(isDoing()&&(Object)this instanceof com.alrex.parcool.common.action.impl.ChargeJump)ParCoolBridge.spend(p,(Action)(Object)this);}
}
