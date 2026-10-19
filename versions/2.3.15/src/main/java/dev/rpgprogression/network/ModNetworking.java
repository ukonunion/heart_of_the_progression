package dev.rpgprogression.network;

import dev.rpgprogression.progression.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.function.*;

/** Server-authoritative payloads; common handlers never reference client classes. */
public final class ModNetworking {
    private static final String VERSION="18";
    public static final Channel CHANNEL=new Channel();
    public static final class Channel { public void sendToServer(CustomPacketPayload packet) { PacketDistributor.sendToServer(packet); } }
    public static Consumer<SyncProgression> clientSync=p->{};
    public static Consumer<SyncStamina> clientStamina=p->{};
    public static Consumer<LevelUp> clientLevelUp=p->{};
    public static Consumer<RecipeUnlock> clientRecipe=p->{};
    public static Consumer<SyncRules> clientRules=p->{};
    public static Consumer<SyncResources> clientResources=p->{};
    public static Consumer<TreeSnapshot> clientTree=p->{};
    public static Consumer<SkintSnapshot> clientSkint=p->{};
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String name) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rpgprogression", name));
    }
    private static <T> StreamCodec<RegistryFriendlyByteBuf,T> codec(BiConsumer<RegistryFriendlyByteBuf,T> writer, Function<RegistryFriendlyByteBuf,T> reader) {
        return StreamCodec.of(writer::accept,reader::apply);
    }
    private static ServerPlayer sender(IPayloadContext context) { return (ServerPlayer)context.player(); }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar(VERSION);
        registrar.playToServer(SpendPoint.TYPE, codec((b,p)->{b.writeVarInt(p.attribute);},b->new SpendPoint(b.readVarInt())),(p,c)->spend(sender(c),p.attribute));
        registrar.playToServer(RequestData.TYPE, codec((b,p)->{},b->new RequestData()),(p,c)->request(sender(c)));
        registrar.playToClient(SyncProgression.TYPE, codec((b,p)->{b.writeNbt(p.data);},b->new SyncProgression(b.readNbt())),(p,c)->clientSync.accept(p));
        registrar.playToClient(SyncStamina.TYPE, codec((b,p)->{b.writeDouble(p.current);b.writeBoolean(p.exhausted);},b->new SyncStamina(b.readDouble(),b.readBoolean())),(p,c)->clientStamina.accept(p));
        registrar.playToClient(LevelUp.TYPE, codec((b,p)->{b.writeVarInt(p.level);b.writeVarInt(p.points);},b->new LevelUp(b.readVarInt(),b.readVarInt())),(p,c)->clientLevelUp.accept(p));
        registrar.playToClient(RecipeUnlock.TYPE, codec((b,p)->{ComponentSerialization.STREAM_CODEC.encode(b,p.name);},b->new RecipeUnlock(ComponentSerialization.STREAM_CODEC.decode(b))),(p,c)->clientRecipe.accept(p));
        registrar.playToClient(SyncRules.TYPE, codec((b,p)->{b.writeUtf(p.toml,1048576);},b->new SyncRules(b.readUtf(1048576))),(p,c)->clientRules.accept(p));
        registrar.playToClient(SyncResources.TYPE, codec((b,p)->{b.writeDouble(p.spark);b.writeDouble(p.mana);b.writeDouble(p.manaCapacity);b.writeBoolean(p.manaActive);},b->new SyncResources(b.readDouble(),b.readDouble(),b.readDouble(),b.readBoolean())),(p,c)->clientResources.accept(p));
        registrar.playToServer(RequestTree.TYPE, codec((b,p)->{},b->new RequestTree()),(p,c)->requestTree(sender(c)));
        registrar.playToClient(TreeSnapshot.TYPE, codec((b,p)->{b.writeNbt(p.data);},b->new TreeSnapshot(b.readNbt())),(p,c)->clientTree.accept(p));
        registrar.playToServer(UnlockSkill.TYPE, codec((b,p)->{b.writeUtf(p.id,256);},b->new UnlockSkill(b.readUtf(256))),(p,c)->skill(sender(c),p.id,-1));
        registrar.playToServer(EquipSkill.TYPE, codec((b,p)->{b.writeUtf(p.id,256);b.writeVarInt(p.slot);},b->new EquipSkill(b.readUtf(256),b.readVarInt())),(p,c)->{if(p.slot>=0)skill(sender(c),p.id,p.slot);});
        registrar.playToServer(SkintAction.TYPE,codec((b,p)->{b.writeVarInt(p.action);b.writeUUID(p.token);b.writeUUID(p.target);},b->new SkintAction(b.readVarInt(),b.readUUID(),b.readUUID())),(p,c)->dev.rpgprogression.skint.SkintManager.action(sender(c),p.action,p.token,p.target));
        registrar.playToClient(SkintSnapshot.TYPE,codec((b,p)->b.writeNbt(p.data),b->new SkintSnapshot(b.readNbt())),(p,c)->clientSkint.accept(p));
    }
    public static void requestTree(ServerPlayer p){
        if(p==null||p.isRemoved()||!p.isAlive())return;
        PlayerProgressionProvider.get(p).ifPresent(d->{if(p.tickCount-d.lastTreeRequestTick>=10){d.lastTreeRequestTick=p.tickCount;dev.rpgprogression.skill.SkillTreeManager.sync(p);}});
    }
    private static void skill(ServerPlayer p,String id,int slot){
        if(p==null||p.isRemoved()||!p.isAlive()||!dev.rpgprogression.skint.SkintManager.canProgress(p))return;
        PlayerProgressionProvider.get(p).ifPresent(d->{if(p.tickCount-d.lastSkillTick>=2){
            d.lastSkillTick=p.tickCount;
            if(slot==-1){dev.rpgprogression.skill.SkillTreeManager.unlock(p,id);dev.rpgprogression.skill.SkillTreeManager.sync(p);}
            else if(slot>=0)dev.rpgprogression.skill.SkillTreeManager.equip(p,id,slot);
        }});
    }
    public static void treeSnapshot(ServerPlayer p,CompoundTag data){send(p,new TreeSnapshot(data));}
    public static void skintSnapshot(ServerPlayer p,CompoundTag data){send(p,new SkintSnapshot(data));}
    public static void spend(ServerPlayer p,int attribute){
        if(p==null||p.isRemoved()||!p.isAlive()||!dev.rpgprogression.skint.SkintManager.canProgress(p))return;
        PlayerProgressionProvider.get(p).ifPresent(d->{
            if(p.tickCount-d.lastSpendTick>=2){d.lastSpendTick=p.tickCount;ProgressionManager.spend(p,attribute);}
        });
    }
    public static void request(ServerPlayer p){
        if(p==null||p.isRemoved())return;
        PlayerProgressionProvider.get(p).ifPresent(d->{
            if(p.tickCount-d.lastRequestTick>=20){d.lastRequestTick=p.tickCount;sync(p);}
        });
    }
    public static void sync(ServerPlayer p) {
        if(p.isRemoved())return;
        PlayerProgressionProvider.get(p).ifPresent(d->{dev.rpgprogression.defense.EquipmentDefense.updateCache(p,d);send(p,new SyncProgression(d.save()));
        d.dirty=false; d.syncTicks=0; d.lastSentStamina=d.stamina;
        d.lastSentSpark=d.spark;d.lastSentMana=d.mana;d.lastSentManaCapacity=d.manaCapacity;d.lastSentManaActive=d.manaActive;
        });
    }
    public static void stamina(ServerPlayer p) {if(!p.isRemoved())PlayerProgressionProvider.get(p).ifPresent(d->{send(p,new SyncStamina(d.stamina,d.exhausted));d.lastSentStamina=d.stamina;});}
    public static void resources(ServerPlayer p){if(!p.isRemoved())PlayerProgressionProvider.get(p).ifPresent(d->{send(p,new SyncResources(d.spark,d.mana,d.manaCapacity,d.manaActive));d.lastSentSpark=d.spark;d.lastSentMana=d.mana;d.lastSentManaCapacity=d.manaCapacity;d.lastSentManaActive=d.manaActive;});}
    public static void levelUp(ServerPlayer p,int level,int points) { send(p,new LevelUp(level,points)); }
    public static void recipeUnlocked(ServerPlayer p,Component name) { /* Kept as a silent compatibility entry point. */ }
    /** Forge's built-in config handshake runs only on login; resend after live server reloads. */
    public static void rules(ServerPlayer p) {
        net.neoforged.fml.config.ModConfigs.getConfigSet(net.neoforged.fml.config.ModConfig.Type.SERVER).stream()
                .filter(c->c.getModId().equals("rpgprogression")).findFirst().ifPresent(c->
                    send(p,new SyncRules(new com.electronwill.nightconfig.toml.TomlWriter().writeToString(c.getLoadedConfig().config()))));
    }
    private static void send(ServerPlayer p,CustomPacketPayload packet) {
        // Optional skills can call resource setters during capability deserialization,
        // before the login connection exists. Login synchronization sends the final state.
        if(p.connection!=null&&!p.isRemoved())PacketDistributor.sendToPlayer(p,packet);
    }
    public record SpendPoint(int attribute) implements CustomPacketPayload {
        public static final Type<SpendPoint> TYPE=payloadType("spend_point");
        @Override public Type<SpendPoint> type(){return TYPE;}
    }
    public record RequestData() implements CustomPacketPayload {
        public static final Type<RequestData> TYPE=payloadType("request_data");
        @Override public Type<RequestData> type(){return TYPE;}
    }
    public record SyncProgression(CompoundTag data) implements CustomPacketPayload {
        public static final Type<SyncProgression> TYPE=payloadType("sync_progression");
        @Override public Type<SyncProgression> type(){return TYPE;}
    }
    public record SyncStamina(double current,boolean exhausted) implements CustomPacketPayload {
        public static final Type<SyncStamina> TYPE=payloadType("sync_stamina");
        @Override public Type<SyncStamina> type(){return TYPE;}
    }
    public record LevelUp(int level,int points) implements CustomPacketPayload {
        public static final Type<LevelUp> TYPE=payloadType("level_up");
        @Override public Type<LevelUp> type(){return TYPE;}
    }
    public record RecipeUnlock(Component name) implements CustomPacketPayload {
        public static final Type<RecipeUnlock> TYPE=payloadType("recipe_unlock");
        @Override public Type<RecipeUnlock> type(){return TYPE;}
    }
    public record SyncRules(String toml) implements CustomPacketPayload {
        public static final Type<SyncRules> TYPE=payloadType("sync_rules");
        @Override public Type<SyncRules> type(){return TYPE;}
    }
    public record SyncResources(double spark,double mana,double manaCapacity,boolean manaActive) implements CustomPacketPayload {
        public static final Type<SyncResources> TYPE=payloadType("sync_resources");
        @Override public Type<SyncResources> type(){return TYPE;}
    }
    public record RequestTree() implements CustomPacketPayload {
        public static final Type<RequestTree> TYPE=payloadType("request_tree");
        @Override public Type<RequestTree> type(){return TYPE;}
    }
    public record TreeSnapshot(CompoundTag data) implements CustomPacketPayload {
        public static final Type<TreeSnapshot> TYPE=payloadType("tree_snapshot");
        @Override public Type<TreeSnapshot> type(){return TYPE;}
    }
    public record UnlockSkill(String id) implements CustomPacketPayload {
        public static final Type<UnlockSkill> TYPE=payloadType("unlock_skill");
        @Override public Type<UnlockSkill> type(){return TYPE;}
    }
    public record EquipSkill(String id,int slot) implements CustomPacketPayload {
        public static final Type<EquipSkill> TYPE=payloadType("equip_skill");
        @Override public Type<EquipSkill> type(){return TYPE;}
    }
    public record SkintAction(int action,java.util.UUID token,java.util.UUID target) implements CustomPacketPayload {
        public static final Type<SkintAction> TYPE=payloadType("skint_action");
        @Override public Type<SkintAction> type(){return TYPE;}
    }
    public record SkintSnapshot(CompoundTag data) implements CustomPacketPayload {
        public static final Type<SkintSnapshot> TYPE=payloadType("skint_snapshot");
        @Override public Type<SkintSnapshot> type(){return TYPE;}
    }
    private ModNetworking() {}
}
