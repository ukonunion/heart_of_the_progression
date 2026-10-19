package dev.rpgprogression.mixin.parcool;

import com.alrex.parcool.api.stamina.IReadableStamina;
import com.alrex.parcool.common.Parkourability;
import dev.rpgprogression.compat.*;
import dev.rpgprogression.progression.PlayerProgressionProvider;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value=Parkourability.class,remap=false)
public abstract class ParkourabilityMixin {
    @Shadow @Final private Player player;
    @Unique private IReadableStamina rpg$stamina;
    @Inject(method="permit",at=@At("RETURN"),cancellable=true)
    private void rpg$learned(com.alrex.parcool.api.action.ActionEntry<?> entry,CallbackInfoReturnable<Boolean> cir){
        if(dev.rpgprogression.skill.SkillTreeManager.parcool()&&entry.option().needLearning()
                &&(!player.level().isClientSide||player.isLocalPlayer()))
            PlayerProgressionProvider.get(player).ifPresent(d->{if(!d.unlockedSkills.contains(entry.id().toString()))cir.setReturnValue(false);});
    }
    @Inject(method="getStamina",at=@At("HEAD"),cancellable=true)
    private void rpg$sharedStamina(CallbackInfoReturnable<IReadableStamina> cir){
        if(ParCoolCompat.enabled()&&(!player.level().isClientSide||player.isLocalPlayer())
                &&PlayerProgressionProvider.get(player).isPresent()){
            if(rpg$stamina==null)rpg$stamina=ParCoolBridge.view(player);
            cir.setReturnValue(rpg$stamina);
        }
    }
}
