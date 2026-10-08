package dev.rpgprogression.resource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ResourceMathTest {
    @Test void manaMilestonesAndCostConversion(){
        assertEquals(0,ResourceMath.manaCapacity(10,10));assertEquals(0,ResourceMath.manaCapacity(14,10));
        assertEquals(1,ResourceMath.manaCapacity(15,10));assertEquals(7,ResourceMath.manaCapacity(49,10));
        assertEquals(8,ResourceMath.manaCapacity(50,10));assertEquals(8,ResourceMath.manaCapacity(Integer.MAX_VALUE,10));
        assertEquals(0,ResourceMath.spellCost(0));assertEquals(1,ResourceMath.spellCost(1));
        assertEquals(1,ResourceMath.spellCost(25));assertEquals(2,ResourceMath.spellCost(26));
        assertEquals(8,ResourceMath.spellCost(200));assertEquals(8,ResourceMath.spellCost(Integer.MAX_VALUE));
    }
    @Test void fullRecoveryAtTwentyMinutesAndFasterLevels(){
        assertEquals(25,ResourceMath.sparkCapacity(1,1,25,5));
        assertEquals(30,ResourceMath.sparkCapacity(2,1,25,5));
        assertEquals(24000,ResourceMath.sparkRecoveryTicks(1,1,1200,.1));
        double value=0;for(int i=0;i<24000;i++)value=ResourceMath.recover(value,25,25.0/24000);
        assertEquals(25,value,1e-9);
        assertTrue(ResourceMath.sparkRecoveryTicks(2,1,1200,.1)<24000);
        assertEquals(30,ResourceMath.recover(29.9,30,1));
    }
}
