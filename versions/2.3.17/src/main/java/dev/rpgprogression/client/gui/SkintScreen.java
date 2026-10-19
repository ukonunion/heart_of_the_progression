package dev.rpgprogression.client.gui;

import dev.rpgprogression.client.ClientSkintData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** The initial interaction menu leaves the world and its side camera visible. */
public final class SkintScreen extends Screen {
    private float scale;private double left,top;
    private boolean editing,saving;
    private net.minecraft.client.gui.components.EditBox nameField;
    public SkintScreen(){super(SkintWidgets.tr("title"));}
    @Override protected void init(){
        scale=Math.min(width/640f,height/360f);left=(width-640*scale)/2;top=(height-360*scale)/2;
        if(editing){
            String text=nameField==null?(ClientSkintData.origin()==null?"":ClientSkintData.origin().name()):nameField.getValue();
            nameField=addRenderableWidget(new net.minecraft.client.gui.components.EditBox(font,20,28,282,20,SkintWidgets.tr("name_hint")));
            nameField.setMaxLength(dev.rpgprogression.skint.SkintNames.MAX_LENGTH);nameField.setValue(text);nameField.setHint(SkintWidgets.tr("name_hint"));nameField.setEditable(!saving);setInitialFocus(nameField);
            addRenderableWidget(new SkintWidgets.Action(20,61,88,19,SkintWidgets.tr("save_name"),b->saveName(),font));
            addRenderableWidget(new SkintWidgets.Action(114,61,88,19,SkintWidgets.tr("cancel_name"),b->{editing=false;nameField=null;rebuildWidgets();},font));
        }else addRenderableWidget(new SkintWidgets.Action(20,61,124,19,SkintWidgets.tr("rename"),b->{editing=true;rebuildWidgets();},font));
        addRenderableWidget(new SkintWidgets.Action(20,100,182,25,SkintWidgets.tr("progression"),b->ClientSkintData.action(0,null),font));
        addRenderableWidget(new SkintWidgets.Action(20,137,182,25,SkintWidgets.tr("travel"),b->minecraft.setScreen(new SkintTravelScreen(this)),font));
        children().forEach(w->{if(w instanceof net.minecraft.client.gui.components.AbstractWidget button)button.active=!ClientSkintData.busy()&&!saving;});
        addRenderableWidget(new SkintWidgets.Action(20,174,182,25,SkintWidgets.tr("exit"),b->onClose(),font));
    }
    @Override public void renderBackground(GuiGraphics g,int mouseX,int mouseY,float partial){}
    @Override public void tick(){if(!ClientSkintData.allowed())minecraft.setScreen(null);}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        for(int x=0;x<width*.5;x++){int alpha=(int)(190*Math.pow(1-x/(width*.5),1.6));g.fill(x,0,x+1,height,alpha<<24);}
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        var name=ClientSkintData.origin()==null?title:SkintWidgets.pointName(ClientSkintData.origin());
        if(!editing){g.fill(12,25,Math.min(318,28+font.width(name)),51,0x70433D33);SkintWidgets.text(g,font,name,21,34,286,0xFFFFBB35);}
        if(ClientSkintData.busy())SkintWidgets.text(g,font,SkintWidgets.tr("travelling"),21,211,286,0xFFFFBB35);
        super.render(g,(int)((mouseX-left)/scale),(int)((mouseY-top)/scale),partial);g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        boolean wasEditing=editing;boolean handled=super.mouseClicked((x-left)/scale,(y-top)/scale,button);
        // The click dispatcher can otherwise focus the rename button removed by rebuildWidgets.
        if(handled&&!wasEditing&&editing){setFocused(nameField);nameField.setFocused(true);}
        return handled;
    }
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased((x-left)/scale,(y-top)/scale,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return super.mouseDragged((x-left)/scale,(y-top)/scale,button,dx/scale,dy/scale);}
    private void saveName(){if(!saving&&!ClientSkintData.busy()){saving=true;ClientSkintData.rename(nameField.getValue());rebuildWidgets();}}
    public void renamed(){if(saving){editing=false;saving=false;nameField=null;rebuildWidgets();}}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(editing){if(key==256){editing=false;saving=false;nameField=null;rebuildWidgets();return true;}if(key==257||key==335){saveName();return true;}}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void onClose(){if(dev.rpgprogression.client.SkintFade.active())return;ClientSkintData.closeWithCameraReturn();minecraft.setScreen(null);}
    @Override public boolean isPauseScreen(){return false;}
}
