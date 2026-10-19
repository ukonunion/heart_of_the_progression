package dev.rpgprogression.client.gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.systems.RenderSystem;
/** Original atlas is 64x128: three 40x40 states at (0,0), (0,40), (0,80). */
public final class SkillTreeButton extends Button {
    public SkillTreeButton(int x,int y,Component label,OnPress press){super(x,y,40,40,label,press,DEFAULT_NARRATION);}
    @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial){
        int state=!active?2:isHoveredOrFocused()?1:0;
        GuiTextures.skillButton(g,getX(),getY(),state);
    }
}
