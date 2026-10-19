package dev.rpgprogression.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import dev.rpgprogression.client.gui.GuiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import java.io.IOException;
import java.util.*;

/** Fits the live spell icon to the transparent interior of the supplied mana frame. */
public final class ManaIconMask {
    private record Span(int y,int left,int right){}
    private static List<Span> spans;
    public static void invalidate(){spans=null;}
    private static List<Span> interior(){
        if(spans!=null)return spans;
        var result=new ArrayList<Span>();
        try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(GuiTextures.texture("mana/mana_empty")).open();
            var image=NativeImage.read(stream)){
            int w=image.getWidth(),h=image.getHeight();
            if(w!=72||h!=72)throw new IOException("Expected a 72x72 mana frame");
            boolean[] inside=new boolean[w*h];var queue=new ArrayDeque<Integer>();
            if((image.getPixelRGBA(0,0)>>>24)==0){inside[0]=true;queue.add(0);}
            // Only the transparent component connected to the screen corner is interior;
            // transparent spaces between the orbs must keep showing the game world.
            while(!queue.isEmpty()){
                int i=queue.removeFirst(),x=i%w,y=i/w;
                for(int n:new int[]{x>0?i-1:-1,x+1<w?i+1:-1,y>0?i-w:-1,y+1<h?i+w:-1})
                    if(n>=0&&!inside[n]&&(image.getPixelRGBA(n%w,n/w)>>>24)==0){inside[n]=true;queue.addLast(n);}
            }
            for(int y=0;y<h;y++)for(int x=0;x<w;){
                if(!inside[y*w+x]){x++;continue;}
                int left=x;while(x<w&&inside[y*w+x])x++;
                result.add(new Span(y,left,x));
            }
        }catch(IOException error){LogUtils.getLogger().warn("Cannot read the mana icon mask",error);}
        return spans=List.copyOf(result);
    }
    public static void draw(GuiGraphics g,ResourceLocation icon){
        draw(g,icon,1);
    }
    public static void draw(GuiGraphics g,ResourceLocation icon,float opacity){
        var mask=interior();if(mask.isEmpty())return;
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,opacity);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderTexture(0,icon);
        var buffer=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);
        var pose=g.pose().last().pose();
        for(var row:mask){
            // A 16x16 icon becomes 48x48 without distorting its pixel grid. The
            // last texel is extended into the small right/bottom bulges of the frame.
            float v0=row.y<48?row.y/48f:15.5f/16,v1=row.y<48?(row.y+1)/48f:15.5f/16;
            int end=Math.min(row.right,48);
            if(row.left<end)quad(buffer,pose,row.left,row.y,end,row.y+1,row.left/48f,v0,end/48f,v1);
            if(row.right>48)quad(buffer,pose,Math.max(row.left,48),row.y,row.right,row.y+1,15.5f/16,v0,15.5f/16,v1);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();
    }
    private static void quad(BufferBuilder b,org.joml.Matrix4f pose,int x0,int y0,int x1,int y1,float u0,float v0,float u1,float v1){
        b.addVertex(pose,x0,y1,0).setUv(u0,v1);b.addVertex(pose,x1,y1,0).setUv(u1,v1);
        b.addVertex(pose,x1,y0,0).setUv(u1,v0);b.addVertex(pose,x0,y0,0).setUv(u0,v0);
    }
    private ManaIconMask(){}
}
