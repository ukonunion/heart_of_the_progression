package dev.rpgprogression.verification;

import com.google.gson.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.requirements.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

@EventBusSubscriber(modid="rpgprogression")
public final class LockpickRequirementChecks {
    private static final String ROOT="it.hurts.shatterbyte.clavis.common.";
    private static int serverAge;private static boolean done;
    public static void check(JsonArray checks,boolean ok,String text){SkintTravelChecks.check(checks,ok,text);}
    public static ItemStack pick(){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("clavis:lock_pick")),2);}
    private static Object addLock(ServerPlayer p,BlockPos pos)throws Exception {
        p.serverLevel().getChunk(pos);p.serverLevel().setBlock(pos,Blocks.CHEST.defaultBlockState(),3);
        ((RandomizableContainerBlockEntity)p.serverLevel().getBlockEntity(pos)).setLootTable(BuiltInLootTables.SIMPLE_DUNGEON,2331);
        var boxClass=Class.forName(ROOT+"data.Box");var box=boxClass.getConstructor(int.class,int.class,int.class,int.class,int.class,int.class).newInstance(pos.getX(),pos.getY(),pos.getZ(),pos.getX(),pos.getY(),pos.getZ());
        var lockClass=Class.forName(ROOT+"data.Lock");var lock=lockClass.getConstructor(UUID.class,boxClass,float.class,long.class,boolean.class,List.class,ResourceLocation.class).newInstance(UUID.randomUUID(),box,.5f,2331L,false,List.of(),ResourceLocation.parse("clavis:gear"));
        Class.forName(ROOT+"LockManager").getMethod("addLock",ServerLevel.class,lockClass).invoke(null,p.serverLevel(),lock);return lock;
    }
    private static boolean locked(ServerPlayer p,BlockPos pos)throws Exception {
        return !((List<?>)Class.forName(ROOT+"LockManager").getMethod("getLocksAt",ServerLevel.class,ServerPlayer.class,BlockPos.class).invoke(null,p.serverLevel(),p,pos)).isEmpty();
    }
    private static void removeLock(ServerPlayer p,Object lock)throws Exception{Class.forName(ROOT+"LockManager").getMethod("removeLock",ServerLevel.class,lock.getClass()).invoke(null,p.serverLevel(),lock);}
    private static void usePacket(ServerPlayer p,BlockPos pos,InteractionHand hand)throws Exception {
        var contextClass=Class.forName("dev.architectury.networking.NetworkManager$PacketContext");
        var context=Proxy.newProxyInstance(contextClass.getClassLoader(),new Class<?>[]{contextClass},(proxy,method,args)->switch(method.getName()){case "getPlayer"->p;case "toString"->"LockpickRequirementVerifier";default->null;});
        var packetClass=Class.forName(ROOT+"network.packet.CheckIfLockedPacket");var packet=packetClass.getConstructor(BlockPos.class,InteractionHand.class).newInstance(pos,hand);var handle=packetClass.getDeclaredMethod("handleServer",contextClass);handle.setAccessible(true);handle.invoke(packet,context);
    }
    private static void packetBoundary(ServerPlayer p,JsonArray checks,BlockPos pos,InteractionHand hand,int threshold)throws Exception {
        Object lock=addLock(p,pos);var data=ProgressionManager.get(p);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(hand,pick());
        try{
            data.set(RPGAttribute.AGILITY,threshold-1);usePacket(p,pos,hand);
            check(checks,locked(p,pos)&&p.getItemInHand(hand).getCount()==2,hand+": a direct native packet below Agility "+threshold+" neither unlocks nor consumes the pick");
            check(checks,((RandomizableContainerBlockEntity)p.serverLevel().getBlockEntity(pos)).getLootTable()!=null,hand+": a denied native packet does not generate or alter chest loot");
            data.set(RPGAttribute.AGILITY,threshold);usePacket(p,pos,hand);
            check(checks,!locked(p,pos)&&p.getItemInHand(hand).getCount()==1,hand+": exact Agility "+threshold+" permits native unlock and consumes one pick");
            check(checks,((RandomizableContainerBlockEntity)p.serverLevel().getBlockEntity(pos)).getLootTable()==null,hand+": successful native unlock retains original loot generation");
        }finally{removeLock(p,lock);p.serverLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);}
    }
    public static void run(ServerPlayer p,JsonArray checks)throws Exception {
        boolean present=net.neoforged.fml.ModList.get().isLoaded("clavis");check(checks,present==Boolean.getBoolean("rpgprogression.expectClavis"),"optional Clavis presence matches this test runtime");
        if(!present){check(checks,!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("clavis:lock_pick")),"without Clavis no optional item or packet class is required");check(checks,EquipmentRequirements.defaultsFor("minecraft:stick",0,0).isEmpty(),"unrelated ordinary items keep their existing requirements");return;}
        var data=ProgressionManager.get(p);var saved=data.save();var mode=p.gameMode.getGameModeForPlayer();var main=p.getMainHandItem().copy();var off=p.getOffhandItem().copy();var overrides=RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.AGILITY).get();boolean defaults=RPGConfig.DEFAULT_ITEM_REQUIREMENTS.get();
        try{
            p.setGameMode(GameType.SURVIVAL);RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(true);RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.AGILITY).set(List.of());
            check(checks,ItemRequirements.required(pick()).equals(Map.of(RPGAttribute.AGILITY,20)),"the actual registered Clavis lockpick requires only Agility 20 by default");
            data.set(RPGAttribute.AGILITY,19);check(checks,!ItemRequirements.allowed(p,pick())&&ItemRequirements.unmet(p,pick()).equals(Map.of(RPGAttribute.AGILITY,20)),"Agility 19 is denied and exposes the same missing requirement to tooltips");
            data.set(RPGAttribute.AGILITY,20);check(checks,ItemRequirements.allowed(p,pick())&&ItemRequirements.unmet(p,pick()).isEmpty(),"Agility 20 is allowed and clears the missing requirement");
            packetBoundary(p,checks,new BlockPos(230,220,230),InteractionHand.MAIN_HAND,20);
            packetBoundary(p,checks,new BlockPos(232,220,230),InteractionHand.OFF_HAND,20);
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.AGILITY).set(List.of("clavis:lock_pick=25"));check(checks,ItemRequirements.required(pick()).equals(Map.of(RPGAttribute.AGILITY,25)),"configured Agility overrides retain priority over the new default");packetBoundary(p,checks,new BlockPos(234,220,230),InteractionHand.MAIN_HAND,25);
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.AGILITY).set(List.of("clavis:lock_pick=0"));check(checks,ItemRequirements.required(pick()).isEmpty(),"an explicit zero override still removes the default requirement");
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.AGILITY).set(List.of());RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(false);check(checks,ItemRequirements.required(pick()).isEmpty(),"useDefaultRequirements=false retains its existing meaning");
            RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(true);data.set(RPGAttribute.AGILITY,19);p.setGameMode(GameType.CREATIVE);check(checks,ItemRequirements.allowed(p,pick()),"the established creative exemption is preserved");
        }finally{RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(defaults);RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.AGILITY).set(overrides);data.load(saved);p.setGameMode(mode);p.setItemInHand(InteractionHand.MAIN_HAND,main);p.setItemInHand(InteractionHand.OFF_HAND,off);}
    }
    public static void report(Path path,JsonArray checks,Throwable error)throws Exception {
        var json=new JsonObject();json.addProperty("version","2.3.31");json.addProperty("result",error==null&&checks.asList().stream().allMatch(c->c.getAsJsonObject().get("passed").getAsBoolean())?"PASSED":"FAILED");if(error!=null)json.addProperty("error",error.toString());json.add("checks",checks);Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(json));
    }
    @SubscribeEvent public static void server(ServerTickEvent.Post event){
        if(done||!Boolean.getBoolean("rpgprogression.lockpickServerSmoke")||++serverAge<10)return;done=true;var checks=new JsonArray();Throwable error=null;try{run(ConfigurationChecks.player(event.getServer().overworld()),checks);}catch(Throwable e){error=e;com.mojang.logging.LogUtils.getLogger().error("LOCKPICK_SERVER",e);}try{report(Path.of("lockpick-2.3.31-result.json"),checks,error);}catch(Exception e){throw new IllegalStateException(e);}event.getServer().halt(false);
    }
}
