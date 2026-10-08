package dev.rpgprogression.resource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ResourceMathTest {
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
