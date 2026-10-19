package dev.rpgprogression.verification;
import com.google.gson.JsonArray;
import dev.rpgprogression.config.RPGConfig;
import java.nio.file.*;
import java.util.List;
public final class AccessConfigTemplates {
    public static void write(JsonArray checks)throws Exception{
        var defaults=com.electronwill.nightconfig.core.CommentedConfig.inMemory();RPGConfig.SERVER.correct(defaults);defaults.set("general.progressionRulesVersion",11);var writer=new com.electronwill.nightconfig.toml.TomlWriter();var parser=new com.electronwill.nightconfig.toml.TomlParser();var folder=Path.of("verification/config-2.3.22");Files.createDirectories(folder);String text=writer.writeToString(defaults);
        AccessRulesChecks.check(checks,RPGConfig.SERVER.isCorrect(parser.parse(text)),"the complete default TOML including all new access settings round-trips through the real schema");Files.writeString(folder.resolve("rpgprogression-server-default.toml"),text);
        var custom=parser.parse(text);custom.set("spark.skintOpenMinimum",10.0);custom.set("interactionRequirements.villagerTradeIntelligence",18);custom.set("spellRequirements.baseIntelligence",5);custom.set("spellRequirements.intelligencePerManaUnit",7);custom.set("itemRequirements.intelligence",List.of("minecraft:lever=12"));custom.set("itemRequirements.spark_potential",List.of("minecraft:ender_chest=0"));text=writer.writeToString(custom);
        AccessRulesChecks.check(checks,RPGConfig.SERVER.isCorrect(parser.parse(text)),"the new editable custom TOML validates opening spark, trade level, scroll formula and item overrides");Files.writeString(folder.resolve("rpgprogression-server-custom-example.toml"),text);
    }
    private AccessConfigTemplates(){}
}
