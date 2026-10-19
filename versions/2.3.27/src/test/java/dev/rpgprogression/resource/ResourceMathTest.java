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
    @Test void fullRecoveryAtFortyMinutesAndFasterLevels(){
        assertEquals(25,ResourceMath.sparkCapacity(1,1,25,5));
        assertEquals(30,ResourceMath.sparkCapacity(2,1,25,5));
        assertEquals(48000,ResourceMath.sparkRecoveryTicks(10,10,2400,.1));
        double value=0;for(int i=0;i<48000;i++)value=ResourceMath.recover(value,25,25.0/48000);
        assertEquals(25,value,1e-9);
        assertEquals(24000,ResourceMath.sparkRecoveryTicks(20,10,2400,.1));
        assertEquals(9600,ResourceMath.sparkRecoveryTicks(50,10,2400,.1));
        assertEquals(30,ResourceMath.recover(29.9,30,1));
    }
    @Test void exhaustionStartsAtTenPercentOfTheCurrentMaximum(){
        assertFalse(ResourceMath.sparkDepleted(2.500001,25));
        assertTrue(ResourceMath.sparkDepleted(2.5,25));
        assertTrue(ResourceMath.sparkDepleted(0,25));
        assertFalse(ResourceMath.sparkDepleted(6.500001,65));
        assertTrue(ResourceMath.sparkDepleted(6.5,65));
        assertFalse(ResourceMath.sparkDepleted(0,0));
    }
}
