package dev.rpgprogression.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Compact skill action using the supplied atlas, with a clickable text label. */
public final class SkillTreeActionButton extends Button {
    public SkillTreeActionButton(int x,int y,int width,Component label,OnPress press){
        super(x,y,width,20,label,press,DEFAULT_NARRATION);
    }
    @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial){
        GuiTextures.skillButton(g,getX(),getY(),!active?2:isHoveredOrFocused()?1:0,20,20);
        var font=Minecraft.getInstance().font;
        float fitted=Math.min(1,(getWidth()-26)/(float)Math.max(1,font.width(getMessage())));
        g.pose().pushPose();g.pose().translate(getX()+26,getY()+6,0);g.pose().scale(fitted,fitted,1);
        g.drawString(font,getMessage(),0,0,active?isHoveredOrFocused()?0xFFD3BA80:0xFFE8E2D4:0xFFB5AC98,false);
        g.pose().popPose();
    }
}
