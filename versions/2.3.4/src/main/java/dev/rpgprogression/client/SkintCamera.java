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
    private static boolean returning;
    private static net.minecraft.world.level.Level cameraLevel;
    private static net.minecraft.world.entity.Entity owner;
    private static Vec3 returnOrigin;
    public static void begin(){
        if(previous!=null)return;var mc=Minecraft.getInstance();if(mc.player==null||ClientSkintData.origin()==null)return;
        cameraLevel=mc.level;owner=mc.player;
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
    private static Vec3 clip(Vec3 position){return clip(position,focus);}
    private static Vec3 clip(Vec3 position,Vec3 anchor){
        var mc=Minecraft.getInstance();var delta=position.subtract(anchor);double length=delta.length();if(length<1e-5)return position;
        double fraction=1;
        // Keep the near plane in front of walls throughout the movement as well as at its endpoint.
        for(int i=0;i<8;i++){
            var inset=new Vec3((i&1)==0?-.12:.12,(i&2)==0?-.12:.12,(i&4)==0?-.12:.12);
            var hit=mc.level.clip(new ClipContext(anchor.add(inset),position.add(inset),ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player));
            if(hit.getType()!=HitResult.Type.MISS)fraction=Math.min(fraction,Math.max(0,hit.getLocation().distanceTo(anchor.add(inset))-.2)/length);
        }
        return anchor.add(delta.scale(fraction));
    }
    public static boolean settled(){return !returning&&view!=null&&System.nanoTime()-began>=TRANSITION_NANOS;}
    public static View view(){
        if(returning||!ClientSkintData.active()||!ClientSkintData.managed(Minecraft.getInstance().screen)||view==null)return null;
        double t=Math.clamp((System.nanoTime()-began)/(double)TRANSITION_NANOS,0,1);if(t>=1)return view;
        // Smootherstep has zero velocity and acceleration at both ends; timing does not depend on FPS.
        double ease=t*t*t*(t*(t*6-15)+10);
        return new View(clip(start.position().lerp(view.position(),ease)),
            Mth.rotLerp((float)ease,start.yaw(),view.yaw()),Mth.lerp((float)ease,start.pitch(),view.pitch()),Mth.rotLerp((float)ease,start.roll(),view.roll()));
    }
    /** Used by Camera.setup to compute its normal, live first/third-person destination. */
    public static CameraType returnPerspective(){return returning?previous:null;}
    public static boolean returning(){return returning;}
    // Camera-angle listeners must see the original perspective while computing the live destination.
    public static void beforeSetup(){if(returning)Minecraft.getInstance().options.setCameraType(previous);}
    public static void afterSetup(){if(returning)Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);}
    public static void returnToPlayer(){
        if(previous==null||returning)return;var mc=Minecraft.getInstance();
        if(mc.player!=owner||mc.level!=cameraLevel||mc.player==null||!mc.player.isAlive()){end();return;}
        var camera=mc.gameRenderer.getMainCamera();
        start=new View(camera.getPosition(),camera.getYRot(),camera.getXRot(),camera.getRoll());
        returnOrigin=mc.player.position();began=System.nanoTime();returning=true;
    }
    public static View view(Camera vanilla){
        if(!returning)return view();var mc=Minecraft.getInstance();
        if(mc.player!=owner||mc.level!=cameraLevel||mc.player==null||!mc.player.isAlive()||mc.player.isRemoved()
                ||mc.player.position().distanceToSqr(returnOrigin)>64){end();return null;}
        double t=Math.clamp((System.nanoTime()-began)/(double)TRANSITION_NANOS,0,1);
        if(t>=1){end();return null;}
        // Vanilla supplies a fresh destination every frame, including movement, mouse look and wall clipping.
        var target=new View(vanilla.getPosition(),vanilla.getYRot(),vanilla.getXRot(),vanilla.getRoll());
        double ease=t*t*t*(t*(t*6-15)+10);
        return new View(clip(start.position().lerp(target.position(),ease),target.position()),
            Mth.rotLerp((float)ease,start.yaw(),target.yaw()),Mth.lerp((float)ease,start.pitch(),target.pitch()),Mth.rotLerp((float)ease,start.roll(),target.roll()));
    }
    public static boolean active(){return previous!=null;}
    /** Hard reset for teleportation, death and logout, also used when a return completes. */
    public static void end(){
        if(previous!=null)Minecraft.getInstance().options.setCameraType(previous);
        previous=null;view=null;start=null;focus=null;began=0;returning=false;cameraLevel=null;owner=null;returnOrigin=null;
    }
    private SkintCamera(){}
}
