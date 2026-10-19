package dev.rpgprogression.skill;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkillTreePathsTest {
    private List<SkillTreeLayout.Entry> entries(){
        var all=new LinkedHashSet<>(DesignedSkillTree.ROOTS);
        all.addAll(List.of("parcool:hide_in_block","parcool:slide","parcool:dive","parcool:fast_swim","parcool:trick_jump","parcool:vault","parcool:wall_jump","parcool:horizontal_wall_run","parcool:wall_run","parcool:hang_down","parcool:ride_zipline","parcool:pole_climb","parcool:slide_down","parcool:climb_up","parcool:breakfall",DodgeSkillGroup.ID,"epicfight:emergency_escape","epicfight:technician","epicfight:phantom_ascent","epicfight:demolition_leap","epicfight:meteor_slam","epicfight:adaptive_skin","epicfight:vengeance","epicfight:guard","epicfight:parrying","epicfight:stamina_pillager","epicfight:hypervitality","epicfight:adrenaline_fiend","epicfight:forbidden_strength","epicfight:death_harvest","epicfight:catharsis","epicfight:bonebreaker","epicfight:berserker"));
        return all.stream().map(id->new SkillTreeLayout.Entry(id,DesignedSkillTree.parents(id,List.of(),all))).toList();
    }
    @Test void eachDependencyIsRepresentedExactlyOnceAndRedConnectionIsAbsent(){
        var nodes=entries();var paths=SkillTreePaths.arrange(SkillTreeLayout.arrangeDesigned(nodes),nodes);var actual=new HashSet<String>();
        for(var path:paths)for(var child:path.children())assertTrue(actual.add(path.parent()+"->"+child),"duplicated connector");
        var expected=new HashSet<String>();for(var node:nodes)for(var parent:node.parents())expected.add(parent+"->"+node.id());
        assertEquals(expected,actual);assertFalse(actual.contains("parcool:fast_run->parcool:long_jump"));
        assertEquals(9,paths.stream().filter(SkillTreePaths.Path::fork).count());
    }
    @Test void rotatedForkTerminalsMatchBothChildBorders(){
        var nodes=entries();var layout=SkillTreeLayout.arrangeDesigned(nodes);var paths=SkillTreePaths.arrange(layout,nodes);
        for(var path:paths)if(path.fork()){
            var endpoints=new ArrayList<double[]>();double r=Math.toRadians(path.rotation());
            for(int sign:new int[]{-1,1}){double x=path.length(),y=sign*path.breadth()/3;endpoints.add(new double[]{path.x()+x*Math.cos(r)-y*Math.sin(r),path.y()+x*Math.sin(r)+y*Math.cos(r)});}
            for(var id:path.children()){
                var child=layout.positions().get(id);double x=child.x(),y=child.y()+20;
                switch(path.rotation()){case 0->x-=20;case 180->x+=20;case 90->y-=20;case 270->y+=20;default->fail("unexpected angle");}
                double tx=x,ty=y;assertTrue(endpoints.stream().anyMatch(e->Math.abs(e[0]-tx)<1e-6&&Math.abs(e[1]-ty)<1e-6),id);
            }
        }
    }
    @Test void straightPathsUseOneWholeTextureAndAllDirectionsStayAvailable(){
        var nodes=entries();var paths=SkillTreePaths.arrange(SkillTreeLayout.arrangeDesigned(nodes),nodes);var angles=new HashSet<Integer>();
        for(var path:paths){assertEquals(40,path.length());if(!path.fork())assertEquals(40,path.breadth());angles.add(path.rotation());}
        assertTrue(angles.containsAll(List.of(0,90,180)));
        assertEquals(List.of(),SkillTreePaths.arrange(SkillTreeLayout.arrange(List.of()),List.of()));
    }
    @Test void brightnessDependsOnTheSourceEvenWhileTheChildIsLockedOrAlreadyLearned(){
        var p=new SkillTreePaths.Path("source",List.of("a","b"),true,0,0,40,120,0);
        assertEquals("paths/double_line_off",p.texture(Set.of()));assertEquals("paths/double_line_off",p.texture(Set.of("a","b")));
        assertEquals("paths/double_line_on",p.texture(Set.of("source")));assertEquals("paths/double_line_on",p.texture(Set.of("source","a")));
    }
}
