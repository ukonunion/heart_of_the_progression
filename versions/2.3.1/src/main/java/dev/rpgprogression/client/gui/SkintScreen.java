package dev.rpgprogression.client.gui;

import dev.rpgprogression.client.ClientSkintData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** The initial interaction menu leaves the world and its side camera visible. */
public final class SkintScreen extends Screen {
    private float scale;private double left,top;
    public SkintScreen(){super(SkintWidgets.tr("title"));}
    @Override protected void init(){
        scale=Math.min(width/640f,height/360f);left=(width-640*scale)/2;top=(height-360*scale)/2;
        addRenderableWidget(new SkintWidgets.Action(20,100,182,25,SkintWidgets.tr("progression"),b->ClientSkintData.action(0,null),font));
        addRenderableWidget(new SkintWidgets.Action(20,137,182,25,SkintWidgets.tr("travel"),b->minecraft.setScreen(new SkintTravelScreen(this)),font));
        addRenderableWidget(new SkintWidgets.Action(20,174,182,25,SkintWidgets.tr("exit"),b->onClose(),font));
    }
    @Override public void renderBackground(GuiGraphics g,int mouseX,int mouseY,float partial){}
    @Override public void tick(){if(!ClientSkintData.allowed())minecraft.setScreen(null);}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        for(int x=0;x<width*.5;x++){int alpha=(int)(190*Math.pow(1-x/(width*.5),1.6));g.fill(x,0,x+1,height,alpha<<24);}
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        var name=ClientSkintData.origin()==null?title:SkintWidgets.pointName(ClientSkintData.origin());
        g.fill(12,25,Math.min(318,28+font.width(name)),51,0x70433D33);
        SkintWidgets.text(g,font,name,21,34,286,0xFFFFBB35);
        super.render(g,(int)((mouseX-left)/scale),(int)((mouseY-top)/scale),partial);g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked((x-left)/scale,(y-top)/scale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased((x-left)/scale,(y-top)/scale,button);}
    @Override public void onClose(){ClientSkintData.close();minecraft.setScreen(null);}
    @Override public boolean isPauseScreen(){return false;}
}
