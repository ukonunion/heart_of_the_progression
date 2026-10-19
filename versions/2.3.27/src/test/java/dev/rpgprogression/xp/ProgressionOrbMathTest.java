package dev.rpgprogression.xp;
import dev.rpgprogression.progression.ProgressionMath;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
class ProgressionOrbMathTest {
    @Test void everyRewardIsPreservedWithABoundedNumberOfEntities(){
        var random=new Random(2326);
        for(int i=0;i<5000;i++){
            long reward=i<10?new long[]{0,1,3,17,25,2477,100000,Long.MAX_VALUE,-1,ProgressionMath.XP_LIMIT}[i]:random.nextLong(ProgressionMath.XP_LIMIT+1);
            var parts=ProgressionOrbMath.split(reward);assertTrue(parts.size()<=12);assertTrue(parts.stream().allMatch(x->x>0));
            assertEquals(Math.max(0,Math.min(ProgressionMath.XP_LIMIT,reward)),parts.stream().mapToLong(Long::longValue).sum());
        }
    }
    @Test void ordinaryRewardsUseFamiliarSmallOrbs(){assertEquals(java.util.List.of(17L,7L,1L),ProgressionOrbMath.split(25));}
    @Test void attractionStaysSlowAcrossTheExpandedRange(){assertEquals(.018,ProgressionOrbMath.attraction(0));assertEquals(.0045,ProgressionOrbMath.attraction(8));assertTrue(ProgressionOrbMath.attraction(12)>0);assertEquals(0,ProgressionOrbMath.attraction(16));assertEquals(0,ProgressionOrbMath.attraction(100));}
}
