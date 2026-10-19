package dev.rpgprogression.verification.gametest;
import com.alrex.parcool.common.attachment.common.Parkourability;
import com.alrex.parcool.common.action.impl.*;
import com.alrex.parcool.common.network.payload.ActionStatePayload;
import dev.rpgprogression.compat.ParCoolSkills;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.SkillTreeManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import java.nio.ByteBuffer;
import java.util.*;
public final class ParCoolTreeGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);SkillTreeManager.refresh(p,d);var parkour=Parkourability.get(p);var run=parkour.get(FastRun.class);var nodes=ParCoolSkills.catalog(p);
        h.assertTrue(nodes.stream().anyMatch(n->n.id().equals("parcool:wall_run")&&n.parents().contains("parcool:wall_jump")),"retained prerequisite graph");
        run.start(p,parkour,ByteBuffer.allocate(128));h.assertFalse(run.isDoing(),"unlearned direct start rejected");
        ActionStatePayload.handleServer(new ActionStatePayload(p.getUUID(),List.of(new ActionStatePayload.Entry(FastRun.class,ActionStatePayload.Entry.Type.Start,new byte[0]))),TestPayloadContext.of(p));h.assertFalse(run.isDoing(),"native start packet cannot bypass learning");
        d.level=5;d.awardSkillPoints();p.experienceLevel=80;
        h.assertFalse(SkillTreeManager.unlock(p,"parcool:wall_run"),"child cannot bypass parent");h.assertTrue(d.skillPoints==1,"failed unlock spends nothing");
        h.assertTrue(SkillTreeManager.unlock(p,"parcool:fast_run"),"RPG one-click unlock succeeds");h.assertTrue(d.skillPoints==0&&p.experienceLevel==80,"RPG point spent; vanilla XP preserved");
        run.start(p,parkour,ByteBuffer.allocate(128));h.assertTrue(run.isDoing(),"native learned action works");run.finish(p);
        h.assertFalse(SkillTreeManager.unlock(p,"parcool:fast_run"),"duplicate unlock rejected");
        d.level=10;d.awardSkillPoints();h.assertTrue(SkillTreeManager.unlock(p,"parcool:wall_jump"),"parent unlock succeeds");d.level=15;d.awardSkillPoints();h.assertTrue(SkillTreeManager.unlock(p,"parcool:wall_run"),"child unlock succeeds");
        var saved=d.save();d.load(saved);SkillTreeManager.refresh(p,d);h.assertTrue(d.skillPoints==0&&d.unlockedSkills.contains("parcool:wall_run"),"saved unlock restored");
        d.unlockedSkills.add("parcool:castaway");SkillTreeManager.refresh(p,d);h.assertTrue(d.skillPoints==1,"removed external action refunds point");SkillTreeManager.refresh(p,d);h.assertTrue(d.skillPoints==1,"refund occurs once");
        var spoof=new ActionStatePayload(UUID.randomUUID(),List.of(new ActionStatePayload.Entry(FastRun.class,ActionStatePayload.Entry.Type.Start,new byte[0])));ActionStatePayload.handleServer(spoof,TestPayloadContext.of(p));h.assertFalse(run.isDoing(),"other-player UUID rejected");
        com.mojang.logging.LogUtils.getLogger().info("RPG_TREE_PARCOOL3 nodes={}",nodes.size());
    }
}
