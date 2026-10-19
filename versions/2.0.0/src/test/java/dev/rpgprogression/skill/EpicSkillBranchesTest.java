package dev.rpgprogression.skill;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EpicSkillBranchesTest {
    @Test void nativeRequirementsHavePriority(){
        assertEquals(List.of("addon:original_parent"),EpicSkillBranches.parents("epicfight:step",List.of("addon:original_parent"),Set.of("epicfight:roll")));
    }
    @Test void supplementaryBranchesHaveRealForks(){
        var known=Set.of("epicfight:roll","epicfight:step","epicfight:endurance","epicfight:stamina_pillager");
        assertEquals(List.of("epicfight:roll"),EpicSkillBranches.parents("epicfight:step",List.of(),known));
        assertEquals(List.of("epicfight:step"),EpicSkillBranches.parents("epicfight:technician",List.of(),known));
        assertEquals(List.of("epicfight:step"),EpicSkillBranches.parents("epicfight:emergency_escape",List.of(),known));
        assertEquals(List.of("epicfight:stamina_pillager"),EpicSkillBranches.parents("epicfight:death_harvest",List.of(),known));
        assertEquals(List.of("epicfight:stamina_pillager"),EpicSkillBranches.parents("epicfight:adrenaline_fiend",List.of(),known));
    }
    @Test void unregisteredPrerequisiteCannotLockAnAddonSkill(){
        assertTrue(EpicSkillBranches.parents("epicfight:step",List.of(),Set.of()).isEmpty());
        assertTrue(EpicSkillBranches.parents("addon:new_skill",List.of(),Set.of("epicfight:roll")).isEmpty());
    }
}
