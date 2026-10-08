package dev.rpgprogression.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionMathTest {
    @Test void defaultLevelCurveAndOverflow() {
        assertEquals(810,ProgressionMath.required(1,810,1.18));
        assertEquals(956,ProgressionMath.required(2,810,1.18));
        assertEquals(ProgressionMath.XP_LIMIT,ProgressionMath.required(1000,810,10));
        assertEquals(ProgressionMath.XP_LIMIT,ProgressionMath.add(ProgressionMath.XP_LIMIT,ProgressionMath.XP_LIMIT));
    }
    @Test void staminaScaling() {
        assertEquals(100,ProgressionMath.stamina(10,10,100,10));
        assertEquals(170,ProgressionMath.stamina(17,10,100,10));
        assertEquals(100,ProgressionMath.stamina(1,10,100,10));
    }
    @Test void bowDiminishingReturns() {
        assertEquals(1,ProgressionMath.drawRatio(10,10,.025,.25));
        assertEquals(.8,ProgressionMath.drawRatio(20,10,.025,.25),1e-9);
        assertEquals(2.0/3,ProgressionMath.drawRatio(30,10,.025,.25),1e-9);
        assertEquals(.25,ProgressionMath.drawRatio(10000,10,.025,.25));
    }
    @Test void allInventoryThresholds() {
        int[] thresholds=new int[27];for(int i=0;i<27;i++)thresholds[i]=12+2*(i/3);
        for(int endurance=10;endurance<=28;endurance++)
            assertEquals(Math.min(27,Math.max(0,(endurance-10)/2*3)),ProgressionMath.unlocked(endurance,thresholds));
    }
}
