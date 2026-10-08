package dev.rpgprogression.network;

import dev.rpgprogression.progression.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.*;

/** Common code has no references to net.minecraft.client, including packet handlers. */
public final class ModNetworking {
    private static final String VERSION="3";
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("rpgprogression","main"),()->VERSION,VERSION::equals,VERSION::equals);
    public static Consumer<SyncProgression> clientSync=p->{};
    public static Consumer<SyncStamina> clientStamina=p->{};
    public static Consumer<LevelUp> clientLevelUp=p->{};
    public static Consumer<RecipeUnlock> clientRecipe=p->{};
    public static Consumer<SyncRules> clientRules=p->{};
    public static Consumer<SyncResources> clientResources=p->{};
    public static void register() {
        CHANNEL.messageBuilder(SpendPoint.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((p,b)->b.writeVarInt(p.attribute)).decoder(b->new SpendPoint(b.readVarInt()))
            .consumerMainThread((p,c)->spend(c.get().getSender(),p.attribute)).add();
        CHANNEL.messageBuilder(RequestData.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((p,b)->{}).decoder(b->new RequestData())
            .consumerMainThread((p,c)->request(c.get().getSender())).add();
        CHANNEL.messageBuilder(SyncProgression.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->b.writeNbt(p.data)).decoder(b->new SyncProgression(b.readNbt()))
            .consumerMainThread((p,c)->clientSync.accept(p)).add();
        CHANNEL.messageBuilder(SyncStamina.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeDouble(p.current);b.writeBoolean(p.exhausted);}).decoder(b->new SyncStamina(b.readDouble(),b.readBoolean()))
            .consumerMainThread((p,c)->clientStamina.accept(p)).add();
        CHANNEL.messageBuilder(LevelUp.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeVarInt(p.level);b.writeVarInt(p.points);}).decoder(b->new LevelUp(b.readVarInt(),b.readVarInt()))
            .consumerMainThread((p,c)->clientLevelUp.accept(p)).add();
        CHANNEL.messageBuilder(RecipeUnlock.class,5,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->b.writeComponent(p.name)).decoder(b->new RecipeUnlock(b.readComponent()))
            .consumerMainThread((p,c)->clientRecipe.accept(p)).add();
        CHANNEL.messageBuilder(SyncRules.class,6,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->b.writeUtf(p.toml,1048576)).decoder(b->new SyncRules(b.readUtf(1048576)))
            .consumerMainThread((p,c)->clientRules.accept(p)).add();
        CHANNEL.messageBuilder(SyncResources.class,7,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p,b)->{b.writeDouble(p.spark);b.writeDouble(p.mana);b.writeDouble(p.manaCapacity);b.writeBoolean(p.manaActive);})
                .decoder(b->new SyncResources(b.readDouble(),b.readDouble(),b.readDouble(),b.readBoolean()))
                .consumerMainThread((p,c)->clientResources.accept(p)).add();
    }
    public static void spend(ServerPlayer p,int attribute){
        if(p==null||p.isRemoved()||!p.isAlive())return;
        p.getCapability(PlayerProgressionProvider.CAPABILITY).ifPresent(d->{
            if(p.tickCount-d.lastSpendTick>=2){d.lastSpendTick=p.tickCount;ProgressionManager.spend(p,attribute);}
        });
    }
    public static void request(ServerPlayer p){
        if(p==null||p.isRemoved())return;
        p.getCapability(PlayerProgressionProvider.CAPABILITY).ifPresent(d->{
            if(p.tickCount-d.lastRequestTick>=20){d.lastRequestTick=p.tickCount;sync(p);}
        });
    }
    public static void sync(ServerPlayer p) {
        if(p.isRemoved())return;
        p.getCapability(PlayerProgressionProvider.CAPABILITY).ifPresent(d->{send(p,new SyncProgression(d.save()));
        d.dirty=false; d.syncTicks=0; d.lastSentStamina=d.stamina;
        d.lastSentSpark=d.spark;d.lastSentMana=d.mana;d.lastSentManaCapacity=d.manaCapacity;d.lastSentManaActive=d.manaActive;
        });
    }
    public static void stamina(ServerPlayer p) {if(!p.isRemoved())p.getCapability(PlayerProgressionProvider.CAPABILITY).ifPresent(d->{send(p,new SyncStamina(d.stamina,d.exhausted));d.lastSentStamina=d.stamina;});}
    public static void resources(ServerPlayer p){if(!p.isRemoved())p.getCapability(PlayerProgressionProvider.CAPABILITY).ifPresent(d->{send(p,new SyncResources(d.spark,d.mana,d.manaCapacity,d.manaActive));d.lastSentSpark=d.spark;d.lastSentMana=d.mana;d.lastSentManaCapacity=d.manaCapacity;d.lastSentManaActive=d.manaActive;});}
    public static void levelUp(ServerPlayer p,int level,int points) { send(p,new LevelUp(level,points)); }
    public static void recipeUnlocked(ServerPlayer p,Component name) { send(p,new RecipeUnlock(name)); }
    /** Forge's built-in config handshake runs only on login; resend after live server reloads. */
    public static void rules(ServerPlayer p) {
        net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().get(net.minecraftforge.fml.config.ModConfig.Type.SERVER).stream()
                .filter(c->c.getModId().equals("rpgprogression")).findFirst().ifPresent(c->
                    send(p,new SyncRules(new com.electronwill.nightconfig.toml.TomlWriter().writeToString(c.getConfigData()))));
    }
    private static void send(ServerPlayer p,Object packet) { CHANNEL.send(PacketDistributor.PLAYER.with(()->p),packet); }
    public record SpendPoint(int attribute) {}
    public record RequestData() {}
    public record SyncProgression(CompoundTag data) {}
    public record SyncStamina(double current,boolean exhausted) {}
    public record LevelUp(int level,int points) {}
    public record RecipeUnlock(Component name) {}
    public record SyncRules(String toml) {}
    public record SyncResources(double spark,double mana,double manaCapacity,boolean manaActive){}
    private ModNetworking() {}
}
