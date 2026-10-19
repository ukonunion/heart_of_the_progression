package dev.rpgprogression.mixin.parcool;
import com.alrex.parcool.common.action.*;
import com.alrex.parcool.common.attachment.common.Parkourability;
import dev.rpgprogression.compat.ParCoolBridge;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.LinkedList;
@Pseudo @Mixin(value=ActionProcessor.class,remap=false)
public abstract class ActionProcessorMixin {
    @Inject(method="processAction",at=@At("HEAD"))
    private void rpg$mirror(Player p,Parkourability parkour,LinkedList<?> states,boolean client,Action action,CallbackInfo ci){ParCoolBridge.mirror(p);}
    @Inject(method="processAction",at=@At("RETURN"))
    private void rpg$working(Player p,Parkourability parkour,LinkedList<?> states,boolean client,Action action,CallbackInfo ci){ParCoolBridge.working(p,action);}
}
