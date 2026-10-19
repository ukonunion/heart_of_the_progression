package dev.rpgprogression.client;

import net.minecraft.client.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** A client-only view; never moves, rotates or changes the pose of the real player. */
public final class SkintCamera {
    public record View(Vec3 position,float yaw,float pitch){}
    private static CameraType previous;
    private static View view;
    public static void begin(){
        if(previous!=null)return;var mc=Minecraft.getInstance();if(mc.player==null||ClientSkintData.origin()==null)return;
        previous=mc.options.getCameraType();mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        var player=mc.player;var crystal=Vec3.atBottomCenterOf(ClientSkintData.origin().pos());
        var direction=player.position().subtract(crystal).multiply(1,0,1);
        if(direction.lengthSqr()<.04)direction=Vec3.directionFromRotation(0,player.getYRot()).scale(-1);
        direction=direction.normalize();var side=new Vec3(direction.z,0,-direction.x);
        var middle=player.position().add(crystal).scale(.5);
        var target=middle.add(direction.scale(.65)).add(0,.85,0);
        var position=middle.add(side.scale(4.2)).add(direction.scale(2.4)).add(0,1.8,0);
        // Stop short of walls, including at the corners of the camera's near plane.
        double fraction=1;var delta=position.subtract(target);
        for(int i=0;i<8;i++){
            var inset=new Vec3((i&1)==0?-.12:.12,(i&2)==0?-.12:.12,(i&4)==0?-.12:.12);
            var hit=mc.level.clip(new ClipContext(target.add(inset),position.add(inset),ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,player));
            if(hit.getType()!=HitResult.Type.MISS)fraction=Math.min(fraction,Math.max(0,hit.getLocation().distanceTo(target.add(inset))-.2)/delta.length());
        }
        position=target.add(delta.scale(fraction));var look=target.subtract(position);
        view=new View(position,(float)Math.toDegrees(Math.atan2(-look.x,look.z)),(float)-Math.toDegrees(Math.atan2(look.y,look.horizontalDistance())));
    }
    public static View view(){return ClientSkintData.active()&&ClientSkintData.managed(Minecraft.getInstance().screen)?view:null;}
    public static boolean active(){return previous!=null;}
    public static void end(){if(previous!=null)Minecraft.getInstance().options.setCameraType(previous);previous=null;view=null;}
    private SkintCamera(){}
}
