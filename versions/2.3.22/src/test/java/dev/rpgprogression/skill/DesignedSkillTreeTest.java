package dev.rpgprogression.skill;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DesignedSkillTreeTest {
    private static Set<String> installed(){
        var result=new HashSet<>(DesignedSkillTree.ROOTS);
        result.addAll(List.of("parcool:hide_in_block","parcool:slide","parcool:dive","parcool:fast_swim","parcool:trick_jump","parcool:vault","parcool:wall_jump",
                "parcool:horizontal_wall_run","parcool:wall_run","parcool:hang_down","parcool:ride_zipline","parcool:pole_climb","parcool:slide_down","parcool:climb_up","parcool:breakfall","parcool:long_jump",
                DodgeSkillGroup.ID,"epicfight:emergency_escape","epicfight:technician","epicfight:phantom_ascent","epicfight:demolition_leap","epicfight:meteor_slam",
                "epicfight:adaptive_skin","epicfight:vengeance","epicfight:guard","epicfight:parrying","epicfight:stamina_pillager","epicfight:hypervitality","epicfight:adrenaline_fiend",
                "epicfight:forbidden_strength","epicfight:death_harvest","epicfight:catharsis","epicfight:bonebreaker","epicfight:berserker"));return result;
    }
    @Test void tenRootsIncludeIndependentLongJump(){
        var known=installed();
        var roots=known.stream().filter(id->DesignedSkillTree.parents(id,List.of("epicfight:guard"),known).isEmpty()).collect(java.util.stream.Collectors.toSet());
        assertEquals(DesignedSkillTree.ROOTS,roots);assertEquals(10,roots.size());
        assertEquals(List.of(),DesignedSkillTree.parents("parcool:long_jump",List.of("parcool:fast_run"),known));
    }
    @Test void graphIsAcyclicAndEverySharedPrerequisiteExists(){
        var known=installed();var remaining=new HashSet<>(known);var learned=new HashSet<String>();
        while(!remaining.isEmpty()){
            var next=remaining.stream().filter(id->learned.containsAll(DesignedSkillTree.parents(id,List.of(),known))).toList();
            assertFalse(next.isEmpty(),"cycle or unavailable prerequisite");learned.addAll(next);remaining.removeAll(next);
        }
        assertEquals(List.of("parcool:horizontal_wall_run","parcool:wall_jump"),DesignedSkillTree.parents("parcool:wall_run",List.of(),known));
        assertEquals(List.of("epicfight:impact_guard"),DesignedSkillTree.parents("epicfight:vengeance",List.of(),known));
        assertEquals(List.of("epicfight:swordmaster"),DesignedSkillTree.parents("epicfight:berserker",List.of(),known));
    }
    @Test void absentParCoolHasACompleteEpicMovementRoute(){
        var known=new HashSet<>(installed());known.removeIf(id->id.startsWith("parcool:"));
        assertEquals(List.of("epicfight:endurance"),DesignedSkillTree.parents(DodgeSkillGroup.ID,List.of(),known));
        assertEquals(List.of(DodgeSkillGroup.ID),DesignedSkillTree.parents("epicfight:phantom_ascent",List.of(),known));
        assertEquals(List.of("epicfight:revelation"),DesignedSkillTree.parents("addon:skill",List.of("epicfight:revelation"),known));
    }
    @Test void designedLocationsNeverOverlapAndEveryConnectorAvoidsOtherIcons(){
        var known=installed();var entries=known.stream().sorted().map(id->new SkillTreeLayout.Entry(id,DesignedSkillTree.parents(id,List.of(),known))).toList();
        var graph=SkillTreeLayout.arrangeDesigned(entries);assertEquals(43,graph.positions().size());assertEquals(graph,SkillTreeLayout.arrangeDesigned(entries));
        var positions=new ArrayList<>(graph.positions().values());
        for(int i=0;i<positions.size();i++)for(int j=i+1;j<positions.size();j++){
            var a=positions.get(i);var b=positions.get(j);
            assertTrue(Math.abs(a.x()-b.x())>=40||Math.abs(a.y()-b.y())>=40,"icons overlap");
        }
        for(var entry:entries)for(var parent:entry.parents()){
            var route=SkillTreeLayout.connection(graph,parent,entry.id());
            for(int i=1;i<route.size();i++){
                var a=route.get(i-1);var b=route.get(i);assertTrue(a.x()==b.x()||a.y()==b.y());
                for(var obstacle:graph.positions().entrySet()){
                    if(obstacle.getKey().equals(parent)||obstacle.getKey().equals(entry.id()))continue;
                    var p=obstacle.getValue();
                    boolean crossing=a.y()==b.y()?a.y()>p.y()&&a.y()<p.y()+40&&Math.max(a.x(),b.x())>p.x()-20&&Math.min(a.x(),b.x())<p.x()+20
                            :a.x()>p.x()-20&&a.x()<p.x()+20&&Math.max(a.y(),b.y())>p.y()&&Math.min(a.y(),b.y())<p.y()+40;
                    assertFalse(crossing,parent+" -> "+entry.id()+" crosses "+obstacle.getKey());
                }
            }
        }
    }
    @Test void oldDodgePurchaseMigratesWithoutChargingAndNewProviderJoinsLater(){
        var unlocked=new HashSet<String>();unlocked.add("epicfight:step");
        assertTrue(DodgeSkillGroup.migrate(unlocked,false,true));
        assertTrue(unlocked.containsAll(List.of(DodgeSkillGroup.ID,"epicfight:roll","epicfight:step")));
        assertFalse(unlocked.contains("parcool:dodge"));assertFalse(DodgeSkillGroup.migrate(unlocked,false,true));
        assertTrue(DodgeSkillGroup.migrate(unlocked,true,true));assertTrue(unlocked.contains("parcool:dodge"));
    }
}
