package dev.rpgprogression.xp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class XPRuleTest {
    @Test void explicitZeroAndFractionalTagRewards(){
        assertEquals(0,XPRule.parse("minecraft:zombie=0").amount());
        var tag=XPRule.parse(" #minecraft:fishes = 2.5 ");
        assertTrue(tag.tag());assertEquals("minecraft:fishes",tag.id());assertEquals(2.5,tag.amount());
    }
    @Test void invalidRewardsRejected(){
        for(String value:new String[]{"minecraft:cod=-1","minecraft:cod=NaN","minecraft:cod=Infinity","cod=2","minecraft:cod=1=2"})assertFalse(XPRule.valid(value),value);
    }
}
