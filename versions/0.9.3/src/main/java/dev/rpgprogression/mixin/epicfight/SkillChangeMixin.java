package dev.rpgprogression.mixin.epicfight;
import dev.rpgprogression.compat.EpicFightSkills;
import dev.rpgprogression.skill.SkillTreeManager;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(value=CPChangeSkill.class,remap=false)
public abstract class SkillChangeMixin {
    @Inject(method="lambda$handle$0",at=@At("HEAD"),cancellable=true)
    private static void rpg$validate(CPChangeSkill packet,ServerPlayerPatch patch,CallbackInfo ci){
        var fields=(SkillPacketAccessor)(Object)packet;
        if(SkillTreeManager.epic()&&!EpicFightSkills.nativeChangeAllowed(patch,fields.rpg$slot(),fields.rpg$bookSlot(),fields.rpg$skill()))ci.cancel();
    }
}
