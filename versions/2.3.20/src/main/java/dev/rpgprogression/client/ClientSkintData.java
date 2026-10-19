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
    private static boolean busy;
    private static net.minecraft.nbt.CompoundTag arriving;
    private static int arrivalTicks;
    private static SkintNetwork.Point origin;
    private static List<SkintNetwork.Point> points=List.of();
    public static void install(){ModNetworking.clientSkint=packet->{
        var n=packet.data();var mc=Minecraft.getInstance();
        if(n.getInt("mode")==-1){resetInteraction();if(managed(mc.screen))mc.setScreen(null);SkintFade.recover();return;}
        if(n.getInt("mode")==5){
            if(token!=null&&token.equals(n.getUUID("token"))){readPoints(n);if(mc.screen instanceof SkintScreen hub)hub.renamed();if(mc.screen instanceof SkintTravelScreen travel)travel.refreshNames();}
            return;
        }
        if(n.getInt("mode")==3){
            resetInteraction();arriving=n.copy();arrivalTicks=0;if(managed(mc.screen))mc.setScreen(null);return;
        }
        if(n.getInt("mode")==4){if(token!=null&&token.equals(n.getUUID("token"))){SkintFade.begin();apply(n);}return;}
        if(n.getInt("mode")==0&&SkintFade.active())SkintFade.reveal();
        apply(n);
    };}
    private static void apply(net.minecraft.nbt.CompoundTag n){
        var mc=Minecraft.getInstance();
        busy=n.getInt("mode")==2||n.getInt("mode")==4;
        boolean fresh=token==null||!token.equals(n.getUUID("token"));
        token=n.getUUID("token");readPoints(n);
        if(fresh){SkintCamera.end();SkintCamera.begin();if(origin!=null)SkintPreviews.request(origin.id());}
        if(n.getInt("mode")==1)mc.setScreen(new CharacterScreen(new SkintScreen()));else mc.setScreen(new SkintScreen());
    }
    private static void readPoints(net.minecraft.nbt.CompoundTag n){
        origin=SkintNetwork.Point.read(n.getCompound("origin"));
        var received=new ArrayList<SkintNetwork.Point>();for(var entry:n.getList("points",10))received.add(SkintNetwork.Point.read((net.minecraft.nbt.CompoundTag)entry));points=List.copyOf(received);
    }
    public static void rename(String name){if(active()&&!busy)ModNetworking.CHANNEL.sendToServer(new ModNetworking.RenameSkint(token,name));}
    /** Teleport packets can precede the receiving-level screen; open only after that screen has finished. */
    public static void tick(){
        if(arriving==null)return;var mc=Minecraft.getInstance();
        if(++arrivalTicks>400){
            ModNetworking.CHANNEL.sendToServer(new ModNetworking.SkintAction(2,arriving.getUUID("token"),new UUID(0,0)));resetInteraction();SkintFade.recover();return;
        }
        if(mc.player==null||mc.level==null||mc.getOverlay()!=null||mc.screen!=null&&!managed(mc.screen))return;
        var point=SkintNetwork.Point.read(arriving.getCompound("origin"));
        if(!mc.level.dimension().equals(point.dimension())||mc.player.position().distanceToSqr(Vec3.atCenterOf(point.pos()))>36
                ||!mc.level.getBlockState(point.pos()).is(ModBlocks.TRAVELSKINT.get())
                ||!(mc.player.getVehicle() instanceof dev.rpgprogression.skint.SkintSeat seat)||seat.isRemoved()
                ||seat.position().distanceToSqr(Vec3.atCenterOf(point.pos()))>36)return;
        // The last rendered camera may still belong to the source dimension or source coordinates.
        var perspective=mc.options.getCameraType();
        mc.gameRenderer.getMainCamera().setup(mc.level,mc.player,!perspective.isFirstPerson(),perspective.isMirrored(),1);
        var tag=arriving;arriving=null;tag.putInt("mode",0);apply(tag);
    }
    public static boolean managed(Screen screen){return screen instanceof SkintScreen||screen instanceof SkintTravelScreen||screen instanceof CharacterScreen c&&!c.readOnly()||screen instanceof SkillTreeScreen t&&!t.readOnly();}
    public static boolean arriving(){return arriving!=null;}
    public static boolean busy(){return busy;}
    public static boolean active(){return token!=null;}
    public static boolean allowed(){
        var mc=Minecraft.getInstance();return active()&&origin!=null&&mc.player!=null&&mc.level!=null&&mc.player.isAlive()
            &&mc.level.dimension().equals(origin.dimension())&&mc.player.position().distanceToSqr(Vec3.atCenterOf(origin.pos()))<=36
            &&mc.level.getBlockState(origin.pos()).is(ModBlocks.TRAVELSKINT.get());
    }
    public static SkintNetwork.Point origin(){return origin;}
    public static List<SkintNetwork.Point> destinations(){return points.stream().filter(p->!p.id().equals(origin.id())).toList();}
    public static void action(int action,UUID destination){if(active())ModNetworking.CHANNEL.sendToServer(new ModNetworking.SkintAction(action,token,destination==null?new UUID(0,0):destination));}
    public static void close(){if(SkintFade.committed())return;action(2,null);resetInteraction();SkintFade.reveal();}
    public static void closeWithCameraReturn(){if(SkintFade.active())return;action(2,null);forget();SkintCamera.returnToPlayer();}
    private static void forget(){SkintPreviews.cancel();token=null;origin=null;points=List.of();busy=false;}
    private static void resetInteraction(){arriving=null;arrivalTicks=0;SkintCamera.end();forget();}
    public static void clear(){resetInteraction();SkintFade.clear();}
    private ClientSkintData(){}
}
