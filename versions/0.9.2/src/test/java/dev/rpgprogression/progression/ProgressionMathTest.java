package dev.rpgprogression.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionMathTest {
    @Test void defaultLevelCurveAndOverflow() {
        assertEquals(100,ProgressionMath.required(1,100,1.18));
        assertEquals(118,ProgressionMath.required(2,100,1.18));
        assertEquals(139,ProgressionMath.required(3,100,1.18));
        assertEquals(ProgressionMath.XP_LIMIT,ProgressionMath.required(1000,100,10));
        assertEquals(ProgressionMath.XP_LIMIT,ProgressionMath.add(ProgressionMath.XP_LIMIT,ProgressionMath.XP_LIMIT));
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
