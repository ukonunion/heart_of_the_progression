package dev.rpgprogression.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Replaceable art under textures/gui; resource packs can override every file. */
public final class GuiTextures {
    public static ResourceLocation texture(String name) {
        return new ResourceLocation("rpgprogression", "textures/gui/" + name + ".png");
    }
    public static void draw(GuiGraphics g,String name,int x,int y,int w,int h,int sourceW,int sourceH) {
        RenderSystem.enableBlend();
        g.blit(texture(name),x,y,w,h,0,0,sourceW,sourceH,sourceW,sourceH);
        RenderSystem.disableBlend();
    }
    public static void icon(GuiGraphics g,String name,int x,int y,int size) {
        draw(g,"icons/"+name,x,y,size,size,16,16);
    }
    public static void bar(GuiGraphics g,String name,int x,int y,int width,int height,double ratio) {
        draw(g,"bars/track",x,y,width,height,64,5);
        int filled=(int)(width*Math.max(0,Math.min(1,ratio)));
        if(filled>0)draw(g,"bars/"+name,x,y,filled,height,64,5);
    }
    private GuiTextures() {}
}
