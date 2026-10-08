package dev.rpgprogression.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.HashMap;

public final class TexturedMeters {
    public static final int WIDTH=80,HEALTH_HEIGHT=9,OTHER_HEIGHT=10;
    public static final int HUNGER=0xFFE57E45,HUNGER_EFFECT=0xFFA76461,STAMINA=0xFFE8B12D;
    private static final Map<String,ResourceLocation> textures=new HashMap<>();
    public static void draw(GuiGraphics g,String name,int center,int y,int height,double ratio,int color){
        int left=center-WIDTH/2,fillY=y+(height==HEALTH_HEIGHT?4:5);
        g.fill(left+3,fillY,left+77,fillY+2,0xCC181419);
        int half=CenteredFill.halfWidth(74,ratio);
        if(half>0)g.fill(center-half,fillY,center+half,fillY+2,color);
        // Flush queued solid geometry before the PNG. The colored lines always remain behind it.
        g.flush();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,1);
        ResourceLocation texture=textures.computeIfAbsent(name,n->new ResourceLocation("rpgprogression","textures/hud/"+n+".png"));
        g.blit(texture,left,y,0,0,WIDTH,height,WIDTH,height);
        RenderSystem.disableBlend();
    }
    private TexturedMeters(){}
}
