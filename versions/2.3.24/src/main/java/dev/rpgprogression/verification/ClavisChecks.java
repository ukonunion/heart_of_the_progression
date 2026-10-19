package dev.rpgprogression.verification;

import com.google.gson.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.ClavisCompat;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import net.minecraft.world.entity.player.Player;
import java.nio.file.*;

public final class ClavisChecks {
    public static void check(JsonArray checks,boolean ok,String description) {
        SkintTravelChecks.check(checks,ok,description);
    }
    public static void common(Player player,JsonArray checks)throws Exception {
        var data=PlayerProgressionProvider.get(player).orElseThrow();var saved=data.save();double previous=RPGConfig.CLAVIS_LIFE_SAVE.get();
        try {
            RPGConfig.CLAVIS_LIFE_SAVE.set(.025);
            int[] levels={0,4,5,9,10,14,15,19,20,24,25,99,100,209,210,10000};
            double[] chances={0,0,0,0,0,0,.025,.025,.05,.05,.075,.425,.45,.975,1,1};
            for(int i=0;i<levels.length;i++){
                data.set(RPGAttribute.AGILITY,levels[i]);
                check(checks,Math.abs(ClavisCompat.lifeSaveChance(player)-chances[i])<1e-12,"Agility "+levels[i]+" gives "+chances[i]*100+"% life protection from level 10");
            }
            data.set(RPGAttribute.AGILITY,100);RPGConfig.CLAVIS_LIFE_SAVE.set(0.0);
            check(checks,ClavisCompat.lifeSaveChance(player)==0,"the server config can disable the bonus");
            RPGConfig.CLAVIS_LIFE_SAVE.set(.05);data.set(RPGAttribute.AGILITY,20);
            check(checks,ClavisCompat.lifeSaveChance(player)==.1,"the server config can change the bonus per five levels");
            check(checks,ClavisCompat.lifeSaveChance(null)==0,"no player means no protection");
            var defaults=com.electronwill.nightconfig.core.CommentedConfig.inMemory();RPGConfig.SERVER.correct(defaults);
            var writer=new com.electronwill.nightconfig.toml.TomlWriter();var parser=new com.electronwill.nightconfig.toml.TomlParser();
            var folder=Path.of("verification/config-2.3.24");Files.createDirectories(folder);
            String text=writer.writeToString(defaults);check(checks,RPGConfig.SERVER.isCorrect(parser.parse(text)),"the complete default TOML round-trips through the actual schema");
            Files.writeString(folder.resolve("rpgprogression-server-default.toml"),text);
            var custom=parser.parse(text);custom.set("attributes.clavisLifeSaveChancePerFiveAgility",.05);
            text=writer.writeToString(custom);check(checks,RPGConfig.SERVER.isCorrect(parser.parse(text)),"the custom Clavis TOML is accepted by the actual schema");
            Files.writeString(folder.resolve("rpgprogression-server-custom-example.toml"),text);
        } finally {RPGConfig.CLAVIS_LIFE_SAVE.set(previous);data.load(saved);}
    }
    public static void report(Path file,JsonArray checks,Throwable error)throws Exception {
        var report=new JsonObject();report.addProperty("version","2.3.24");
        report.addProperty("result",error==null&&checks.asList().stream().allMatch(c->c.getAsJsonObject().get("passed").getAsBoolean())?"PASSED":"FAILED");
        if(error!=null)report.addProperty("error",error.toString());report.add("checks",checks);
        Files.createDirectories(file.toAbsolutePath().getParent());Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
    private ClavisChecks(){}
}
