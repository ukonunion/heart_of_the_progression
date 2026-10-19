package dev.rpgprogression.client.gui;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.rpgprogression.client.ClientSkintData;
import dev.rpgprogression.skint.SkintNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Original artwork; game text is drawn separately for Russian and English. */
public final class SkintScreen extends Screen {
    private static final int W=480,H=320,GOLD=0xFFE3C585,TEXT=0xFFE8E1D4,MUTED=0xFFA89D8C,ROWS=5;
    private float scale;private double left,top;private int offset,selected=-1;
    private List<SkintNetwork.Point> destinations=List.of();
    private Button travel;
    public SkintScreen(){super(tr("title"));}
    private static Component tr(String key,Object...args){return Component.translatable("rpgprogression.skint."+key,args);}
    @Override protected void init(){
        scale=Math.min(1.5f,Math.min(width/(float)W,height/(float)H));left=(width-W*scale)/2;top=(height-H*scale)/2;
        destinations=ClientSkintData.active()?ClientSkintData.destinations():List.of();
        if(selected>=destinations.size())selected=-1;offset=Math.max(0,Math.min(offset,Math.max(0,destinations.size()-ROWS)));
        for(int row=0;row<ROWS;row++){final int index=offset+row;if(index>=destinations.size())break;
            addRenderableWidget(new SkintButton(43,94+row*29,394,27,pointName(destinations.get(index)),b->selected=index));}
        addRenderableWidget(new SkintButton(43,266,190,24,tr("progression"),b->ClientSkintData.action(0,null)));
        travel=addRenderableWidget(new SkintButton(247,266,190,24,tr("travel"),b->{if(selected>=0)ClientSkintData.action(1,destinations.get(selected).id());}));travel.active=selected>=0;
    }
    private static Component pointName(SkintNetwork.Point point){var p=point.pos();return tr("point",p.getX(),p.getY(),p.getZ());}
    private static Component dimension(SkintNetwork.Point point){var id=point.dimension().location();return id.getNamespace().equals("minecraft")?tr("dimension."+id.getPath()):Component.literal(id.toString());}
    @Override public void tick(){if(!ClientSkintData.allowed()){minecraft.setScreen(null);return;}travel.active=selected>=0;}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g,mouseX,mouseY,partial);int mx=(int)((mouseX-left)/scale),my=(int)((mouseY-top)/scale);
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        GuiTextures.draw(g,"skint_panel",0,0,W,H,1536,1024);
        g.drawCenteredString(font,title,240,40,GOLD);
        if(ClientSkintData.origin()!=null){var point=ClientSkintData.origin();g.drawCenteredString(font,pointName(point),240,56,MUTED);}
        g.drawString(font,tr("destinations",destinations.size()),46,77,TEXT,false);
        if(destinations.isEmpty()){
            int y=145;for(var line:font.split(tr("empty"),330)){g.drawCenteredString(font,line,240,y,MUTED);y+=12;}
        }
        for(var child:children())if(child instanceof Renderable renderable)renderable.render(g,mx,my,partial);
        for(int row=0;row<ROWS&&offset+row<destinations.size();row++){
            int index=offset+row,y=94+row*29;
            if(index==selected)g.fill(49,y+4,51,y+23,GOLD);
            g.drawString(font,dimension(destinations.get(index)),60,y+16,MUTED,false);
        }
        if(destinations.size()>ROWS){int height=ROWS*29-4,thumb=Math.max(12,height*ROWS/destinations.size());g.fill(443,96,445,96+height,0xFF393027);int y=96+offset*(height-thumb)/Math.max(1,destinations.size()-ROWS);g.fill(443,y,445,y+thumb,GOLD);}
        g.drawCenteredString(font,tr("mana_aura"),240,246,GOLD);g.pose().popPose();
    }
    private final class SkintButton extends Button {
        SkintButton(int x,int y,int w,int h,Component text,OnPress pressed){super(x,y,w,h,text,pressed,DEFAULT_NARRATION);}
        @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial){
            g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();float tint=active?1:.5f;RenderSystem.setShaderColor(tint,tint,tint,1);
            g.blit(GuiTextures.texture("skint_button"),getX(),getY(),width,height,0,165,2172,350,2172,724);
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();
            if(isHoveredOrFocused()&&active)g.fill(getX()+4,getY()+3,getX()+width-4,getY()+height-3,0x18E3C585);
            boolean row=getY()<240;int x=row?getX()+17:getX()+(width-font.width(getMessage()))/2,y=row?getY()+5:getY()+(height-8)/2;
            g.drawString(font,getMessage(),x,y,active?TEXT:MUTED,false);
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked((x-left)/scale,(y-top)/scale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased((x-left)/scale,(y-top)/scale,button);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(destinations.size()<=ROWS)return false;offset=Math.max(0,Math.min(destinations.size()-ROWS,offset-(int)Math.signum(vertical)));rebuildWidgets();return true;}
    @Override public boolean isPauseScreen(){return false;}
}
