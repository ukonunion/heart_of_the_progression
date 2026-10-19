package dev.rpgprogression.xp;

import dev.rpgprogression.progression.ProgressionMath;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobXPTest {
    @Test void exactEndpointsAndCenter(){
        assertEquals(15,MobXP.reward(20,0));assertEquals(20,MobXP.reward(20,.5));assertEquals(25,MobXP.reward(20,1));
        assertEquals(1125,MobXP.reward(1500,0));assertEquals(1875,MobXP.reward(1500,1));
    }
    @Test void eachRewardRoundsIndependently(){
        assertEquals(2,MobXP.reward(3,0));assertEquals(3,MobXP.reward(3,.5));assertEquals(4,MobXP.reward(3,1));
        assertEquals(20,MobXP.reward(26,0));assertEquals(33,MobXP.reward(26,1));
    }
    @Test void zeroAndOverflowStayBounded(){
        for(double roll:new double[]{0,.5,1})assertEquals(0,MobXP.reward(0,roll));
        assertEquals(0,MobXP.reward(-100,1));
        assertEquals(ProgressionMath.XP_LIMIT,MobXP.reward(ProgressionMath.XP_LIMIT,1));
        assertEquals(ProgressionMath.XP_LIMIT,MobXP.reward(Long.MAX_VALUE,1));
    }
    @Test void allRollsStayInsideTheRoundedInterval(){
        for(long base:new long[]{1,3,20,26,100,1500,1000000})for(int i=0;i<=1000;i++){
            long reward=MobXP.reward(base,i/1000.0);
            assertTrue(reward>=Math.round(base*.75)&&reward<=Math.round(base*1.25));
        }
    }
}
