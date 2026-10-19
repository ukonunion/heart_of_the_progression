package dev.rpgprogression.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Supplied menu and button textures; HUD meters use primitives. */
public final class GuiTextures {
    public static ResourceLocation texture(String name) {
        return new ResourceLocation("rpgprogression", "textures/gui/" + name + ".png");
    }
    public static void draw(GuiGraphics g,String name,int x,int y,int w,int h,int sourceW,int sourceH) {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1,1,1,1);
        g.blit(texture(name),x,y,w,h,0,0,sourceW,sourceH,sourceW,sourceH);
        RenderSystem.disableBlend();
    }
    public static void skillButton(GuiGraphics g,int x,int y,int state){
        skillButton(g,x,y,state,40,40);
    }
    public static void skillButton(GuiGraphics g,int x,int y,int state,int width,int height){
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,1);
        g.blit(texture("skill_tree_buttons"),x,y,width,height,0,state*40,40,40,64,128);
        RenderSystem.disableBlend();
    }
    public static void bar(GuiGraphics g,String name,int x,int y,int width,int height,double ratio) {
        g.fill(x,y,x+width,y+height,0xFF383429);
        int color=switch(name){case "health"->0xFFB45B60;case "exhausted"->0xFFAD5148;case "xp"->0xFFAABD69;case "mana"->0xFF67BCCD;case "spark"->0xFFE3BF71;default->0xFFD3BA80;};
        int half=dev.rpgprogression.client.hud.CenteredFill.halfWidth(width,ratio),center=x+width/2;
        if(half>0)g.fill(center-half,y,center+half,y+height,color);
    }
    private GuiTextures() {}
}
