package dev.rpgprogression.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.rpgprogression.xp.ProgressionOrb;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Reuses the exact seven-pixel Skint-charged particle, without tinting or replacing it. */
public final class ProgressionOrbRenderer extends EntityRenderer<ProgressionOrb> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("rpgprogression","textures/particle/skint_charged.png");
    private static final RenderType LAYER=RenderType.entityTranslucent(TEXTURE);
    public ProgressionOrbRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.08f;shadowStrength=.3f;}
    @Override public ResourceLocation getTextureLocation(ProgressionOrb orb){return TEXTURE;}
    @Override public void render(ProgressionOrb orb,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.translate(0,.13+Math.sin((orb.tickCount+partial)*.07+orb.getId())*.02,0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());pose.mulPose(Axis.ZP.rotationDegrees((orb.tickCount+partial)*.8f));
        float size=orb.value()>=1000?.4f:orb.value()>=17?.32f:.24f;pose.scale(size,size,size);
        var vertex=buffers.getBuffer(LAYER);var matrix=pose.last();
        point(vertex,matrix,-.5f,-.5f,0,1);point(vertex,matrix,.5f,-.5f,1,1);point(vertex,matrix,.5f,.5f,1,0);point(vertex,matrix,-.5f,.5f,0,0);
        pose.popPose();super.render(orb,yaw,partial,pose,buffers,light);
    }
    private static void point(VertexConsumer vertex,PoseStack.Pose pose,float x,float y,float u,float v){
        vertex.addVertex(pose,x,y,0).setColor(255,255,255,255).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose,0,1,0);
    }
}
