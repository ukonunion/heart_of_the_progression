package dev.rpgprogression.verification.gametest;
import com.alrex.parcool.common.attachment.common.Parkourability;
import com.alrex.parcool.common.action.impl.*;
import com.alrex.parcool.common.network.payload.ActionStatePayload;
import dev.rpgprogression.compat.ParCoolSkills;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import java.nio.ByteBuffer;
import java.util.*;
public final class ParCoolTreeGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);SkillTreeManager.refresh(p,d);var parkour=Parkourability.get(p);var run=parkour.get(FastRun.class);var nodes=SkillTreeManager.catalog(p);
        h.assertTrue(nodes.stream().anyMatch(n->n.id().equals("parcool:wall_run")&&n.parents().equals(List.of("parcool:horizontal_wall_run","parcool:wall_jump"))),"new wall-run shared prerequisites");
        for(String root:List.of("parcool:crawl","parcool:fast_run","parcool:charge_jump","parcool:hang_on","parcool:skydive"))
            h.assertTrue(nodes.stream().anyMatch(n->n.id().equals(root)&&n.parents().isEmpty()),"independent ParCool starting skill: "+root);
        run.start(p,parkour,ByteBuffer.allocate(128));h.assertFalse(run.isDoing(),"unlearned direct start rejected");
        ActionStatePayload.handleServer(new ActionStatePayload(p.getUUID(),List.of(new ActionStatePayload.Entry(FastRun.class,ActionStatePayload.Entry.Type.Start,new byte[0]))),TestPayloadContext.of(p));h.assertFalse(run.isDoing(),"native start packet cannot bypass learning");
        d.level=5;d.awardSkillPoints();p.experienceLevel=80;
        h.assertFalse(SkillTreeManager.unlock(p,"parcool:wall_run"),"child cannot bypass parent");h.assertTrue(d.skillPoints==1,"failed unlock spends nothing");
        h.assertTrue(SkillTreeManager.unlock(p,"parcool:fast_run"),"RPG one-click unlock succeeds");h.assertTrue(d.skillPoints==0&&p.experienceLevel==80,"RPG point spent; vanilla XP preserved");
        run.start(p,parkour,ByteBuffer.allocate(128));h.assertTrue(run.isDoing(),"native learned action works");run.finish(p);
        h.assertFalse(SkillTreeManager.unlock(p,"parcool:fast_run"),"duplicate unlock rejected");
        d.level=10;d.awardSkillPoints();h.assertTrue(SkillTreeManager.unlock(p,"parcool:charge_jump"),"charged jump is independently unlockable");
        d.level=15;d.awardSkillPoints();h.assertTrue(SkillTreeManager.unlock(p,"parcool:wall_jump"),"wall jump follows charged jump");
        d.level=20;d.awardSkillPoints();h.assertFalse(SkillTreeManager.unlock(p,"parcool:wall_run"),"wall run requires both parents");
        h.assertTrue(SkillTreeManager.unlock(p,"parcool:horizontal_wall_run"),"horizontal wall run follows fast run");
        d.level=25;d.awardSkillPoints();h.assertTrue(SkillTreeManager.unlock(p,"parcool:wall_run"),"both wall-run prerequisites allow purchase");
        var saved=d.save();d.load(saved);SkillTreeManager.refresh(p,d);h.assertTrue(d.skillPoints==0&&d.unlockedSkills.contains("parcool:wall_run"),"saved unlock restored");
        d.unlockedSkills.add("parcool:castaway");SkillTreeManager.refresh(p,d);h.assertTrue(d.skillPoints==1,"removed external action refunds point");SkillTreeManager.refresh(p,d);h.assertTrue(d.skillPoints==1,"refund occurs once");
        d.skillPoints=3;h.assertFalse(SkillTreeManager.unlock(p,DodgeSkillGroup.ID),"combined dodge requires dive");
        h.assertTrue(SkillTreeManager.unlock(p,"parcool:crawl"),"crawl root purchase succeeds");h.assertTrue(SkillTreeManager.unlock(p,"parcool:dive"),"dive requires crawl and fast run");
        h.assertTrue(SkillTreeManager.unlock(p,DodgeSkillGroup.ID)&&d.skillPoints==0,"combined dodge costs exactly one point");
        h.assertTrue(d.unlockedSkills.containsAll(DodgeSkillGroup.members(true,SkillTreeManager.epic())),"combined purchase grants every installed native dodge");
        h.assertFalse(SkillTreeManager.unlock(p,"parcool:dodge"),"hidden native action cannot be purchased a second time");
        d.load(d.save());SkillTreeManager.refresh(p,d);h.assertTrue(d.skillPoints==0&&d.unlockedSkills.contains(DodgeSkillGroup.ID),"combined purchase persists without repeat charge");
        var spoof=new ActionStatePayload(UUID.randomUUID(),List.of(new ActionStatePayload.Entry(FastRun.class,ActionStatePayload.Entry.Type.Start,new byte[0])));ActionStatePayload.handleServer(spoof,TestPayloadContext.of(p));h.assertFalse(run.isDoing(),"other-player UUID rejected");
        com.mojang.logging.LogUtils.getLogger().info("RPG_TREE_PARCOOL3 nodes={}",nodes.size());
    }
}
