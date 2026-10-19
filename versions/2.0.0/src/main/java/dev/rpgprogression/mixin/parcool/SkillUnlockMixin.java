package dev.rpgprogression.mixin.parcool;
import com.alrex.parcool.api.action.ActionEntry;
import com.alrex.parcool.common.action.ActionCapabilities;
import dev.rpgprogression.skill.SkillTreeManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(value=ActionCapabilities.class,remap=false)
public abstract class SkillUnlockMixin {
    @Inject(method="unlock",at=@At("HEAD"),cancellable=true)
    private void rpg$unlock(ServerPlayer p,ActionEntry<?> action,CallbackInfo ci){
        if(SkillTreeManager.parcool()){SkillTreeManager.unlock(p,action.id().toString());ci.cancel();}
    }
}
