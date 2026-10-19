package dev.rpgprogression.client;

import java.util.*;
import dev.rpgprogression.block.ModBlocks;
import dev.rpgprogression.client.gui.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.skint.SkintNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec3;

public final class ClientSkintData {
    private static UUID token;
    private static SkintNetwork.Point origin;
    private static List<SkintNetwork.Point> points=List.of();
    public static void install(){ModNetworking.clientSkint=packet->{
        var n=packet.data();var mc=Minecraft.getInstance();
        if(n.getInt("mode")==-1){clear();if(managed(mc.screen))mc.setScreen(null);return;}
        token=n.getUUID("token");origin=SkintNetwork.Point.read(n.getCompound("origin"));
        var received=new ArrayList<SkintNetwork.Point>();for(var entry:n.getList("points",10))received.add(SkintNetwork.Point.read((net.minecraft.nbt.CompoundTag)entry));points=List.copyOf(received);
        if(n.getInt("mode")==1)mc.setScreen(new CharacterScreen(new SkintScreen()));else mc.setScreen(new SkintScreen());
    };}
    public static boolean managed(Screen screen){return screen instanceof SkintScreen||screen instanceof CharacterScreen||screen instanceof SkillTreeScreen;}
    public static boolean active(){return token!=null;}
    public static boolean allowed(){
        var mc=Minecraft.getInstance();return active()&&origin!=null&&mc.player!=null&&mc.level!=null&&mc.player.isAlive()
            &&mc.level.dimension().equals(origin.dimension())&&mc.player.position().distanceToSqr(Vec3.atCenterOf(origin.pos()))<=36
            &&mc.level.getBlockState(origin.pos()).is(ModBlocks.TRAVELSKINT.get());
    }
    public static SkintNetwork.Point origin(){return origin;}
    public static List<SkintNetwork.Point> destinations(){return points.stream().filter(p->!p.id().equals(origin.id())).toList();}
    public static void action(int action,UUID destination){if(active())ModNetworking.CHANNEL.sendToServer(new ModNetworking.SkintAction(action,token,destination==null?new UUID(0,0):destination));}
    public static void close(){action(2,null);clear();}
    public static void clear(){token=null;origin=null;points=List.of();}
    private ClientSkintData(){}
}
