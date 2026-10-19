package dev.rpgprogression.gametest;
import dev.rpgprogression.compat.EpicFightSkills;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
public final class EpicTreeGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);SkillTreeManager.refresh(p,d);var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);var cap=patch.getSkillCapability();
        var nodes=EpicFightSkills.catalog();h.assertTrue(nodes.size()==SkillManager.getSkills(s->s.getCategory().learnable()).size(),"all native book skills represented");
        var graph=SkillTreeLayout.arrange(nodes.stream().map(n->new SkillTreeLayout.Entry(n.id(),n.parents())).toList());
        for(var n:nodes){
            var prior=SkillManager.getSkill(n.id()).getPriorSkill();
            if(prior!=null&&prior.getCategory().learnable())h.assertTrue(n.parents().equals(java.util.List.of(prior.getRegistryName().toString())),"native Epic prerequisites unchanged");
            for(var parent:n.parents())h.assertTrue(graph.positions().containsKey(parent)&&graph.positions().get(parent).depth()<graph.positions().get(n.id()).depth(),"complete acyclic Epic branches");
        }
        d.skillPoints=1;
        h.assertFalse(SkillTreeManager.unlock(p,"epicfight:step"),"new Epic prerequisite enforced by the real server unlock path");
        h.assertTrue(d.skillPoints==1&&!cap.hasLearned(SkillManager.getSkill("epicfight:step")),"blocked child cannot charge or learn natively");d.skillPoints=0;
        var node=nodes.stream().filter(n->n.parents().isEmpty()&&!d.unlockedSkills.contains(n.id())).findFirst().orElseThrow();var skill=SkillManager.getSkill(node.id());
        var book=new ItemStack(yesman.epicfight.world.item.EpicFightItems.SKILLBOOK.get());yesman.epicfight.world.item.SkillBookItem.setContainingSkill(skill,book);p.setItemInHand(InteractionHand.MAIN_HAND,book);
        h.assertTrue(book.use(p.level(),p,InteractionHand.MAIN_HAND).getResult()==InteractionResult.FAIL,"old native skill book use disabled");
        var slot=cap.getSkillContainersFor(skill.getCategory()).iterator().next().getSlot();
        try{
            var method=yesman.epicfight.network.client.CPChangeSkill.class.getDeclaredMethod("lambda$handle$0",yesman.epicfight.network.client.CPChangeSkill.class,ServerPlayerPatch.class);method.setAccessible(true);
            method.invoke(null,new yesman.epicfight.network.client.CPChangeSkill(slot,-1,skill),patch);
            method.invoke(null,new yesman.epicfight.network.client.CPChangeSkill(slot,0,skill),patch);
        }catch(ReflectiveOperationException error){throw new RuntimeException(error);}
        h.assertFalse(cap.hasLearned(skill),"forged native book/equip packet cannot unlock skills");h.assertTrue(p.getMainHandItem().getCount()==1,"blocked book not consumed");
        d.level=5;d.awardSkillPoints();h.assertTrue(SkillTreeManager.unlock(p,node.id()),"RPG skill point unlocks Epic skill");
        h.assertTrue(d.skillPoints==0&&cap.hasLearned(skill),"real native learned list updated");h.assertFalse(SkillTreeManager.unlock(p,node.id()),"duplicate Epic unlock rejected");
        if(node.id().equals("epicfight:roll")){d.skillPoints=1;h.assertTrue(SkillTreeManager.unlock(p,"epicfight:step")&&d.skillPoints==0&&cap.hasLearned(SkillManager.getSkill("epicfight:step")),"child unlocks only after its predecessor");}
        h.assertTrue(EpicFightSkills.nativeChangeAllowed(patch,slot,-1,skill),"learned skill allowed in native equipment editor");
        h.assertFalse(EpicFightSkills.nativeChangeAllowed(patch,slot,0,skill),"books disabled even for already known skills");
        var loot=it.unimi.dsi.fastutil.objects.ObjectArrayList.of(book.copy(),new ItemStack(Items.DIAMOND));
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(p.serverLevel()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY);
        var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(null);
        var result=net.minecraftforge.common.ForgeHooks.modifyLoot(new net.minecraft.resources.ResourceLocation("rpgprogression","test_books"),loot,context);
        h.assertTrue(result.size()==1&&result.get(0).is(Items.DIAMOND),"final global loot path removes books and preserves other items");
        int slotId=slot.universalOrdinal();h.assertFalse(EpicFightSkills.equip(p,node.id(),99999),"invalid equip slot rejected");
        var saved=d.save();d.load(saved);SkillTreeManager.refresh(p,d);h.assertTrue(cap.hasLearned(skill)&&d.skillPoints==0,"native learned skill persists without charging twice");
        com.mojang.logging.LogUtils.getLogger().info("RPG_TREE_EPIC nodes={}, learned={}, slot={}",nodes.size(),node.id(),slotId);
    }
    private EpicTreeGameTests(){}
}
