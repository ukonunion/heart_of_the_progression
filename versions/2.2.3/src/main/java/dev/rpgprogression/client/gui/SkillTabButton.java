package dev.rpgprogression.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Original 26x32 tabs; the supplied frame covers their bottom edge. */
public final class SkillTabButton extends Button {
    private final boolean selected;
    private final ItemStack icon;
    public SkillTabButton(int x, int y, Component label, ItemStack icon, boolean selected, OnPress press) {
        super(x, y, 26, 32, label, press, DEFAULT_NARRATION); this.icon=icon; this.selected=selected;
    }
    @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial) {
        GuiTextures.draw(g,selected?"tab_top_selected_1":"tab_top_unselected_1",getX(),getY(),26,32,26,32);
        g.renderFakeItem(icon,getX()+5,getY()+7);
        if(!active){g.pose().pushPose();g.pose().translate(0,0,300);g.fill(getX()+4,getY()+6,getX()+22,getY()+24,0xA0181512);g.pose().popPose();}
    }
}
