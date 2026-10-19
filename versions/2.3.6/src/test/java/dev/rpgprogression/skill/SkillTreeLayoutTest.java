package dev.rpgprogression.skill;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SkillTreeLayoutTest {
    private static SkillTreeLayout.Entry node(String id,String...parents){return new SkillTreeLayout.Entry(id,List.of(parents));}
    @Test void forksAndSharedPrerequisitesDoNotOverlap(){
        var entries=List.of(node("root"),node("left","root"),node("right","root"),node("merged","left","right"),node("leaf","right"));
        var graph=SkillTreeLayout.arrange(entries);
        for(var entry:entries)for(var parent:entry.parents())assertTrue(graph.positions().get(parent).depth()<graph.positions().get(entry.id()).depth());
        var positions=new ArrayList<>(graph.positions().values());
        for(int i=0;i<positions.size();i++)for(int j=i+1;j<positions.size();j++){
            var a=positions.get(i);var b=positions.get(j);
            assertTrue(Math.abs(a.x()-b.x())>=40||Math.abs(a.y()-b.y())>=76,"node frames and labels overlap");
        }
    }
    @Test void forestIsDeterministicAndContainsEveryNode(){
        var entries=List.of(node("a"),node("b","a"),node("c","a"),node("d"),node("e","d"));
        var graph=SkillTreeLayout.arrange(entries);assertEquals(graph,SkillTreeLayout.arrange(entries));assertEquals(5,graph.positions().size());
        for(var p:graph.positions().values()){assertTrue(p.x()-20>=0&&p.x()+20<=graph.width());assertTrue(p.y()>=0&&p.y()+76<=graph.height());}
    }
    @Test void malformedAddonGraphCannotFreezeTheScreen(){
        var entries=List.of(node("a","b"),node("b","a"),node("orphan","missing"),node("self","self"),node("orphan"));
        var graph=assertTimeout(java.time.Duration.ofSeconds(2),()->SkillTreeLayout.arrange(entries));
        assertEquals(Set.of("a","b","orphan","self"),graph.positions().keySet());
        assertTrue(Double.isFinite(graph.width())&&Double.isFinite(graph.height()));
    }
}
