package dev.rpgprogression.gametest.client;
import net.minecraft.server.level.ServerPlayer;
import dev.rpgprogression.progression.PlayerProgression;
import java.util.function.BiConsumer;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
public final class EpicTreeClientCheck {
    public static void prepareFixture(ServerPlayer p){
        var cap=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class).getSkillCapability();
        cap.listAcquiredSkills().filter(s->s.getCategory().learnable()).toList().forEach(cap::removeLearnedSkill);
        cap.listSkillContainers().filter(c->c.getSlot().category().learnable()).forEach(c->{
            c.setSkill(null);c.setReplaceCooldown(0);
            yesman.epicfight.network.EpicFightNetworkManager.sendToPlayer(c.createSyncPacketToLocalPlayer(),p);
        });
        yesman.epicfight.world.gamerule.EpicFightGameRules.SKILL_REPLACE_COOLDOWN.setRuleValue(p.level(),40);
    }
    public static void blockEquipForFixture(ServerPlayer p,String id){
        var cap=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class).getSkillCapability();
        var container=cap.getFirstEmptyContainer(SkillManager.getSkill(id).getCategory());
        if(container==null)throw new IllegalStateException("Fixture needs an empty native skill slot");
        container.setReplaceCooldown(40);
        dev.rpgprogression.skill.SkillTreeManager.sync(p);
    }
    public static void equipped(ServerPlayer p,String id,BiConsumer<Boolean,String> check){var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);check.accept(patch.getSkillCapability().isEquipping(SkillManager.getSkill(id)),"real Epic native skill equipped from our tree");}
    public static void learned(ServerPlayer p,java.util.Set<String> ids,BiConsumer<Boolean,String> check){var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);for(var id:ids)if(id.startsWith("epicfight:"))check.accept(patch.getSkillCapability().hasLearned(SkillManager.getSkill(id)),"native Epic skill persists: "+id);}
    private EpicTreeClientCheck(){}
}
