package dev.rpgprogression.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClavisMathTest {
    @Test void startsAtTenAndUsesCompleteGroups() {
        int[] levels={0,4,5,9,10,14,15,19,20,24,25,99,100};
        double[] expected={0,0,0,0,0,0,.025,.025,.05,.05,.075,.425,.45};
        for(int i=0;i<levels.length;i++)assertEquals(expected[i],ClavisMath.lifeSaveChance(levels[i],.025),1e-12,"Agility "+levels[i]);
    }
    @Test void configCanDisableOrChangeTheBonusAndChanceCannotOverflow() {
        assertEquals(0,ClavisMath.lifeSaveChance(100,0));
        assertEquals(.2,ClavisMath.lifeSaveChance(20,.1));
        assertEquals(1,ClavisMath.lifeSaveChance(210,.025));
        assertEquals(1,ClavisMath.lifeSaveChance(Integer.MAX_VALUE,1));
        assertEquals(0,ClavisMath.lifeSaveChance(Integer.MIN_VALUE,.025));
        assertEquals(0,ClavisMath.lifeSaveChance(100,Double.NaN));
        assertEquals(0,ClavisMath.lifeSaveChance(100,Double.POSITIVE_INFINITY));
        assertEquals(0,ClavisMath.lifeSaveChance(100,-.025));
    }
}
