package dev.rpgprogression.animation;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Numeric Blockbench keyframes, independent of a player renderer or optional animation library. */
public final class SkintAnimation {
    public static final int TICKS=28;
    public static final double SECONDS=1.4;
    public static final List<String> BONES=List.of("body","torso","head","right_arm","left_arm","right_leg","left_leg");
    public record Transform(double x,double y,double z,double rx,double ry,double rz) {
        public double[] values(){return new double[]{x,y,z,rx,ry,rz};}
    }
    public record Pose(List<Transform> bones) {
        public Pose {bones=List.copyOf(bones);if(bones.size()!=BONES.size())throw new IllegalArgumentException("Invalid bone count");}
        public Transform bone(String name){return bones.get(BONES.indexOf(name));}
    }
    private record Key(double time,double[] value,boolean curved){}
    private record Track(List<Key> keys) {
        double value(double time,int axis){
            if(time<=keys.getFirst().time)return keys.getFirst().value[axis];
            if(time>=keys.getLast().time)return keys.getLast().value[axis];
            int i=0;while(keys.get(i+1).time<time)i++;
            var a=keys.get(i);var b=keys.get(i+1);double t=(time-a.time)/(b.time-a.time);
            if(!a.curved&&!b.curved)return a.value[axis]+(b.value[axis]-a.value[axis])*t;
            // Match Blockbench's uniform Catmull-Rom spline with repeated endpoint controls.
            double v1=a.value[axis],v2=b.value[axis];
            double v0=i>0?keys.get(i-1).value[axis]:v1;
            double v3=i+2<keys.size()?keys.get(i+2).value[axis]:v2;
            return .5*((2*v1)+(-v0+v2)*t+(2*v0-5*v1+4*v2-v3)*t*t+(-v0+3*v1-3*v2+v3)*t*t*t);
        }
    }
    private record Bone(Track position,Track rotation) {
        Transform sample(double time){return new Transform(position.value(time,0),position.value(time,1),position.value(time,2),rotation.value(time,0),rotation.value(time,1),rotation.value(time,2));}
    }
    public static final class Clip {
        private final double length;
        private final List<Bone> bones;
        private Clip(JsonObject json){
            length=json.get("length").getAsDouble();var list=new ArrayList<Bone>();
            for(String name:BONES){var bone=json.getAsJsonObject("bones").getAsJsonObject(name);list.add(new Bone(track(bone.getAsJsonArray("position")),track(bone.getAsJsonArray("rotation"))));}
            bones=List.copyOf(list);
        }
        public double length(){return length;}
        public Pose sample(double time){double t=Math.clamp(time,0,length);return new Pose(bones.stream().map(b->b.sample(t)).toList());}
    }
    public static final Clip SIT,STAND;
    static {
        try(var input=SkintAnimation.class.getResourceAsStream("/assets/rpgprogression/animations/skint_player.json")){
            if(input==null)throw new IOException("Missing skint keyframes");
            var clips=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("clips");
            SIT=new Clip(clips.getAsJsonObject("sit"));STAND=new Clip(clips.getAsJsonObject("stand"));
        }catch(IOException error){throw new ExceptionInInitializerError(error);}
    }
    private static Track track(JsonArray rows){
        var keys=new ArrayList<Key>();for(var row:rows){var key=row.getAsJsonObject();var value=key.getAsJsonArray("value");keys.add(new Key(key.get("time").getAsDouble(),new double[]{value.get(0).getAsDouble(),value.get(1).getAsDouble(),value.get(2).getAsDouble()},key.get("interpolation").getAsString().equals("catmullrom")));}
        if(keys.isEmpty())throw new IllegalArgumentException("Empty animation track");return new Track(List.copyOf(keys));
    }
    /** Correct only the initial difference, so interrupted/reopened animations start without a pose jump. */
    public static Pose play(boolean standing,double seconds,Pose from){
        var clip=standing?STAND:SIT;var current=clip.sample(seconds);var initial=clip.sample(0);
        double t=Math.clamp(seconds/SECONDS,0,1),weight=1-t*t*(3-2*t);var bones=new ArrayList<Transform>();
        for(int i=0;i<BONES.size();i++){
            double[] now=current.bones.get(i).values(),a=from.bones.get(i).values(),b=initial.bones.get(i).values();
            for(int j=0;j<6;j++)now[j]+=(a[j]-b[j])*weight;
            bones.add(new Transform(now[0],now[1],now[2],now[3],now[4],now[5]));
        }
        return new Pose(bones);
    }
    private SkintAnimation(){}
}
