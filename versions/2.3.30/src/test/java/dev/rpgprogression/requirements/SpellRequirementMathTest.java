package dev.rpgprogression.requirements;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpellRequirementMathTest {
    @Test void requestedManaThresholds(){int[] expected={0,15,20,25,30,35,40,45,50};for(int units=0;units<expected.length;units++)assertEquals(expected[units],SpellRequirementMath.intelligence(units,10,5));}
    @Test void overridesDisableAndSaturate(){assertEquals(0,SpellRequirementMath.intelligence(8,10,0));assertEquals(19,SpellRequirementMath.intelligence(2,5,7));assertEquals(10000,SpellRequirementMath.intelligence(8,10000,10000));assertEquals(50,SpellRequirementMath.intelligence(Integer.MAX_VALUE,10,5));}
}
