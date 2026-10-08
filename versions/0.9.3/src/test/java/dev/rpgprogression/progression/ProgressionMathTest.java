package dev.rpgprogression.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionMathTest {
    @Test void defaultLevelCurveAndOverflow() {
        double rate=ProgressionMath.DEFAULT_MULTIPLIER;
        assertEquals(810,ProgressionMath.required(1,810,rate));
        assertEquals(819,ProgressionMath.required(2,810,rate));
        assertEquals(100,ProgressionMath.required(1,100,rate));
        assertEquals(101,ProgressionMath.required(2,100,rate));
        assertEquals(102,ProgressionMath.required(3,100,rate));
        assertEquals(103,ProgressionMath.required(4,100,rate));
        assertEquals(ProgressionMath.XP_LIMIT,ProgressionMath.required(Integer.MAX_VALUE,100,rate));
        assertEquals(ProgressionMath.XP_LIMIT,ProgressionMath.add(ProgressionMath.XP_LIMIT,ProgressionMath.XP_LIMIT));
    }
    @Test void proportionalCurveUsesUnroundedBaseAtEveryLevel() {
        double rate=ProgressionMath.DEFAULT_MULTIPLIER;
        // At level 10 repeated rounding would give 109; evaluating the formula gives 110.
        assertEquals(110,ProgressionMath.required(10,100,rate));
        assertEquals(299,ProgressionMath.required(100,100,rate));
        assertEquals(100,ProgressionMath.required(1,100.49,rate));
        assertEquals(101,ProgressionMath.required(1,100.5,rate));
    }
    @Test void cumulativeCostsRetainExperienceAcrossLevels() {
        double rate=ProgressionMath.DEFAULT_MULTIPLIER;long total=0;
        for(int level=1;level<100;level++)total+=ProgressionMath.required(level,100,rate);
        assertEquals(17876,total);
        assertEquals(97,400-ProgressionMath.required(1,100,rate)-ProgressionMath.required(2,100,rate)-ProgressionMath.required(3,100,rate));
    }
    @Test void staminaScaling() {
        assertEquals(100,ProgressionMath.stamina(9,9,100,10));
        assertEquals(170,ProgressionMath.stamina(16,9,100,10));
        assertEquals(100,ProgressionMath.stamina(1,9,100,10));
    }
    @Test void bowDiminishingReturns() {
        assertEquals(1,ProgressionMath.drawRatio(10,10,.025,.25));
        assertEquals(.8,ProgressionMath.drawRatio(20,10,.025,.25),1e-9);
        assertEquals(2.0/3,ProgressionMath.drawRatio(30,10,.025,.25),1e-9);
        assertEquals(.25,ProgressionMath.drawRatio(10000,10,.025,.25));
    }
    @Test void allInventoryThresholds() {
        int[] thresholds=new int[27];for(int i=0;i<27;i++)thresholds[i]=10+i;
        for(int endurance=0;endurance<=100;endurance++)
            assertEquals(Math.min(27,Math.max(0,endurance-9)),ProgressionMath.unlocked(endurance,thresholds));
    }
}
