package dev.rpgprogression.verification;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;

@EventBusSubscriber(modid="rpgprogression")
public final class SkintTravelServerSmoke {
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(!Boolean.getBoolean("rpgprogression.skintTravelServerSmoke"))return;var server=event.getServer();server.execute(()->{
            var report=new JsonObject();report.addProperty("version","2.3.4");
            try{var checks=SkintTravelChecks.run(server.overworld(),new BlockPos(0,160,0));report.add("checks",checks);report.addProperty("result",checks.asList().stream().allMatch(c->c.getAsJsonObject().get("passed").getAsBoolean())?"PASSED":"FAILED");}
            catch(Throwable error){report.addProperty("result","FAILED");report.addProperty("error",error.toString());com.mojang.logging.LogUtils.getLogger().error("SKINT_TRAVEL_SERVER failed",error);}
            try{Files.writeString(Path.of("skint-2.3.4-server-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("SKINT_TRAVEL_SERVER report failed",error);}
            com.mojang.logging.LogUtils.getLogger().info("SKINT_TRAVEL_SERVER_RESULT {}",report.get("result"));server.halt(false);
        });
    }
    private SkintTravelServerSmoke(){}
}
