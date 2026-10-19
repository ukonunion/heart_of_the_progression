package dev.rpgprogression.requirements;

import com.google.gson.*;
import dev.rpgprogression.attribute.RPGAttribute;
import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentRequirementsTest {
    @TestFactory Stream<DynamicTest> requestedBalance()throws Exception{
        try(var reader=new InputStreamReader(getClass().getResourceAsStream("/equipment-requirements-2.3.21.json"),StandardCharsets.UTF_8)){
            return JsonParser.parseReader(reader).getAsJsonArray().asList().stream().map(JsonElement::getAsJsonObject).map(c->DynamicTest.dynamicTest(c.get("id").getAsString(),()->{
                var expected=new EnumMap<RPGAttribute,Integer>(RPGAttribute.class);
                c.getAsJsonObject("requirements").entrySet().forEach(e->expected.put(RPGAttribute.valueOf(e.getKey().toUpperCase(Locale.ROOT)),e.getValue().getAsInt()));
                assertEquals(expected,EquipmentRequirements.defaultsFor(c.get("id").getAsString(),c.get("toolMinimum").getAsInt(),c.get("armorMinimum").getAsInt()));
            }));
        }
    }
    @Test void otherModsKeepTheirMaterialRequirement(){
        assertEquals(Map.of(RPGAttribute.ENDURANCE,20),EquipmentRequirements.defaultsFor("another_mod:diamond_dagger",20,0));
        assertEquals(Map.of(RPGAttribute.ENDURANCE,15),EquipmentRequirements.defaultsFor("another_mod:iron_spear",15,0));
    }
}
