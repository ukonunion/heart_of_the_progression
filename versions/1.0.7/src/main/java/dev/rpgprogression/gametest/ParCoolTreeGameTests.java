package dev.rpgprogression.gametest;
import com.alrex.parcool.common.Parkourability;
import com.alrex.parcool.common.action.ParCoolActions;
import dev.rpgprogression.compat.ParCoolSkills;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.SkillTreeManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
public final class ParCoolTreeGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);SkillTreeManager.refresh(p,d);var parkour=Parkourability.get(p);
        var nodes=ParCoolSkills.catalog(p);h.assertTrue(nodes.stream().anyMatch(n->n.id().equals("parcool:wall_run")&&n.parents().contains("parcool:wall_jump")),"native tree dependency imported");
        parkour.get(ParCoolActions.FAST_RUN).start();h.assertFalse(parkour.get(ParCoolActions.FAST_RUN).isDoing(),"unlearned action blocked even when start called directly");
        d.level=5;d.awardSkillPoints();p.experienceLevel=80;int vanilla=p.experienceLevel;
        h.assertFalse(SkillTreeManager.unlock(p,"parcool:wall_run"),"dependent action cannot bypass parent");h.assertTrue(d.skillPoints==1,"failed prerequisite never charges");
        parkour.getCapabilities().unlock(p,ParCoolActions.FAST_RUN);
        h.assertTrue(d.skillPoints==0&&d.unlockedSkills.contains("parcool:fast_run")&&p.experienceLevel==vanilla,"native unlock packet path uses RPG points, not vanilla XP");
        parkour.get(ParCoolActions.FAST_RUN).start();h.assertTrue(parkour.get(ParCoolActions.FAST_RUN).isDoing(),"learned action really starts");parkour.get(ParCoolActions.FAST_RUN).finish();
        h.assertFalse(SkillTreeManager.unlock(p,"parcool:fast_run"),"duplicate rejected");
        d.level=10;d.awardSkillPoints();h.assertTrue(SkillTreeManager.unlock(p,"parcool:wall_jump"),"parent unlock valid");
        d.level=15;d.awardSkillPoints();h.assertTrue(SkillTreeManager.unlock(p,"parcool:wall_run"),"child unlock valid after parent");
        h.assertTrue(parkour.getCapabilities().can(ParCoolActions.WALL_RUN)&&d.skillPoints==0,"native capability follows tree");
        var saved=d.save();d.load(saved);SkillTreeManager.refresh(p,d);h.assertTrue(d.skillPoints==0&&parkour.getCapabilities().can(ParCoolActions.WALL_RUN),"relogin preserves parkour unlocks");
        com.mojang.logging.LogUtils.getLogger().info("RPG_TREE_PARCOOL nodes={}",nodes.size());
    }
    private ParCoolTreeGameTests(){}
}
