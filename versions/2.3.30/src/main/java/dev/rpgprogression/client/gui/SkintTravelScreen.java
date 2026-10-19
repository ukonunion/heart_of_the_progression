package dev.rpgprogression.client.gui;

import java.util.*;
import dev.rpgprogression.client.ClientSkintData;
import dev.rpgprogression.client.SkintPreviews;
import dev.rpgprogression.skint.SkintNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** User artwork at native pixel coordinates. Tabs are drawn behind the main frame. */
public final class SkintTravelScreen extends Screen {
    public static final int W=330,H=222,PANEL_Y=28,TAB_Y=2,ROWS=6;
    private static final List<ResourceKey<Level>> DIMENSIONS=List.of(Level.OVERWORLD,Level.NETHER,Level.END);
    private final Screen parent;
    private float scale;private double left,top;private int dimension,offset;private UUID selected;
    private List<SkintNetwork.Point> points=List.of();private Button travel;
    public SkintTravelScreen(Screen parent){super(SkintWidgets.tr("travel"));this.parent=parent;if(ClientSkintData.origin()!=null)dimension=Math.max(0,DIMENSIONS.indexOf(ClientSkintData.origin().dimension()));}
    public void refreshNames(){rebuildWidgets();}
    @Override protected void init(){
        scale=Math.min(width/(float)(W+16),height/(float)(H+12));left=(width-W*scale)/2;top=(height-H*scale)/2;
        points=ClientSkintData.destinations().stream().filter(p->p.dimension().equals(DIMENSIONS.get(dimension))).toList();
        offset=Math.max(0,Math.min(offset,Math.max(0,points.size()-ROWS)));
        for(int i=0;i<3;i++){final int tab=i;addRenderableWidget(new Tab(105+i*40,tab));}
        for(int i=0;i<ROWS&&offset+i<points.size();i++){
            var point=points.get(offset+i);addRenderableWidget(new SkintWidgets.Action(34,66+i*18,115,15,SkintWidgets.pointName(point),b->{selected=point.id();travel.active=true;},font));
        }
        travel=addRenderableWidget(new SkintWidgets.Action(90,186,150,17,SkintWidgets.tr("travel"),b->{if(selected!=null)ClientSkintData.action(1,selected);},font));
        travel.active=selected!=null;
    }
    @Override public void tick(){if(!ClientSkintData.allowed()){minecraft.setScreen(null);return;}travel.active=selected!=null;}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,0x28000000);
        int mx=(int)((mouseX-left)/scale),my=(int)((mouseY-top)/scale);
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        for(var child:children())if(child instanceof Tab tab)tab.render(g,mx,my,partial);
        GuiTextures.draw(g,"tp_menu",0,PANEL_Y,330,194,330,194);
        for(var child:children())if(child instanceof Renderable renderable&&!(child instanceof Tab))renderable.render(g,mx,my,partial);
        for(int i=0;i<ROWS&&offset+i<points.size();i++)if(points.get(offset+i).id().equals(selected))g.fill(35,69+i*18,37,78+i*18,0xFFE3C585);
        if(points.isEmpty()){int y=101;for(var line:font.split(SkintWidgets.tr("dimension_empty"),110)){g.drawCenteredString(font,line,91,y,0xFFB8AD9B);y+=11;}}
        if(points.size()>ROWS){int thumb=Math.max(8,108*ROWS/points.size());g.fill(153,66,155,174,0xFF393027);int y=66+offset*(108-thumb)/(points.size()-ROWS);g.fill(153,y,155,y+thumb,0xFFE3C585);}
        g.fill(168,64,300,145,0xFF756450);g.fill(169,65,299,144,0xFF0F0E0D);
        var point=points.stream().filter(p->p.id().equals(selected)).findFirst().orElse(null);
        var preview=point==null?null:SkintPreviews.get(point.id());
        if(preview!=null)g.blit(preview,170,68,128,72,0,0,640,360,640,360);
        else {int y=94;for(var line:font.split(SkintWidgets.tr(point==null?"select_destination":"preview_missing"),116)){g.drawCenteredString(font,line,234,y,0xFFB8AD9B);y+=11;}}
        GuiTextures.draw(g,"tp_button",171,151,126,16,115,14);
        if(point!=null){var name=SkintWidgets.pointName(point);int tw=Math.min(118,font.width(name));SkintWidgets.text(g,font,name,234-tw/2,155,118,0xFFEFE9DF);}
        g.drawCenteredString(font,SkintWidgets.tr("travel_cost",dev.rpgprogression.resource.SparkManager.skintTravelCostText()),165,176,0xFFD3BA80);
        g.pose().popPose();
        if(my>=TAB_Y&&my<PANEL_Y&&mx>=105&&mx<223){int tab=(mx-105)/40;if(tab<3)g.renderTooltip(font,SkintWidgets.tr("dimension."+DIMENSIONS.get(tab).location().getPath()),mouseX,mouseY);}
    }
    private final class Tab extends Button {
        private final int index;
        Tab(int x,int index){super(x,TAB_Y,38,PANEL_Y-TAB_Y,SkintWidgets.tr("dimension."+DIMENSIONS.get(index).location().getPath()),b->{},DEFAULT_NARRATION);this.index=index;}
        @Override public void onPress(){if(dimension==index)return;dimension=index;offset=0;selected=null;rebuildWidgets();}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
            GuiTextures.draw(g,index==dimension?"tp_tab_on":"tp_tab_off",getX(),TAB_Y,38,32,38,32);
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked((x-left)/scale,(y-top)/scale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased((x-left)/scale,(y-top)/scale,button);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(points.size()<=ROWS)return false;offset=Math.max(0,Math.min(points.size()-ROWS,offset-(int)Math.signum(vertical)));rebuildWidgets();return true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
