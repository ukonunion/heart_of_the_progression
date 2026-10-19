package dev.rpgprogression.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Tooltip placement uses the same logical coordinate system as our scaled screens. */
final class GuiTooltips {
    static void show(GuiGraphics g,Font font,List<Component> text,int mouseX,int mouseY,int left,int top,int right,int bottom,boolean avoidNode){
        var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();
        for(var part:text)lines.addAll(font.split(part,Math.min(210,right-left-12)));
        g.renderTooltip(font,lines,(screenWidth,screenHeight,x,y,width,height)->{
            int px=x+12,py=y-12;
            if(px+width>right-4){
                px=x-width-12;
                if(avoidNode&&px<left+4){px=x-width/2;py=y-height-24;if(py<top+4)py=y+24;}
            }
            return new org.joml.Vector2i(Math.max(left+4,Math.min(right-width-4,px)),Math.max(top+4,Math.min(bottom-height-4,py)));
        },mouseX,mouseY);
    }
    private GuiTooltips(){}
}
