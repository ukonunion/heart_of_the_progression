package dev.rpgprogression.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.HashMap;

public final class TexturedMeters {
    public static final int WIDTH=79,HEALTH_HEIGHT=9,FOOD_HEIGHT=9,STAMINA_HEIGHT=10;
    public static final int HUNGER=0xFFE57E45,HUNGER_EFFECT=0xFFA76461,STAMINA=0xFFE8B12D,ANIMAL_HEALTH=0xFFDA662C,OXYGEN=0xFF20ACF4;
    private static final Map<String,ResourceLocation> textures=new HashMap<>();
    public static void draw(GuiGraphics g,String name,int center,int y,int height,double ratio,int color){
        draw(g,name,center,y,height,ratio,color,1);
    }
    public static void draw(GuiGraphics g,String name,int center,int y,int height,double ratio,int color,float opacity){
        opacity=Math.max(0,Math.min(1,opacity));if(opacity<=0)return;
        int left=center-WIDTH/2,fillY=y+(height==9?4:5);
        g.fill(left+3,fillY,left+76,fillY+2,alpha(0xCC181419,opacity));
        int filled=CenteredFill.pixels(73,ratio),fillLeft=center-filled/2;
        if(filled>0)g.fill(fillLeft,fillY,fillLeft+filled,fillY+2,alpha(color,opacity));
        // Flush queued solid geometry before the PNG. The colored lines always remain behind it.
        g.flush();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,opacity);
        ResourceLocation texture=textures.computeIfAbsent(name,n->ResourceLocation.fromNamespaceAndPath("rpgprogression","textures/hud/"+n+".png"));
        // The normal HP PNGs retain a transparent 80th column; the visible frame is 79 px.
        int sourceWidth=name.equals("hp/survival/empty")||name.equals("hp/hardcore/main_hc")?80:79;
        g.blit(texture,left,y,0,0,sourceWidth,height,sourceWidth,height);
        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.disableBlend();
    }
    private static int alpha(int color,float opacity){return (Math.round((color>>>24)*opacity)<<24)|(color&0xFFFFFF);}
    private TexturedMeters(){}
}
