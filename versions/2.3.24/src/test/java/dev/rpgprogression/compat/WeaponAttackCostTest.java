package dev.rpgprogression.compat;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WeaponAttackCostTest {
    @Test void weaponCategoriesAndFallback(){
        var costs=List.of("dagger=6","sword=12","greatsword=24","addon:hammer=30");
        assertEquals(6,WeaponAttackCost.get("DAGGER",costs,12));
        assertEquals(24,WeaponAttackCost.get("GREATSWORD",costs,12));
        assertEquals(30,WeaponAttackCost.get("addon:hammer",costs,12));
        assertEquals(12,WeaponAttackCost.get("unknown",costs,12));
    }
    @Test void rejectsUnusableCosts(){
        for(String bad:List.of("sword=-1","sword=NaN","sword=Infinity","sword=10001","=3","sword=abc","sword=2=3"))assertFalse(WeaponAttackCost.valid(bad),bad);
        assertTrue(WeaponAttackCost.valid("sword=0"));
        assertTrue(WeaponAttackCost.valid("addon:heavy_hammer=24.5"));
    }
}
