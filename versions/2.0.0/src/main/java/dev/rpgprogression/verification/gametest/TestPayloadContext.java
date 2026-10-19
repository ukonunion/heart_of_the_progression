package dev.rpgprogression.verification.gametest;

import java.lang.reflect.Proxy;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Supplies the sending player to the real native handler, without constructing a network channel. */
final class TestPayloadContext {
    static IPayloadContext of(ServerPlayer player){
        return (IPayloadContext)Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),new Class<?>[]{IPayloadContext.class},(proxy,method,args)->{
            if(method.getName().equals("player"))return player;
            throw new AssertionError("Unexpected native handler context operation: "+method.getName());
        });
    }
    private TestPayloadContext(){}
}
