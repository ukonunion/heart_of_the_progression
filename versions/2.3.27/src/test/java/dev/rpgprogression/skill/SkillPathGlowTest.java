package dev.rpgprogression.skill;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SkillPathGlowTest {
    @Test void opensKnownPathsWithoutReplayingAndFadesOnlyNewlyLearnedSources(){
        var g=new SkillPathGlow();g.observe(List.of("old","new","other"),Set.of("old"),0);
        assertEquals(1,g.alpha("old",Set.of("old"),1));assertEquals(0,g.alpha("new",Set.of("old"),1));
        g.observe(List.of("old","new","other"),Set.of("old","new"),100);
        assertEquals(0,g.alpha("new",Set.of("old","new"),100));
        assertEquals(.5,g.alpha("new",Set.of("old","new"),100+SkillPathGlow.DURATION/2),.0001);
        g.observe(List.of("old","new","other"),Set.of("old","new"),100+SkillPathGlow.DURATION/2);
        assertEquals(.5,g.alpha("new",Set.of("old","new"),100+SkillPathGlow.DURATION/2),.0001);
        assertEquals(1,g.alpha("new",Set.of("old","new"),100+SkillPathGlow.DURATION));
        assertEquals(0,g.alpha("other",Set.of("old","new"),Long.MAX_VALUE));
    }
    @Test void revokingAndRelearningResetsTheTransition(){
        var g=new SkillPathGlow();g.observe(List.of("a"),Set.of(),0);g.observe(List.of("a"),Set.of("a"),1);
        g.observe(List.of("a"),Set.of(),100);assertEquals(0,g.alpha("a",Set.of(),100));
        g.observe(List.of("a"),Set.of("a"),200);assertEquals(0,g.alpha("a",Set.of("a"),200));
        float previous=0;for(int i=0;i<=20;i++){float a=g.alpha("a",Set.of("a"),200+SkillPathGlow.DURATION*i/20);assertTrue(a>=previous);previous=a;}
    }
}
