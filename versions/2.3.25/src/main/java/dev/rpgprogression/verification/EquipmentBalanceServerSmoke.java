package dev.rpgprogression.verification;

import com.google.gson.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;

@EventBusSubscriber(modid="rpgprogression")
public final class EquipmentBalanceServerSmoke {
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(!Boolean.getBoolean("rpgprogression.equipmentBalanceSmoke"))return;var server=event.getServer();server.execute(()->{
            var report=new JsonObject();report.addProperty("version","2.3.21");var checks=new JsonArray();
            try{EquipmentBalanceChecks.run(ConfigurationChecks.player(server.overworld()),checks);report.addProperty("result","PASSED");}
            catch(Throwable e){report.addProperty("result","FAILED");report.addProperty("error",e.toString());com.mojang.logging.LogUtils.getLogger().error("EQUIPMENT_BALANCE failed",e);}
            report.add("checks",checks);
            try{Files.writeString(Path.of("equipment-2.3.21-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new IllegalStateException(e);}
            server.halt(false);
        });
    }
    private EquipmentBalanceServerSmoke(){}
}
