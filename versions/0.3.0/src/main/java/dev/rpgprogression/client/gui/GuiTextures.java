package dev.rpgprogression.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** The menu background is the only custom texture; meters and controls use primitives. */
public final class GuiTextures {
    public static ResourceLocation texture(String name) {
        return new ResourceLocation("rpgprogression", "textures/gui/" + name + ".png");
    }
    public static void draw(GuiGraphics g,String name,int x,int y,int w,int h,int sourceW,int sourceH) {
        RenderSystem.enableBlend();
        g.blit(texture(name),x,y,w,h,0,0,sourceW,sourceH,sourceW,sourceH);
        RenderSystem.disableBlend();
    }
    public static void bar(GuiGraphics g,String name,int x,int y,int width,int height,double ratio) {
        g.fill(x,y,x+width,y+height,0xFF383429);
        int color=switch(name){case "health"->0xFFB45B60;case "exhausted"->0xFFAD5148;case "xp"->0xFFAABD69;default->0xFFD3BA80;};
        int filled=(int)(width*Math.max(0,Math.min(1,ratio)));
        if(filled>0)g.fill(x,y,x+filled,y+height,color);
    }
    private GuiTextures() {}
}
