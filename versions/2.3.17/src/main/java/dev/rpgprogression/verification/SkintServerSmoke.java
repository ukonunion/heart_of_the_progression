package dev.rpgprogression.verification;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;

@EventBusSubscriber(modid="rpgprogression")
public final class SkintServerSmoke {
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(!Boolean.getBoolean("rpgprogression.skintServerSmoke"))return;
        var server=event.getServer();server.execute(()->{
            var report=new JsonObject();report.addProperty("version","2.2.4");
            try{
                var level=server.overworld();var checks=TravelSkintChecks.run(level,FakePlayerFactory.getMinecraft(level),new BlockPos(0,160,0));
                boolean passed=checks.asList().stream().allMatch(c->c.getAsJsonObject().get("passed").getAsBoolean());
                report.addProperty("result",passed?"PASSED":"FAILED");report.add("checks",checks);
            }catch(Throwable error){report.addProperty("result","FAILED");report.addProperty("error",error.toString());com.mojang.logging.LogUtils.getLogger().error("SKINT_SERVER failed",error);}
            try{Files.writeString(Path.of("skint-2.2.4-server-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}
            catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("SKINT_SERVER report failed",error);}
            com.mojang.logging.LogUtils.getLogger().info("SKINT_SERVER_RESULT {}",report.get("result"));server.halt(false);
        });
    }
    private SkintServerSmoke(){}
}
