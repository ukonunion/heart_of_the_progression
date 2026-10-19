package dev.rpgprogression.verification;

import java.util.*;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.block.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skint.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Tests real world blocks, saved data and packet authorization without a client. */
public final class SkintTravelChecks {
    public static void check(JsonArray checks,boolean passed,String name){var row=new JsonObject();row.addProperty("check",name);row.addProperty("passed",passed);checks.add(row);com.mojang.logging.LogUtils.getLogger().info("SKINT_TRAVEL_CHECK {} {}",passed?"PASSED":"FAILED",name);}
    public static void point(ServerLevel level,BlockPos center){
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-1;y<=6;y++)level.setBlock(center.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        level.setBlock(center,ModBlocks.TRAVELSKINT.get().defaultBlockState(),3);
    }
    private static net.minecraft.server.level.ServerPlayer serverPlayer(ServerLevel level,GameProfile profile){
        // NeoForge FakePlayer explicitly refuses startRiding. Use a real ServerPlayer with its no-op transport.
        var transport=FakePlayerFactory.get(level,profile);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=transport.connection;return player;
    }
    public static JsonArray run(ServerLevel level,BlockPos base){
        var checks=new JsonArray();var a=base;var b=base.offset(12,0,0);point(level,a);point(level,b);
        var p=serverPlayer(level,new GameProfile(UUID.fromString("a5e95020-9343-4425-9d89-57d4e9030023"),"SkintVerifier"));
        var other=serverPlayer(level,new GameProfile(UUID.fromString("b5e95020-9343-4425-9d89-57d4e9030023"),"SkintOther"));
        p.setGameMode(GameType.SURVIVAL);p.setPos(a.getX()+.5,a.getY(),a.getZ()+2.5);p.setNoGravity(true);
        var data=ProgressionManager.get(p);data.initialize();data.discoveredSkints.clear();data.points=3;SkintManager.close(p,false);
        var network=SkintNetwork.get(level);var first=network.ensure(level,a);var second=network.ensure(level,b);
        check(checks,network.point(first.id()).equals(first)&&network.ensure(level,a).id().equals(first.id()),"placed points receive stable identities without duplicate registration");
        int strength=data.get(RPGAttribute.STRENGTH);ModNetworking.spend(p,RPGAttribute.STRENGTH.ordinal());
        check(checks,data.points==3&&data.get(RPGAttribute.STRENGTH)==strength,"forged attribute spending without an interaction session is rejected");
        check(checks,!SkintManager.canProgress(p),"standing near a skint alone does not open progression access");
        SkintManager.use(p,a);
        check(checks,SkintManager.canProgress(p)&&data.discoveredSkints.contains(first.id()),"interacting personally discovers the point and opens a server session");
        check(checks,!ProgressionManager.get(other).discoveredSkints.contains(first.id()),"discoveries are personal, not shared between players");
        check(checks,network.discovered(data.discoveredSkints).size()==1&&!SkintManager.travel(p,second.id()),"an undiscovered destination cannot be used by a forged travel request");
        SkintManager.use(p,a);check(checks,data.discoveredSkints.size()==1,"activating the same point twice does not duplicate it");
        var token=SkintManager.session(p).token();SkintManager.action(p,2,UUID.randomUUID(),first.id());
        check(checks,SkintManager.session(p)!=null&&SkintManager.session(p).token().equals(token),"a stale or forged session token cannot close the active session");
        p.tickCount+=10;ModNetworking.spend(p,RPGAttribute.STRENGTH.ordinal());
        check(checks,data.points==2&&data.get(RPGAttribute.STRENGTH)==strength+1,"authorized progression spending works through the skint");
        p.setPos(a.getX()+20,a.getY(),a.getZ());p.tickCount+=10;ModNetworking.spend(p,RPGAttribute.STRENGTH.ordinal());
        check(checks,data.points==2&&!SkintManager.canProgress(p),"moving away prevents further attribute purchases");
        SkintManager.tick(new PlayerTickEvent.Post(p));check(checks,SkintManager.session(p)==null,"the server invalidates a session after the player leaves");
        p.setPos(a.getX()+.5,a.getY(),a.getZ()+2.5);SkintManager.use(p,a);
        var roundTrip=new PlayerProgression();roundTrip.load(data.save());
        check(checks,roundTrip.discoveredSkints.equals(data.discoveredSkints)&&roundTrip.points==data.points&&roundTrip.get(RPGAttribute.STRENGTH)==data.get(RPGAttribute.STRENGTH),"player NBT preserves discoveries together with purchased attributes");
        var restored=SkintNetwork.load(network.save(new CompoundTag(),level.registryAccess()),level.registryAccess());
        check(checks,restored.point(first.id()).equals(first)&&restored.point(second.id()).equals(second),"world saved-data roundtrip preserves point identities and dimensions");
        level.setBlock(a.below(),Blocks.AIR.defaultBlockState(),3);
        check(checks,level.getBlockState(a).isAir()&&network.point(first.id())==null&&!SkintManager.canProgress(p),"support loss removes the world point and invalidates progression access");
        point(level,a);var replacement=network.ensure(level,a);
        check(checks,!replacement.id().equals(first.id())&&!data.discoveredSkints.contains(replacement.id()),"a replacement at the same coordinates requires a new discovery");
        p.setPos(a.getX()+3.5,a.getY()+.5,a.getZ()+.5);
        check(checks,SkintManager.manaMultiplier(p)==2,"mana aura includes the exact spherical three-block boundary");
        p.setPos(a.getX()+3.501,a.getY()+.5,a.getZ()+.5);
        check(checks,SkintManager.manaMultiplier(p)==1,"mana aura stops outside the three-block radius");
        var nearby=a.offset(0,0,2);level.setBlock(nearby,ModBlocks.TRAVELSKINT.get().defaultBlockState(),3);p.setPos(a.getX()+1.5,a.getY()+.5,a.getZ()+.5);
        check(checks,SkintManager.manaMultiplier(p)==2,"overlapping skints still give only a double mana multiplier");
        if(dev.rpgprogression.compat.IronsCompat.enabled()){
            data.set(RPGAttribute.SPARK_POTENTIAL,110);data.manaInitialized=true;data.mana=0;data.manaRegenCooldown=0;
            p.setPos(a.getX()+6.5,a.getY(),a.getZ()+.5);dev.rpgprogression.compat.IronsBridge.tick(p,data);double normal=data.mana;
            data.mana=0;data.manaRegenCooldown=0;p.setPos(a.getX()+1.5,a.getY()+.5,a.getZ()+.5);dev.rpgprogression.compat.IronsBridge.tick(p,data);
            check(checks,normal>0&&Math.abs(data.mana-normal*2)<1e-6,"actual Iron's mana regeneration is doubled near a skint");
        }
        p.setPos(a.getX()+.5,a.getY(),a.getZ()+2.5);SkintManager.use(p,a);data.discoveredSkints.add(second.id());
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=6;y++)if(x!=0||y!=0||z!=0)level.setBlock(b.offset(x,y,z),Blocks.BEDROCK.defaultBlockState(),3);
        var before=p.position();check(checks,BonfireTravel.attemptToPlaceNextToBonfire(b,level,p).isEmpty()&&!SkintManager.travel(p,second.id())&&p.position().equals(before),"a fully obstructed destination refuses travel without moving the player");
        check(checks,ModBlocks.TRAVELSKINT.get().defaultBlockState().getDestroySpeed(level,a)==-1,"the waypoint remains unbreakable by survival mining");
        SkintManager.close(p,false);return checks;
    }
    private SkintTravelChecks(){}
}
