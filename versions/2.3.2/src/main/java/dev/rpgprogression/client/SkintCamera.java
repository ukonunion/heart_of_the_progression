package dev.rpgprogression.client;

import net.minecraft.client.*;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** A client-only view; never moves, rotates or changes the pose of the real player. */
public final class SkintCamera {
    public record View(Vec3 position,float yaw,float pitch,float roll){}
    public static final long TRANSITION_NANOS=700_000_000L;
    private static CameraType previous;
    private static View view,start;
    private static Vec3 focus;
    private static long began;
    public static void begin(){
        if(previous!=null)return;var mc=Minecraft.getInstance();if(mc.player==null||ClientSkintData.origin()==null)return;
        var camera=mc.gameRenderer.getMainCamera();
        start=new View(camera.getPosition(),camera.getYRot(),camera.getXRot(),camera.getRoll());
        previous=mc.options.getCameraType();mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        var player=mc.player;var crystal=Vec3.atBottomCenterOf(ClientSkintData.origin().pos());
        var direction=player.position().subtract(crystal).multiply(1,0,1);
        if(direction.lengthSqr()<.04)direction=Vec3.directionFromRotation(0,player.getYRot()).scale(-1);
        direction=direction.normalize();var side=new Vec3(direction.z,0,-direction.x);
        var middle=player.position().add(crystal).scale(.5);
        var target=middle.add(direction.scale(.65)).add(0,.85,0);
        var position=middle.add(side.scale(4.2)).add(direction.scale(2.4)).add(0,1.8,0);
        focus=target;position=clip(position);var look=target.subtract(position);
        view=new View(position,(float)Math.toDegrees(Math.atan2(-look.x,look.z)),(float)-Math.toDegrees(Math.atan2(look.y,look.horizontalDistance())),0);
        began=System.nanoTime();
    }
    private static Vec3 clip(Vec3 position){
        var mc=Minecraft.getInstance();var delta=position.subtract(focus);double length=delta.length();if(length<1e-5)return position;
        double fraction=1;
        // Keep the near plane in front of walls throughout the movement as well as at its endpoint.
        for(int i=0;i<8;i++){
            var inset=new Vec3((i&1)==0?-.12:.12,(i&2)==0?-.12:.12,(i&4)==0?-.12:.12);
            var hit=mc.level.clip(new ClipContext(focus.add(inset),position.add(inset),ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player));
            if(hit.getType()!=HitResult.Type.MISS)fraction=Math.min(fraction,Math.max(0,hit.getLocation().distanceTo(focus.add(inset))-.2)/length);
        }
        return focus.add(delta.scale(fraction));
    }
    public static boolean settled(){return view!=null&&System.nanoTime()-began>=TRANSITION_NANOS;}
    public static View view(){
        if(!ClientSkintData.active()||!ClientSkintData.managed(Minecraft.getInstance().screen)||view==null)return null;
        double t=Math.clamp((System.nanoTime()-began)/(double)TRANSITION_NANOS,0,1);if(t>=1)return view;
        // Smootherstep has zero velocity and acceleration at both ends; timing does not depend on FPS.
        double ease=t*t*t*(t*(t*6-15)+10);
        return new View(clip(start.position().lerp(view.position(),ease)),
            Mth.rotLerp((float)ease,start.yaw(),view.yaw()),Mth.lerp((float)ease,start.pitch(),view.pitch()),Mth.rotLerp((float)ease,start.roll(),view.roll()));
    }
    public static boolean active(){return previous!=null;}
    public static void end(){if(previous!=null)Minecraft.getInstance().options.setCameraType(previous);previous=null;view=null;start=null;focus=null;began=0;}
    private SkintCamera(){}
}
