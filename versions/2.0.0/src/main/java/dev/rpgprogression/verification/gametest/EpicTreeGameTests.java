package dev.rpgprogression.verification.gametest;
import dev.rpgprogression.compat.EpicFightSkills;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import yesman.epicfight.registry.EpicFightRegistries;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
public final class EpicTreeGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);SkillTreeManager.refresh(p,d);var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);var cap=patch.getPlayerSkills();
        var nodes=EpicFightSkills.catalog();h.assertTrue(nodes.size()==EpicFightRegistries.SKILL.stream().filter(s->s.getCategory().learnable()).count(),"all native book skills represented");
        var graph=SkillTreeLayout.arrange(nodes.stream().map(n->new SkillTreeLayout.Entry(n.id(),n.parents())).toList());
        for(var n:nodes){
            var prior=EpicFightRegistries.SKILL.get(ResourceLocation.parse(n.id())).getPriorSkill();
            if(prior!=null&&prior.getCategory().learnable())h.assertTrue(n.parents().equals(java.util.List.of(prior.getRegistryName().toString())),"native Epic prerequisites unchanged");
            for(var parent:n.parents())h.assertTrue(graph.positions().containsKey(parent)&&graph.positions().get(parent).depth()<graph.positions().get(n.id()).depth(),"complete acyclic Epic branches");
        }
        d.skillPoints=1;
        h.assertFalse(SkillTreeManager.unlock(p,"epicfight:step"),"new Epic prerequisite enforced by the real server unlock path");
        h.assertTrue(d.skillPoints==1&&!cap.hasLearned(EpicFightRegistries.SKILL.get(ResourceLocation.parse("epicfight:step"))),"blocked child cannot charge or learn natively");d.skillPoints=0;
        var node=nodes.stream().filter(n->n.parents().isEmpty()&&!d.unlockedSkills.contains(n.id())).findFirst().orElseThrow();var skill=EpicFightRegistries.SKILL.get(ResourceLocation.parse(node.id()));
        var book=new ItemStack(yesman.epicfight.registry.entries.EpicFightItems.SKILLBOOK.get());yesman.epicfight.world.item.SkillBookItem.setContainingSkill(skill.holder(),book);p.setItemInHand(InteractionHand.MAIN_HAND,book);
        h.assertTrue(book.use(p.level(),p,InteractionHand.MAIN_HAND).getResult()==InteractionResult.FAIL,"old native skill book use disabled");
        var slot=cap.getSkillContainersFor(skill.getCategory()).iterator().next().getSlot();
        var payloadContext=TestPayloadContext.of(p);
        yesman.epicfight.network.EpicFightServerBoundPayloadHandler.handleChangeSkill(new yesman.epicfight.network.client.CPChangeSkill(slot,skill.holder(),-1),payloadContext);
        yesman.epicfight.network.EpicFightServerBoundPayloadHandler.handleChangeSkill(new yesman.epicfight.network.client.CPChangeSkill(slot,skill.holder(),0),payloadContext);
        h.assertFalse(cap.hasLearned(skill),"forged native book/equip packet cannot unlock skills");h.assertTrue(p.getMainHandItem().getCount()==1,"blocked book not consumed");
        d.level=5;d.awardSkillPoints();h.assertTrue(SkillTreeManager.unlock(p,node.id()),"RPG skill point unlocks Epic skill");
        h.assertTrue(d.skillPoints==0&&cap.hasLearned(skill),"real native learned list updated");h.assertFalse(SkillTreeManager.unlock(p,node.id()),"duplicate Epic unlock rejected");
        if(node.id().equals("epicfight:roll")){d.skillPoints=1;h.assertTrue(SkillTreeManager.unlock(p,"epicfight:step")&&d.skillPoints==0&&cap.hasLearned(EpicFightRegistries.SKILL.get(ResourceLocation.parse("epicfight:step"))),"child unlocks only after its predecessor");}
        h.assertTrue(EpicFightSkills.nativeChangeAllowed(patch,slot,-1,skill),"learned skill allowed in native equipment editor");
        h.assertFalse(EpicFightSkills.nativeChangeAllowed(patch,slot,0,skill),"books disabled even for already known skills");
        var loot=it.unimi.dsi.fastutil.objects.ObjectArrayList.of(book.copy(),new ItemStack(Items.DIAMOND));
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(p.serverLevel()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY);
        var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(java.util.Optional.empty());
        var result=net.neoforged.neoforge.common.CommonHooks.modifyLoot(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("rpgprogression","test_books"),loot,context);
        h.assertTrue(result.size()==1&&result.get(0).is(Items.DIAMOND),"final global loot path removes books and preserves other items");
        int slotId=slot.universalOrdinal();h.assertFalse(EpicFightSkills.equip(p,node.id(),99999),"invalid equip slot rejected");
        var saved=d.save();d.load(saved);SkillTreeManager.refresh(p,d);h.assertTrue(cap.hasLearned(skill)&&d.skillPoints==0,"native learned skill persists without charging twice");
        com.mojang.logging.LogUtils.getLogger().info("RPG_TREE_EPIC nodes={}, learned={}, slot={}",nodes.size(),node.id(),slotId);
    }
    private EpicTreeGameTests(){}
}
