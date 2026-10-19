package dev.rpgprogression.verification;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;

@EventBusSubscriber(modid="rpgprogression")
public final class SkintChargeServerSmoke {
    private static boolean pending;
    @SubscribeEvent public static void started(ServerStartedEvent event){pending=Boolean.getBoolean("rpgprogression.skintChargeServerSmoke");}
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        if(!pending)return;pending=false;var server=event.getServer();server.execute(()->{
            var report=new JsonObject();report.addProperty("version","2.3.15");boolean restart=Boolean.getBoolean("rpgprogression.skintChargeRestart");
            try{
                var level=server.overworld();var checks=new JsonArray();
                if(restart){
                    SkintTravelChecks.check(checks,!dev.rpgprogression.mob.SkintChargeWorldData.get(level).claimFirstDragon(false),"a complete dedicated-server restart preserves the consumed first-dragon guarantee");
                    SkintActivationChecks.restart(level,checks);
                    if(Boolean.getBoolean("rpgprogression.configCustomRestart"))ConfigurationChecks.loadedOverride(level,checks);
                }else{
                    checks=SkintChargeChecks.run(level,new BlockPos(30,170,30));ConfigurationChecks.run(ConfigurationChecks.player(level),checks);checks.addAll(SkintTravelChecks.run(level,new BlockPos(0,160,0)));
                    SkintActivationChecks.run(ConfigurationChecks.player(level),checks);SkintActivationChecks.persist(level);
                    dev.rpgprogression.mob.SkintChargeWorldData.get(level).claimFirstDragon(false);
                }
                report.add("checks",checks);report.addProperty("result",checks.asList().stream().allMatch(c->c.getAsJsonObject().get("passed").getAsBoolean())?"PASSED":"FAILED");
            }catch(Throwable error){report.addProperty("result","FAILED");report.addProperty("error",error.toString());com.mojang.logging.LogUtils.getLogger().error("SKINT_CHARGE_SERVER failed",error);}
            try{Files.writeString(Path.of("skint-2.3.15-"+(restart?"restart":"server")+"-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("SKINT_CHARGE_SERVER report failed",error);}
            com.mojang.logging.LogUtils.getLogger().info("SKINT_CHARGE_SERVER_RESULT {}",report.get("result"));server.halt(false);
        });
    }
    private SkintChargeServerSmoke(){}
}
