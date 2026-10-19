package dev.rpgprogression.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.rpgprogression.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import java.io.*;

public final class TravelSkintRenderer implements BlockEntityRenderer<TravelSkintBlockEntity> {
    private static SkintActivationModel model;
    private static final ResourceLocation MODEL=ResourceLocation.fromNamespaceAndPath("rpgprogression","models/block/travelskint-animation.json");
    private static final RenderType[] MATERIALS=java.util.stream.IntStream.range(0,12).mapToObj(i->RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("rpgprogression","textures/block/travelskint/texture_"+i+".png"))).toArray(RenderType[]::new);
    public TravelSkintRenderer(BlockEntityRendererProvider.Context context){}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(TravelSkintBlockEntity entity){
        return new net.minecraft.world.phys.AABB(entity.getBlockPos()).inflate(.25,1.5,.25);
    }
    public static void invalidate(){model=null;}
    public static SkintActivationModel model(){
        if(model==null)try(var input=Minecraft.getInstance().getResourceManager().openAsReader(MODEL)){model=new SkintActivationModel(JsonParser.parseReader(input).getAsJsonObject());}
        catch(IOException error){throw new IllegalStateException("Cannot load Wayfinding Skint animation",error);}
        return model;
    }
    @Override public void render(TravelSkintBlockEntity entity,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=entity.getBlockState();if(!state.is(ModBlocks.TRAVELSKINT.get()))return;
        var data=model();float seconds=entity.animationSeconds(partialTick);
        pose.pushPose();pose.translate(.5,0,.5);
        int yaw=switch(state.getValue(TravelSkintBlock.FACING)){case EAST->90;case SOUTH->180;case WEST->270;default->0;};
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));pose.translate(-.5,0,-.5);
        pose.translate(data.shift[0],data.shift[1],data.shift[2]);
        for(var bone:data.bones)draw(bone,seconds,pose,buffers,light,overlay);
        pose.popPose();
    }
    private static void draw(SkintActivationModel.Bone bone,float seconds,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float[] pos=SkintActivationModel.sample(bone,"position",seconds),rot=SkintActivationModel.sample(bone,"rotation",seconds),scale=SkintActivationModel.sample(bone,"scale",seconds),origin=bone.origin();
        pose.pushPose();pose.translate(pos[0]/16f+origin[0],pos[1]/16f+origin[1],pos[2]/16f+origin[2]);
        float radians=(float)(Math.PI/180);pose.mulPose(new Quaternionf().rotationZYX((bone.rotation()[2]+rot[2])*radians,(bone.rotation()[1]+rot[1])*radians,(bone.rotation()[0]+rot[0])*radians));
        pose.scale(scale[0],scale[1],scale[2]);pose.translate(-origin[0],-origin[1],-origin[2]);
        var frame=pose.last();for(var face:bone.faces()){
            var consumer=buffers.getBuffer(MATERIALS[face.texture()]);float[] n=face.normal();
            for(var vertex:face.vertices())consumer.addVertex(frame,vertex[0],vertex[1],vertex[2]).setColor(1f,1f,1f,1f).setUv(vertex[3],vertex[4]).setOverlay(overlay).setLight(light).setNormal(frame,n[0],n[1],n[2]);
        }
        for(var child:bone.children())draw(child,seconds,pose,buffers,light,overlay);
        pose.popPose();
    }
}
