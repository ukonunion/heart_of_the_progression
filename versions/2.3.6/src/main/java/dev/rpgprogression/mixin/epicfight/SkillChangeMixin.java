package dev.rpgprogression.mixin.epicfight;
import dev.rpgprogression.compat.EpicFightSkills;
import dev.rpgprogression.skill.SkillTreeManager;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import yesman.epicfight.network.EpicFightServerBoundPayloadHandler;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(value=EpicFightServerBoundPayloadHandler.class,remap=false)
public interface SkillChangeMixin {
    @Inject(method="handleChangeSkill",at=@At("HEAD"),cancellable=true)
    private static void rpg$validate(CPChangeSkill packet,IPayloadContext context,CallbackInfo ci){
        if(!SkillTreeManager.epic())return;
        var patch=EpicFightCapabilities.getEntityPatch(context.player(),ServerPlayerPatch.class);
        if(patch==null||!EpicFightSkills.nativeChangeAllowed(patch,packet.skillSlot(),packet.skillBookSlotIndex(),Skill.skillOrNull(packet.skill())))ci.cancel();
    }
}
