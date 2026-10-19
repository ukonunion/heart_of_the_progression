package dev.rpgprogression.client;

import com.google.gson.*;
import java.util.*;

/** Numeric Blockbench channels and bind-pose meshes, without a dependency on an animation library. */
public final class SkintActivationModel {
    public record Face(int texture,float[] normal,float[][] vertices){}
    public record Bone(String name,float[] origin,float[] rotation,Map<String,List<Key>> channels,List<Face> faces,List<Bone> children){}
    public record Key(double time,float[] value,String interpolation){}
    public final float[] shift;
    public final float length;
    public final List<Bone> bones;
    public SkintActivationModel(JsonObject json){
        shift=vector(json.getAsJsonArray("shift"));length=json.get("length").getAsFloat();bones=new ArrayList<>();
        for(var node:json.getAsJsonArray("bones"))bones.add(readBone(node.getAsJsonObject()));
    }
    private static Bone readBone(JsonObject node){
        var channels=new HashMap<String,List<Key>>();
        for(var entry:node.getAsJsonArray("keys")){
            var key=entry.getAsJsonObject();var point=key.getAsJsonArray("data_points").get(0).getAsJsonObject();
            var value=new float[]{point.get("x").getAsFloat(),point.get("y").getAsFloat(),point.get("z").getAsFloat()};
            channels.computeIfAbsent(key.get("channel").getAsString(),k->new ArrayList<>()).add(new Key(key.get("time").getAsDouble(),value,key.get("interpolation").getAsString()));
        }
        channels.values().forEach(keys->keys.sort(Comparator.comparingDouble(Key::time)));
        var faces=new ArrayList<Face>();for(var entry:node.getAsJsonArray("faces")){
            var face=entry.getAsJsonObject();var vertices=new float[4][];for(int i=0;i<4;i++)vertices[i]=vector(face.getAsJsonArray("vertices").get(i).getAsJsonArray());
            faces.add(new Face(face.get("texture").getAsInt(),vector(face.getAsJsonArray("normal")),vertices));
        }
        var children=new ArrayList<Bone>();for(var child:node.getAsJsonArray("children"))children.add(readBone(child.getAsJsonObject()));
        return new Bone(node.get("name").getAsString(),vector(node.getAsJsonArray("origin")),vector(node.getAsJsonArray("rotation")),channels,faces,children);
    }
    private static float[] vector(JsonArray array){float[] out=new float[array.size()];for(int i=0;i<out.length;i++)out[i]=array.get(i).getAsFloat();return out;}
    public static float[] sample(Bone bone,String channel,double seconds){
        var keys=bone.channels.get(channel);float fallback=channel.equals("scale")?1:0;
        if(keys==null||keys.isEmpty())return new float[]{fallback,fallback,fallback};
        if(seconds<=keys.getFirst().time)return keys.getFirst().value.clone();
        if(seconds>=keys.getLast().time)return keys.getLast().value.clone();
        int i=0;while(i+1<keys.size()&&keys.get(i+1).time<seconds)i++;
        var a=keys.get(i);var b=keys.get(i+1);double t=(seconds-a.time)/(b.time-a.time);
        float[] out=new float[3];boolean cubic=a.interpolation.equals("catmullrom")||b.interpolation.equals("catmullrom");
        if(!cubic&&!a.interpolation.equals("linear")&&!a.interpolation.equals("step"))throw new IllegalArgumentException("Unsupported activation channel: "+a.interpolation);
        for(int axis=0;axis<3;axis++){
            double v=a.value[axis],w=b.value[axis];
            if(a.interpolation.equals("step"))out[axis]=(float)v;
            else if(cubic){
                double p=keys.get(Math.max(0,i-1)).value[axis],q=keys.get(Math.min(keys.size()-1,i+2)).value[axis];
                out[axis]=(float)(.5*(2*v+(w-p)*t+(2*p-5*v+4*w-q)*t*t+(-p+3*v-3*w+q)*t*t*t));
            }else out[axis]=(float)(v+(w-v)*t);
        }return out;
    }
}
