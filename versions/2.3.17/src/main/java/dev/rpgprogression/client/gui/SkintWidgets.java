package dev.rpgprogression.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.rpgprogression.skint.SkintNetwork;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class SkintWidgets {
    static Component tr(String key,Object...args){return Component.translatable("rpgprogression.skint."+key,args);}
    static Component pointName(SkintNetwork.Point point){if(!point.name().isEmpty())return Component.literal(point.name());var p=point.pos();return tr("point",p.getX(),p.getY(),p.getZ());}
    static void text(GuiGraphics g,Font font,Component text,int x,int y,int maxWidth,int color){
        float scale=Math.min(1,maxWidth/(float)Math.max(1,font.width(text)));
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);g.drawString(font,text,0,0,color,true);g.pose().popPose();
    }
    static final class Action extends Button {
        private final Font font;
        Action(int x,int y,int w,int h,Component text,OnPress press,Font font){super(x,y,w,h,text,press,DEFAULT_NARRATION);this.font=font;}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
            g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();float tint=active?1:.45f;RenderSystem.setShaderColor(tint,tint,tint,1);
            g.blit(GuiTextures.texture("tp_button"),getX(),getY(),width,height,0,0,115,14,115,14);
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();
            if(active&&isHoveredOrFocused())g.fill(getX()+2,getY()+1,getX()+width-2,getY()+height-1,0x30E3C585);
            int textWidth=Math.min(width-10,font.width(getMessage()));
            text(g,font,getMessage(),getX()+(width-textWidth)/2,getY()+(height-8)/2,width-10,active?0xFFEFE9DF:0xFF9C9386);
        }
    }
    private SkintWidgets(){}
}
