package dev.rpgprogression.requirements;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequirementRuleTest {
    @Test void acceptsNamespacedItemsAndTags(){
        assertEquals(new RequirementRule("minecraft:diamond_sword",false,20),RequirementRule.parse(" minecraft:diamond_sword = 20 "));
        assertEquals(new RequirementRule("forge:tools/swords",true,0),RequirementRule.parse("#forge:tools/swords=0"));
    }
    @Test void rejectsMalformedAndOutOfRangeRules(){
        for(String text:new String[]{"sword=20","minecraft:sword=-1","minecraft:sword=10001","minecraft:sword=NaN","minecraft:sword=1=2","#=2","Minecraft:sword=3"})assertFalse(RequirementRule.valid(text),text);
    }
}
