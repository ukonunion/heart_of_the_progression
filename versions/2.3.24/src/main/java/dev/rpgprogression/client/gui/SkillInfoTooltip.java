package dev.rpgprogression.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

/** The supplied info.png: fixed ornaments, a fitted title and a scrollable inner body. */
public final class SkillInfoTooltip {
    public static final int WIDTH=276;
    private static final float SCALE=WIDTH/176f;
    private static final int HEADER=Math.round(46*SCALE),FOOTER=Math.round(14*SCALE);
    private static final int MARGIN=(int)Math.ceil(12*SCALE),TEXT_WIDTH=WIDTH-2*MARGIN-6,LINE=10;
    private final Font font;
    private final Component title;
    private final List<FormattedCharSequence> lines=new ArrayList<>();
    private final List<SkillTreeActionButton> buttons;
    private final int middle,rows,height;
    private int x,y,scroll;
    public SkillInfoTooltip(Font font,Component title,List<Component> body,List<SkillTreeActionButton> buttons){
        this.font=font;this.title=title;this.buttons=List.copyOf(buttons);
        for(var part:body){if(part.getString().isBlank())lines.add(FormattedCharSequence.EMPTY);else lines.addAll(font.split(part,TEXT_WIDTH));}
        int actions=buttons.isEmpty()?0:buttons.size()*22+4;
        middle=Math.min(268-HEADER-FOOTER,Math.max(Math.round(22*SCALE),lines.size()*LINE+8+actions));
        rows=Math.max(1,(middle-8-actions)/LINE);height=HEADER+middle+FOOTER;
    }
    public List<SkillTreeActionButton> buttons(){return buttons;}
    public int x(){return x;}
    public int y(){return y;}
    public int height(){return height;}
    public int scroll(){return scroll;}
    public int maximumScroll(){return Math.max(0,lines.size()-rows);}
    public void scrollTo(int value){scroll=Math.max(0,Math.min(maximumScroll(),value));}
    public void scrollBy(double amount){scrollTo(scroll-(int)Math.signum(amount)*2);}
    public boolean contains(double mx,double my){return mx>=x&&mx<x+WIDTH&&my>=y&&my<y+height;}
    public void place(double nodeX,double nodeY){
        // A 40-pixel gap accommodates even a node at the maximum 1.75x tree zoom.
        x=(int)Math.round(nodeX+40+WIDTH<=636?nodeX+40:nodeX-40-WIDTH);
        x=Math.max(4,Math.min(636-WIDTH,x));y=Math.max(50,Math.min(327-height,(int)Math.round(nodeY-28)));
        int actionY=y+HEADER+middle-4-buttons.size()*22;
        for(var button:buttons){button.setX(x+MARGIN);button.setY(actionY);actionY+=22;}
    }
    public void render(GuiGraphics g,int mouseX,int mouseY,float partial,double left,double top,float screenScale){
        g.pose().pushPose();g.pose().translate(0,0,500);g.flush();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,1);
        var texture=GuiTextures.texture("info");
        // Stretch only the straight middle; the title, raised rails and ornaments keep their proportions.
        g.blit(texture,x,y,WIDTH,HEADER,0,0,176,46,176,82);
        g.blit(texture,x,y+HEADER,WIDTH,middle,0,46,176,22,176,82);
        g.blit(texture,x,y+HEADER+middle,WIDTH,FOOTER,0,68,176,14,176,82);
        RenderSystem.disableBlend();
        int titleWidth=Math.round(110*SCALE);float fitted=Math.min(1,titleWidth/(float)Math.max(1,font.width(title)));
        g.pose().pushPose();g.pose().translate(x+WIDTH/2f,y+Math.round(18*SCALE),0);g.pose().scale(fitted,fitted,1);
        g.drawString(font,title,-font.width(title)/2,0,0xFFD3BA80,false);g.pose().popPose();
        int textX=x+MARGIN,textY=y+HEADER+4;
        g.enableScissor((int)Math.ceil(left+textX*screenScale),(int)Math.ceil(top+textY*screenScale),
                (int)Math.floor(left+(textX+TEXT_WIDTH)*screenScale),(int)Math.floor(top+(textY+rows*LINE)*screenScale));
        for(int row=0;row<rows&&row+scroll<lines.size();row++)g.drawString(font,lines.get(row+scroll),textX,textY+row*LINE,0xFFE8E2D4,false);
        g.disableScissor();
        if(maximumScroll()>0){
            int track=rows*LINE,thumb=Math.max(9,track*rows/lines.size()),offset=(track-thumb)*scroll/maximumScroll();
            g.fill(x+WIDTH-MARGIN-3,textY,x+WIDTH-MARGIN-1,textY+track,0xFF51412B);
            g.fill(x+WIDTH-MARGIN-3,textY+offset,x+WIDTH-MARGIN-1,textY+offset+thumb,0xFFD3BA80);
        }
        for(var button:buttons)button.render(g,mouseX,mouseY,partial);
        g.pose().popPose();
    }
}
