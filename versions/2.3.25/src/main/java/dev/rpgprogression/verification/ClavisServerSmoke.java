package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.Path;

@EventBusSubscriber(modid="rpgprogression")
public final class ClavisServerSmoke {
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(!Boolean.getBoolean("rpgprogression.clavisServerSmoke"))return;
        var server=event.getServer();server.execute(()->{
            var checks=new JsonArray();Throwable error=null;
            try {
                ClavisChecks.check(checks,net.neoforged.fml.ModList.get().isLoaded("clavis")==Boolean.getBoolean("rpgprogression.expectClavis"),"Clavis presence matches the isolated test runtime");
                ClavisChecks.common(ConfigurationChecks.player(server.overworld()),checks);
            }catch(Throwable e){error=e;com.mojang.logging.LogUtils.getLogger().error("CLAVIS_SERVER failed",e);}
            try {ClavisChecks.report(Path.of("clavis-2.3.25-result.json"),checks,error);}catch(Exception e){throw new IllegalStateException(e);}
            server.halt(false);
        });
    }
    private ClavisServerSmoke(){}
}
